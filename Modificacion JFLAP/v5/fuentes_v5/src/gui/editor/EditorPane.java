/*
 * Decompiled with CFR 0.152.
 */
package gui.editor;

import automata.Automaton;
import automata.Note;
import gui.editor.AutomatonSizeSlider;
import gui.editor.DefaultToolBox;
import gui.editor.EditCanvas;
import gui.editor.ToolBar;
import gui.editor.ToolBox;
import gui.viewer.AutomatonDrawer;
import gui.viewer.SelectionDrawer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

public class EditorPane
extends JComponent {
    protected Automaton automaton;
    protected AutomatonDrawer drawer;
    protected EditCanvas pane;
    protected ToolBar toolbar;

    public EditorPane(Automaton automaton) {
        this(new SelectionDrawer(automaton));
    }

    public EditorPane(Automaton automaton, ToolBox toolBox) {
        this(new SelectionDrawer(automaton), toolBox);
    }

    public EditorPane(AutomatonDrawer automatonDrawer) {
        this(automatonDrawer, (ToolBox)new DefaultToolBox());
    }

    public EditorPane(AutomatonDrawer automatonDrawer, ToolBox toolBox) {
        this(automatonDrawer, toolBox, false);
    }

    public EditorPane(AutomatonDrawer automatonDrawer, ToolBox toolBox, boolean bl) {
        this.pane = new EditCanvas(automatonDrawer, bl);
        this.pane.setCreator(this);
        this.drawer = automatonDrawer;
        this.automaton = automatonDrawer.getAutomaton();
        this.setLayout(new BorderLayout());
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.add((Component)new JScrollPane(this.pane, 22, 32), "Center");
        jPanel.setBorder(new BevelBorder(1));
        this.toolbar = new ToolBar(this.pane, automatonDrawer, toolBox);
        if (this.automaton instanceof automata.turing.TuringMachine) {
            final EditorPane self = this;
            javax.swing.JButton tableButton = new javax.swing.JButton("Table");
            tableButton.setToolTipText("Edit the rules as a table (Ctrl+Shift+T)");
            tableButton.setFocusable(false);
            tableButton.addActionListener(new java.awt.event.ActionListener(){

                @Override
                public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                    plus.RuleTable.open(self);
                }
            });
            this.toolbar.addSeparator();
            this.toolbar.add(tableButton);
        }
        this.pane.setToolBar(this.toolbar);
        this.add((Component)jPanel, "Center");
        this.add((Component)this.toolbar, "North");
        this.add((Component)new AutomatonSizeSlider(this.pane, automatonDrawer), "South");
        ArrayList arrayList = automatonDrawer.getAutomaton().getNotes();
        for (int i = 0; i < arrayList.size(); ++i) {
            ((Note)arrayList.get(i)).initializeForView(this.pane);
        }
    }

    public ToolBar getToolBar() {
        return this.toolbar;
    }

    public AutomatonDrawer getDrawer() {
        return this.pane.getDrawer();
    }

    public EditCanvas getAutomatonPane() {
        return this.pane;
    }

    @Override
    public void printComponent(Graphics graphics) {
        this.pane.print(graphics);
    }

    @Override
    public void printChildren(Graphics graphics) {
    }

    public Automaton getAutomaton() {
        return this.automaton;
    }
}

