package plus;

import automata.Automaton;
import automata.State;
import automata.Transition;
import automata.event.AutomataStateEvent;
import automata.event.AutomataStateListener;
import automata.event.AutomataTransitionEvent;
import automata.event.AutomataTransitionListener;
import automata.turing.TMState;
import automata.turing.TMTransition;
import automata.turing.TuringMachine;
import gui.editor.EditorPane;
import gui.environment.AutomatonEnvironment;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 * Editor de reglas de una Maquina de Turing en forma de tabla.
 * Pestana "Transitions": una fila por regla (Desde | Lee | Escribe | Mueve | Hacia, repetido por cinta).
 * Pestana "States": nombre, inicial y final de cada estado.
 * Los cambios se aplican al diagrama con "Apply" (con deshacer) y el diagrama refresca la tabla.
 */
public final class RuleTable extends JDialog {
    private static final Map<Automaton, RuleTable> OPEN = new HashMap<Automaton, RuleTable>();
    private static final String BLANK = TMTransition.BLANK;
    private static final String[] MOVES = new String[]{"R", "L", "S"};

    private final EditorPane pane;
    private final TuringMachine tm;
    private final int tapes;

    private final DefaultTableModel tModel;
    private final DefaultTableModel sModel;
    private final JTable tTable;
    private final JTable sTable;
    private final JLabel status = new JLabel(" ");

    private final List<Transition> origT = new ArrayList<Transition>();
    private final List<State> origS = new ArrayList<State>();
    private final List<String> lastNames = new ArrayList<String>();

    private boolean loading = false;
    private boolean applying = false;
    private boolean edited = false;

    private final AutomataStateListener stateListener;
    private final AutomataTransitionListener transitionListener;

    // ------------------------------------------------------------------ apertura

    public static void open(EditorPane pane) {
        if (pane == null || !(pane.getAutomaton() instanceof TuringMachine)) {
            return;
        }
        RuleTable existing = OPEN.get(pane.getAutomaton());
        if (existing != null) {
            existing.reload();
            existing.toFront();
            existing.setVisible(true);
            return;
        }
        RuleTable table = new RuleTable(pane);
        OPEN.put(pane.getAutomaton(), table);
        table.setVisible(true);
    }

