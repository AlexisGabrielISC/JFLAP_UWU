package plus;

import automata.Configuration;
import automata.turing.TMConfiguration;
import automata.turing.Tape;
import gui.sim.ConfigurationController;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultHighlighter;

/**
 * Visor de la cinta completa de una Maquina de Turing durante la simulacion.
 * Muestra TODO el contenido de cada cinta (sin recortes), la posicion del cabezal, la salida
 * (desde el cabezal hasta el siguiente blanco) y el conteo de cada simbolo.
 * Sigue los pasos de la simulacion (Step, Auto, Reset) mientras esta abierto.
 */
public final class TapeViewer extends JDialog {
    private static final Map<ConfigurationController, TapeViewer> OPEN = new HashMap<ConfigurationController, TapeViewer>();
    private static final char BLANK = '□';
    private static final int MAX_TABS = 8;

    private final ConfigurationController controller;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel summary = new JLabel(" ");
    private final Timer coalesce;

    public static void show(ConfigurationController controller, Component anchor) {
        if (controller == null || !controller.isTuringMachine()) {
            return;
        }
        TapeViewer viewer = OPEN.get(controller);
        if (viewer == null) {
            viewer = new TapeViewer(controller, anchor);
            OPEN.put(controller, viewer);
        }
        viewer.refreshNow();
        viewer.setVisible(true);
        viewer.toFront();
    }

    public static boolean isOpen(ConfigurationController controller) {
        TapeViewer viewer = OPEN.get(controller);
        return viewer != null && viewer.isShowing();
    }

    private TapeViewer(ConfigurationController controller, Component anchor) {
        super(anchor == null ? null : SwingUtilities.getWindowAncestor(anchor), "Tape viewer", ModalityType.MODELESS);
        this.controller = controller;
        this.coalesce = new Timer(40, e -> refreshNow());
        this.coalesce.setRepeats(false);

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(6, 8, 2, 8));
        top.add(summary, BorderLayout.CENTER);
        JLabel hint = new JLabel("Follows the simulation. Select configurations to inspect them.");
        hint.setBorder(new EmptyBorder(0, 8, 4, 8));

        getContentPane().setLayout(new BorderLayout());
        JPanel north = new JPanel(new BorderLayout());
        north.add(top, BorderLayout.NORTH);
        north.add(hint, BorderLayout.SOUTH);
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(720, 520));
        pack();
        setLocationRelativeTo(anchor == null ? null : SwingUtilities.getWindowAncestor(anchor));

        controller.addChangeListener(this::scheduleRefresh);
        controller.addCleanupListener(this::dispose);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                coalesce.stop();
                OPEN.remove(TapeViewer.this.controller);
            }
        });
    }

    private void scheduleRefresh() {
        if (!isShowing()) {
            return;
        }
        if (!coalesce.isRunning()) {
            coalesce.start();
        }
    }

    private static String statusName(int state) {
        switch (state) {
            case 1:
                return "Accepted";
            case 2:
                return "Rejected";
            case 3:
                return "Frozen";
            case 4:
                return "Focused";
            default:
                return "Running";
        }
    }

    private void refreshNow() {
        int selected = Math.max(0, tabs.getSelectedIndex());
        Configuration[] targets = controller.viewTargets();
        tabs.removeAll();
        int shown = Math.min(targets.length, MAX_TABS);
        for (int i = 0; i < shown; i++) {
            if (!(targets[i] instanceof TMConfiguration)) {
                continue;
            }
            TMConfiguration c = (TMConfiguration)targets[i];
            String status = statusName(controller.stateOf(c));
            JPanel panel = buildConfiguration(c, status);
            tabs.addTab(shown == 1 ? "Configuration" : (i + 1) + " " + status, new JScrollPane(panel));
        }
        if (tabs.getTabCount() > 0) {
            tabs.setSelectedIndex(Math.min(selected, tabs.getTabCount() - 1));
        }
        String extra = targets.length > shown ? "  (+" + (targets.length - shown) + " more not shown)" : "";
        if (targets.length == 0) {
            summary.setText("No configurations.");
        } else {
            TMConfiguration first = targets[0] instanceof TMConfiguration ? (TMConfiguration)targets[0] : null;
            summary.setText("<html><b>" + (first == null ? "" : statusName(controller.stateOf(first)))
                    + "</b> &nbsp; " + targets.length + " configuration(s)" + extra + "</html>");
        }
        revalidate();
        repaint();
    }

    private JPanel buildConfiguration(TMConfiguration c, String status) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel head = new JLabel("<html><b>State:</b> " + escape(c.getCurrentState().getName()) + " &nbsp; <b>Status:</b> " + status + "</html>");
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(head);
        panel.add(Box.createVerticalStrut(8));
        Tape[] tapes = c.getTapes();
        for (int t = 0; t < tapes.length; t++) {
            panel.add(buildTape(t, tapes.length, tapes[t]));
            panel.add(Box.createVerticalStrut(10));
        }
        return panel;
    }

    private JPanel buildTape(int index, int count, Tape tape) {
        String all = tape.getContents();
        int head = Math.max(0, Math.min(tape.getTapeHead(), Math.max(0, all.length() - 1)));
        int first = 0;
        while (first < all.length() && all.charAt(first) == BLANK) {
            first++;
        }
        int last = all.length() - 1;
        while (last >= 0 && all.charAt(last) == BLANK) {
            last--;
        }
        int lo = Math.min(first, head);
        int hi = Math.max(last, head);
        if (all.isEmpty()) {
            lo = 0;
            hi = -1;
        }
        String shown = all.isEmpty() ? "" : all.substring(lo, hi + 1);
        String content = first > last ? "" : all.substring(first, last + 1);
        int end = all.indexOf(BLANK, head);
        String output = all.isEmpty() ? "" : all.substring(head, end == -1 ? all.length() : end);

        TreeMap<Character, Integer> counts = new TreeMap<Character, Integer>();
        for (int i = first; i <= last; i++) {
            counts.merge(all.charAt(i), 1, Integer::sum);
        }
        StringBuilder histogram = new StringBuilder();
        for (Map.Entry<Character, Integer> e : counts.entrySet()) {
            if (histogram.length() > 0) {
                histogram.append("   ");
            }
            histogram.append(e.getKey()).append(" × ").append(e.getValue());
        }

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        String title = count == 1 ? "Tape" : "Tape " + (index + 1);
        JLabel label = new JLabel("<html><b>" + title + "</b> &nbsp; " + all.length() + " cells, head at cell " + head + "</html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(label);

        JTextArea area = new JTextArea(shown);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setRows(5);
        try {
            int h = head - lo;
            if (!shown.isEmpty() && h >= 0 && h < shown.length()) {
                area.getHighlighter().addHighlight(h, h + 1, new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 170, 0, 170)));
                area.setCaretPosition(h);
                final int headIndex = h;
                SwingUtilities.invokeLater(() -> {
                    try {
                        java.awt.geom.Rectangle2D r = area.modelToView2D(headIndex);
                        if (r != null) {
                            java.awt.Rectangle rect = r.getBounds();
                            rect.grow(0, 30);
                            area.scrollRectToVisible(rect);
                        }
                    } catch (javax.swing.text.BadLocationException ex) {
                        // sin desplazamiento
                    }
                });
            }
        } catch (javax.swing.text.BadLocationException ex) {
            // sin resaltado
        }
        JScrollPane scroll = new JScrollPane(area);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(600, 110));
        box.add(scroll);
        box.add(Box.createVerticalStrut(4));
        box.add(field("Head (orange) is highlighted. Blanks at both ends are not shown.", null));
        box.add(field("Content without blanks", content));
        box.add(field("Output (head → next blank)", output));
        box.add(field("Symbol count", histogram.toString()));
        final String copy = content;
        JButton copyButton = new JButton("Copy content");
        copyButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyButton.addActionListener(e -> java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(copy), null));
        box.add(Box.createVerticalStrut(4));
        box.add(copyButton);
        return box;
    }

    private JPanel field(String name, String value) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel l = new JLabel(name + (value == null ? "" : ":"));
        if (value == null) {
            l.setFont(l.getFont().deriveFont(Font.ITALIC));
            row.add(l, BorderLayout.CENTER);
            return row;
        }
        row.add(l, BorderLayout.WEST);
        JTextField f = new JTextField(value);
        f.setEditable(false);
        f.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        f.setCaretPosition(0);
        f.setPreferredSize(new Dimension(120, f.getPreferredSize().height));
        row.add(f, BorderLayout.CENTER);
        return row;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Solo para pruebas. */
    static List<String> debugTexts(TapeViewer v) {
        return new ArrayList<String>();
    }
}