    private RuleTable(EditorPane pane) {
        super(SwingUtilities.getWindowAncestor(pane), "Transition table", ModalityType.MODELESS);
        this.pane = pane;
        this.tm = (TuringMachine)pane.getAutomaton();
        this.tapes = this.tm.tapes();

        // ---- modelo de transiciones
        List<String> cols = new ArrayList<String>();
        cols.add("From");
        for (int t = 0; t < tapes; t++) {
            String suffix = tapes == 1 ? "" : " " + (t + 1);
            cols.add("Read" + suffix);
            cols.add("Write" + suffix);
            cols.add("Move" + suffix);
        }
        cols.add("To");
        this.tModel = new DefaultTableModel(cols.toArray(), 0);
        this.tTable = new JTable(tModel);
        this.tTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        this.tTable.setRowHeight(this.tTable.getRowHeight() + 6);
        this.tTable.getTableHeader().setReorderingAllowed(false);
        for (int t = 0; t < tapes; t++) {
            TableColumn move = tTable.getColumnModel().getColumn(3 + 3 * t);
            move.setCellEditor(new DefaultCellEditor(new JComboBox<String>(MOVES)));
            move.setMaxWidth(80);
        }

        // ---- modelo de estados
        this.sModel = new DefaultTableModel(new Object[]{"State", "Initial", "Final"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 0 ? String.class : Boolean.class;
            }
        };
        this.sTable = new JTable(sModel);
        this.sTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        this.sTable.setRowHeight(this.sTable.getRowHeight() + 6);
        this.sTable.getTableHeader().setReorderingAllowed(false);

        this.sModel.addTableModelListener(e -> {
            if (loading) {
                return;
            }
            edited = true;
            if (e.getType() != TableModelEvent.UPDATE || e.getFirstRow() < 0) {
                return;
            }
            int row = e.getFirstRow();
            if (e.getColumn() == 0 && row < lastNames.size()) {
                // renombrar un estado: actualizar las referencias en la tabla de reglas
                String oldName = lastNames.get(row);
                String newName = String.valueOf(sModel.getValueAt(row, 0)).trim();
                if (!oldName.equals(newName)) {
                    loading = true;
                    try {
                        for (int r = 0; r < tModel.getRowCount(); r++) {
                            if (oldName.equals(cell(r, 0))) {
                                tModel.setValueAt(newName, r, 0);
                            }
                            if (oldName.equals(cell(r, tModel.getColumnCount() - 1))) {
                                tModel.setValueAt(newName, r, tModel.getColumnCount() - 1);
                            }
                        }
                    } finally {
                        loading = false;
                    }
                    lastNames.set(row, newName);
                }
            } else if (e.getColumn() == 1 && Boolean.TRUE.equals(sModel.getValueAt(row, 1))) {
                // solo un estado inicial
                loading = true;
                try {
                    for (int r = 0; r < sModel.getRowCount(); r++) {
                        if (r != row) {
                            sModel.setValueAt(Boolean.FALSE, r, 1);
                        }
                    }
                } finally {
                    loading = false;
                }
            }
        });
        this.tModel.addTableModelListener(e -> {
            if (!loading) {
                edited = true;
            }
        });

        // ---- interfaz
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Transitions", new JScrollPane(tTable));
        tabs.addTab("States", new JScrollPane(sTable));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.add(button("Add rule", "Adds a rule after the selected one (starts from its 'To' state)", e -> addRule(false)));
        top.add(button("Duplicate", "Duplicates the selected rule", e -> addRule(true)));
        top.add(button("Delete rule", "Deletes the selected rules", e -> deleteSelected(tTable, tModel, false)));
        top.add(new JLabel("  |  "));
        top.add(button("Add state", "Adds a new state", e -> addState()));
        top.add(button("Delete state", "Deletes the selected states and the rules connected to them", e -> deleteSelected(sTable, sModel, true)));

        JPanel bottom = new JPanel(new BorderLayout());
        JLabel hint = new JLabel("<html>Read/Write: one symbol. Empty = blank (□). <b>~</b> = keep the symbol. "
                + "Read also accepts <b>!a</b>, <b>[a-z]</b>, <b>[\\d]</b>, <b>\\w</b>, <b>\\W</b>... "
                + "Unknown state names are created automatically.</html>");
        hint.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        buttons.add(status);
        buttons.add(button("Refresh", "Discards the table edits and reloads it from the diagram", e -> reload()));
        JButton apply = button("Apply", "Writes the table into the diagram (can be undone)", e -> apply());
        buttons.add(apply);
        buttons.add(button("Close", "Closes this window", e -> dispose()));
        bottom.add(hint, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(null);
        setPreferredSize(new Dimension(Math.max(720, 260 + 230 * tapes), 460));
        pack();
        setLocationRelativeTo(SwingUtilities.getWindowAncestor(pane));

        // ---- sincronizacion con el diagrama
        this.stateListener = new AutomataStateListener() {
            @Override
            public void automataStateChange(AutomataStateEvent ev) {
                diagramChanged();
            }
        };
        this.transitionListener = new AutomataTransitionListener() {
            @Override
            public void automataTransitionChange(AutomataTransitionEvent ev) {
                diagramChanged();
            }
        };
        tm.addStateListener(stateListener);
        tm.addTransitionListener(transitionListener);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                tm.removeStateListener(stateListener);
                tm.removeTransitionListener(transitionListener);
                OPEN.remove(tm);
            }
        });
        reload();
    }

    private JButton button(String text, String tip, java.awt.event.ActionListener l) {
        JButton b = new JButton(text);
        b.setToolTipText(tip);
        b.addActionListener(l);
        return b;
    }

    // ------------------------------------------------------------------ carga

    private void diagramChanged() {
        if (applying || loading || edited) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (!edited && !applying) {
                reload();
            }
        });
    }

    private String cell(int row, int col) {
        Object v = tModel.getValueAt(row, col);
        return v == null ? "" : v.toString();
    }

    private static String show(String s) {
        return s == null || s.isEmpty() ? BLANK : s;
    }

    private static String fromShown(String s) {
        return BLANK.equals(s) ? "" : s;
    }

    private void reload() {
        stopEditing();
        loading = true;
        try {
            tModel.setRowCount(0);
            sModel.setRowCount(0);
            origT.clear();
            origS.clear();
            lastNames.clear();

            State[] states = tm.getStates();
            State[] sorted = states.clone();
            Arrays.sort(sorted, Comparator.comparingInt(State::getID));
            for (State s : sorted) {
                origS.add(s);
                lastNames.add(s.getName());
                sModel.addRow(new Object[]{s.getName(), tm.getInitialState() == s, tm.isFinalState(s)});
            }
            Transition[] ts = tm.getTransitions();
            List<Transition> list = new ArrayList<Transition>(Arrays.asList(ts));
            Collections.sort(list, Comparator.<Transition>comparingInt(t -> t.getFromState().getID()).thenComparingInt(t -> t.getToState().getID()));
            for (Transition t : list) {
                TMTransition m = (TMTransition)t;
                Object[] row = new Object[tModel.getColumnCount()];
                row[0] = m.getFromState().getName();
                for (int i = 0; i < tapes; i++) {
                    row[1 + 3 * i] = show(m.getRead(i));
                    row[2 + 3 * i] = show(m.getWrite(i));
                    row[3 + 3 * i] = m.getDirection(i);
                }
                row[row.length - 1] = m.getToState().getName();
                origT.add(t);
                tModel.addRow(row);
            }
        } finally {
            loading = false;
        }
        edited = false;
        status.setText(" ");
    }

    private void stopEditing() {
        if (tTable.isEditing()) {
            tTable.getCellEditor().stopCellEditing();
        }
        if (sTable.isEditing()) {
            sTable.getCellEditor().stopCellEditing();
        }
    }

    // ------------------------------------------------------------------ filas

    private void addRule(boolean duplicate) {
        stopEditing();
        int sel = tTable.getSelectedRow();
        Object[] row = new Object[tModel.getColumnCount()];
        if (duplicate && sel >= 0) {
            for (int c = 0; c < row.length; c++) {
                row[c] = tModel.getValueAt(sel, c);
            }
        } else {
            String from = "";
            if (sel >= 0) {
                from = cell(sel, tModel.getColumnCount() - 1);
            } else if (tModel.getRowCount() > 0) {
                from = cell(tModel.getRowCount() - 1, tModel.getColumnCount() - 1);
            } else if (sModel.getRowCount() > 0) {
                from = String.valueOf(sModel.getValueAt(0, 0));
            }
            row[0] = from;
            for (int i = 0; i < tapes; i++) {
                row[1 + 3 * i] = "";
                row[2 + 3 * i] = "~";
                row[3 + 3 * i] = "R";
            }
            row[row.length - 1] = "";
        }
        int at = sel >= 0 ? sel + 1 : tModel.getRowCount();
        origT.add(at, null);
        tModel.insertRow(at, row);
        tTable.setRowSelectionInterval(at, at);
        tTable.editCellAt(at, 1);
        tTable.requestFocusInWindow();
    }

    private void addState() {
        stopEditing();
        int n = 0;
        Set<String> names = new java.util.HashSet<String>();
        for (int r = 0; r < sModel.getRowCount(); r++) {
            names.add(String.valueOf(sModel.getValueAt(r, 0)));
        }
        while (names.contains("q" + n)) {
            n++;
        }
        origS.add(null);
        lastNames.add("q" + n);
        sModel.addRow(new Object[]{"q" + n, Boolean.FALSE, Boolean.FALSE});
        sTable.setRowSelectionInterval(sModel.getRowCount() - 1, sModel.getRowCount() - 1);
    }

    private void deleteSelected(JTable table, DefaultTableModel model, boolean states) {
        stopEditing();
        int[] rows = table.getSelectedRows();
        Arrays.sort(rows);
        for (int i = rows.length - 1; i >= 0; i--) {
            int r = rows[i];
            if (states) {
                String name = String.valueOf(model.getValueAt(r, 0));
                origS.remove(r);
                lastNames.remove(r);
                // las reglas conectadas a ese estado tambien se eliminan
                for (int t = tModel.getRowCount() - 1; t >= 0; t--) {
                    if (name.equals(cell(t, 0)) || name.equals(cell(t, tModel.getColumnCount() - 1))) {
                        origT.remove(t);
                        tModel.removeRow(t);
                    }
                }
            } else {
                origT.remove(r);
            }
            model.removeRow(r);
        }
    }

    // ------------------------------------------------------------------ aplicar

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Transition table", JOptionPane.ERROR_MESSAGE);
        status.setText("Not applied");
    }

    private void apply() {
        stopEditing();

        // el diagrama no debe haber cambiado por debajo (p. ej. con deshacer)
        Set<State> present = Collections.newSetFromMap(new IdentityHashMap<State, Boolean>());
        present.addAll(Arrays.asList(tm.getStates()));
        for (State s : origS) {
            if (s != null && !present.contains(s)) {
                error("The diagram changed since the table was loaded.\nPress Refresh and redo your edits.");
                return;
            }
        }

        // ---- 1. estados
        int nS = sModel.getRowCount();
        String[] names = new String[nS];
        Map<String, Integer> seen = new HashMap<String, Integer>();
        State initialRowState = null;
        int initialRow = -1;
        for (int r = 0; r < nS; r++) {
            names[r] = String.valueOf(sModel.getValueAt(r, 0)).trim();
            if (names[r].isEmpty()) {
                error("States: row " + (r + 1) + " has an empty name.");
                return;
            }
            if (seen.put(names[r], r) != null) {
                error("States: the name \"" + names[r] + "\" is repeated.");
                return;
            }
            if (Boolean.TRUE.equals(sModel.getValueAt(r, 1))) {
                if (initialRow >= 0) {
                    error("Only one state can be initial.");
                    return;
                }
                initialRow = r;
            }
        }

        // ---- 2. reglas: validar
        int nT = tModel.getRowCount();
        Set<String> newNames = new java.util.LinkedHashSet<String>();
        State dummy = new TMState(-1, new Point(0, 0), tm);
        for (int r = 0; r < nT; r++) {
            String from = cell(r, 0).trim();
            String to = cell(r, tModel.getColumnCount() - 1).trim();
            if (from.isEmpty() || to.isEmpty()) {
                error("Rules: row " + (r + 1) + " needs a 'From' and a 'To' state.");
                return;
            }
            try {
                new TMTransition(dummy, dummy, reads(r), writes(r), moves(r));
            } catch (IllegalArgumentException ex) {
                error("Rules: row " + (r + 1) + ": " + ex.getMessage());
                return;
            }
            if (!seen.containsKey(from)) {
                newNames.add(from);
            }
            if (!seen.containsKey(to)) {
                newNames.add(to);
            }
        }

        // ---- 3. escribir en el diagrama
        AutomatonEnvironment env = null;
        if (tm.getEnvironmentFrame() != null && tm.getEnvironmentFrame().getEnvironment() instanceof AutomatonEnvironment) {
            env = (AutomatonEnvironment)tm.getEnvironmentFrame().getEnvironment();
            env.saveStatus();
        }
        applying = true;
        try {
            // estados eliminados
            Set<State> keep = Collections.newSetFromMap(new IdentityHashMap<State, Boolean>());
            for (State s : origS) {
                if (s != null) {
                    keep.add(s);
                }
            }
            for (State s : tm.getStates()) {
                if (!keep.contains(s)) {
                    tm.removeState(s);
                }
            }
            // renombrar / crear
            Map<String, State> byName = new LinkedHashMap<String, State>();
            for (int r = 0; r < nS; r++) {
                State s = origS.get(r);
                if (s == null) {
                    s = tm.createState(freeSpot());
                }
                if (!names[r].equals(s.getName())) {
                    s.setName(names[r]);
                }
                byName.put(names[r], s);
            }
            for (String name : newNames) {
                State s = tm.createState(freeSpot());
                s.setName(name);
                byName.put(name, s);
            }
            // inicial / finales
            tm.setInitialState(initialRow >= 0 ? byName.get(names[initialRow]) : null);
            for (int r = 0; r < nS; r++) {
                State s = byName.get(names[r]);
                boolean fin = Boolean.TRUE.equals(sModel.getValueAt(r, 2));
                if (fin && !tm.isFinalState(s)) {
                    tm.addFinalState(s);
                } else if (!fin && tm.isFinalState(s)) {
                    tm.removeFinalState(s);
                }
            }

            // reglas: conservar las que no cambiaron (asi no se pierden las curvas del diagrama)
            Set<Transition> existing = Collections.newSetFromMap(new IdentityHashMap<Transition, Boolean>());
            existing.addAll(Arrays.asList(tm.getTransitions()));
            Set<Transition> kept = Collections.newSetFromMap(new IdentityHashMap<Transition, Boolean>());
            List<Object[]> toAdd = new ArrayList<Object[]>();
            for (int r = 0; r < nT; r++) {
                State from = byName.get(cell(r, 0).trim());
                State to = byName.get(cell(r, tModel.getColumnCount() - 1).trim());
                Transition orig = origT.get(r);
                String[] rd = reads(r);
                String[] wr = writes(r);
                String[] mv = moves(r);
                if (orig != null && existing.contains(orig) && !kept.contains(orig) && same((TMTransition)orig, from, to, rd, wr, mv)) {
                    kept.add(orig);
                } else {
                    toAdd.add(new Object[]{from, to, rd, wr, mv, orig});
                }
            }
            for (Transition t : existing) {
                if (!kept.contains(t)) {
                    tm.removeTransition(t);
                }
            }
            for (Object[] a : toAdd) {
                TMTransition nt = new TMTransition((State)a[0], (State)a[1], (String[])a[2], (String[])a[3], (String[])a[4]);
                Transition orig = (Transition)a[5];
                if (orig != null) {
                    nt.setBlockTransition(((TMTransition)orig).isBlockTransition());
                    if (orig.getControl() != null && orig.getFromState() == a[0] && orig.getToState() == a[1]) {
                        nt.setControl(new Point(orig.getControl()));
                    }
                }
                tm.addTransition(nt);
            }
        } finally {
            applying = false;
        }
        reload();
        pane.getAutomatonPane().repaint();
        status.setText("Applied");
    }

    private boolean same(TMTransition t, State from, State to, String[] rd, String[] wr, String[] mv) {
        if (t.getFromState() != from || t.getToState() != to) {
            return false;
        }
        for (int i = 0; i < tapes; i++) {
            if (!show(t.getRead(i)).equals(show(rd[i])) || !show(t.getWrite(i)).equals(show(wr[i])) || !t.getDirection(i).equals(mv[i])) {
                return false;
            }
        }
        return true;
    }

    private String[] reads(int r) {
        String[] a = new String[tapes];
        for (int i = 0; i < tapes; i++) {
            a[i] = fromShown(cell(r, 1 + 3 * i));
        }
        return a;
    }

    private String[] writes(int r) {
        String[] a = new String[tapes];
        for (int i = 0; i < tapes; i++) {
            a[i] = fromShown(cell(r, 2 + 3 * i));
        }
        return a;
    }

    private String[] moves(int r) {
        String[] a = new String[tapes];
        for (int i = 0; i < tapes; i++) {
            a[i] = cell(r, 3 + 3 * i).trim();
        }
        return a;
    }

    private Point freeSpot() {
        State[] states = tm.getStates();
        for (int row = 0; row < 40; row++) {
            for (int col = 0; col < 8; col++) {
                Point p = new Point(70 + 130 * col, 70 + 130 * row);
                boolean free = true;
                for (State s : states) {
                    if (s.getPoint() != null && s.getPoint().distance(p) < 70) {
                        free = false;
                        break;
                    }
                }
                if (free) {
                    return p;
                }
            }
        }
        return new Point(70, 70);
    }
}
