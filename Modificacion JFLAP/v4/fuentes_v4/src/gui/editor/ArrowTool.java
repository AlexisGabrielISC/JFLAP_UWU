/*
 * Decompiled with CFR 0.152.
 */
package gui.editor;

import automata.Note;
import automata.State;
import automata.StateRenamer;
import automata.Transition;
import automata.graph.AutomatonGraph;
import automata.graph.LayoutAlgorithm;
import automata.graph.layout.GEMLayoutAlgorithm;
import automata.turing.TMState;
import automata.turing.TMTransition;
import automata.turing.TuringMachine;
import debug.EDebug;
import gui.editor.ArrowDisplayOnlyTool;
import gui.editor.EditBlockPane;
import gui.editor.Tool;
import gui.editor.TransitionCreator;
import gui.environment.AutomatonEnvironment;
import gui.environment.Environment;
import gui.environment.EnvironmentFrame;
import gui.environment.tag.CriticalTag;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;
import gui.viewer.CurvedArrow;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

public class ArrowTool
extends Tool {
    private boolean transitionInFlux = false;
    private TransitionCreator creator;
    private State lastClickedState = null;
    private Transition lastClickedTransition = null;
    private Note lastClickedNote = null;
    private Point initialPointState = new Point();
    private Point initialPointClick = new Point();
    protected StateMenu stateMenu = new StateMenu();
    private TransitionMenu transitionMenu = new TransitionMenu();
    private EmptyMenu emptyMenu = new EmptyMenu();
    private Transition selectedTransition = null;

    public ArrowTool(AutomatonPane automatonPane, AutomatonDrawer automatonDrawer, TransitionCreator transitionCreator) {
        super(automatonPane, automatonDrawer);
        this.creator = transitionCreator;
    }

    public ArrowTool(AutomatonPane automatonPane, AutomatonDrawer automatonDrawer) {
        super(automatonPane, automatonDrawer);
        this.creator = TransitionCreator.creatorForAutomaton(this.getAutomaton(), this.getView());
    }

    @Override
    public String getToolTip() {
        return "Attribute Editor";
    }

    @Override
    protected Icon getIcon() {
        URL uRL = this.getClass().getResource("/ICON/arrow.gif");
        return new ImageIcon(uRL);
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        Transition transition;
        if (mouseEvent.getClickCount() == 1 && (transition = this.getDrawer().transitionAtPoint(mouseEvent.getPoint())) != null) {
            if (transition.isSelected) {
                transition.isSelected = false;
                this.selectedTransition = null;
            } else {
                if (this.selectedTransition != null) {
                    this.selectedTransition.isSelected = false;
                }
                transition.isSelected = true;
                this.selectedTransition = transition;
            }
            return;
        }
        transition = this.getDrawer().transitionAtPoint(mouseEvent.getPoint());
        if (transition == null) {
            Rectangle rectangle = new Rectangle(0, 0, -1, -1);
            this.getView().getDrawer().getAutomaton().selectStatesWithinBounds(rectangle);
            this.getView().repaint();
            return;
        }
        EDebug.print("Beginning to Edit with creator " + this.creator.getClass());
        this.creator.editTransition(transition, mouseEvent.getPoint());
    }

    protected void showPopup(MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            Point point = this.getView().transformFromAutomatonToView(mouseEvent.getPoint());
            if (this.lastClickedState != null && this.shouldShowStatePopup()) {
                this.stateMenu.show(this.lastClickedState, this.getView(), point);
            } else {
                this.emptyMenu.show(this.getView(), point);
            }
        }
        this.lastClickedState = null;
        this.lastClickedTransition = null;
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        Object object;
        if (this.getDrawer().getAutomaton().getEnvironmentFrame() != null) {
            ((AutomatonEnvironment)this.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
        } else {
            EDebug.print("I cannot preserve what you ask");
        }
        this.initialPointClick.setLocation(mouseEvent.getPoint());
        this.lastClickedState = this.getDrawer().stateAtPoint(mouseEvent.getPoint());
        if (this.lastClickedState == null) {
            this.lastClickedTransition = this.getDrawer().transitionAtPoint(mouseEvent.getPoint());
        }
        if (mouseEvent.isPopupTrigger()) {
            this.showPopup(mouseEvent);
        }
        if (this.lastClickedState != null) {
            this.initialPointState.setLocation(this.lastClickedState.getPoint());
            if (!this.lastClickedState.isSelected()) {
                object = new Rectangle(0, 0, -1, -1);
                this.getView().getDrawer().getAutomaton().selectStatesWithinBounds((Rectangle)object);
                this.getView().getDrawer().setSelectionBounds((Rectangle)object);
                this.lastClickedState.setSelect(true);
            }
            this.getView().repaint();
        } else if (this.lastClickedTransition != null) {
            this.initialPointClick.setLocation(mouseEvent.getPoint());
        } else {
            object = this.getDrawer().getAutomaton().getNotes();
            for (int i = 0; i < ((ArrayList)object).size(); ++i) {
                ((Note)((ArrayList)object).get(i)).setEditable(false);
                ((Note)((ArrayList)object).get(i)).setEnabled(false);
                ((Note)((ArrayList)object).get(i)).setCaretColor(new Color(255, 255, 150));
            }
            Rectangle rectangle = new Rectangle(0, 0, -1, -1);
            this.getView().getDrawer().getAutomaton().selectStatesWithinBounds(rectangle);
            this.getView().getDrawer().setSelectionBounds(rectangle);
        }
        Transition[] transitionsNow = this.getAutomaton().getTransitions();
        for (int i = 0; i < transitionsNow.length; ++i) {
            if (!transitionsNow[i].isSelected) continue;
            this.selectedTransition = transitionsNow[i];
            return;
        }
        this.selectedTransition = null;
    }

    protected boolean shouldShowStatePopup() {
        return true;
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        int n;
        Point object;
        Object object2;
        if (this.lastClickedState != null) {
            if (mouseEvent.isPopupTrigger()) {
                return;
            }
            Point dragPoint = mouseEvent.getPoint();
            State[] dragStates = this.getView().getDrawer().getAutomaton().getStates();
            for (n = 0; n < dragStates.length; ++n) {
                State state = dragStates[n];
                if (!state.isSelected()) continue;
                int n2 = state.getPoint().x + dragPoint.x - this.initialPointClick.x;
                int n3 = state.getPoint().y + dragPoint.y - this.initialPointClick.y;
                state.getPoint().setLocation(n2, n3);
                state.setPoint(state.getPoint());
            }
            this.initialPointClick = dragPoint;
            this.getView().repaint();
        } else if (this.lastClickedTransition != null) {
            State state;
            if (mouseEvent.isPopupTrigger()) {
                return;
            }
            object2 = mouseEvent.getPoint();
            int n4 = ((Point)object2).x - this.initialPointClick.x;
            n = ((Point)object2).y - this.initialPointClick.y;
            State state2 = this.lastClickedTransition.getFromState();
            if (state2 == (state = this.lastClickedTransition.getToState())) {
                // empty if block
            }
            if (state2 != state) {
                double d = ((Point)object2).x - state2.getPoint().x;
                double d2 = ((Point)object2).y - state2.getPoint().y;
                Transition[] transitionArray = this.getAutomaton().getTransitionsFromStateToState(state2, state);
                for (int i = 0; i < transitionArray.length; ++i) {
                    CurvedArrow curvedArrow = (CurvedArrow)this.getView().getDrawer().transitionToArrowMap.get(transitionArray[i]);
                    this.getView().getDrawer().arrowToTransitionMap.put(curvedArrow, transitionArray[i]);
                    this.getView().getDrawer().transitionToArrowMap.put(transitionArray[i], curvedArrow);
                }
            }
            this.initialPointClick.setLocation((Point)object2);
            this.getView().repaint();
        } else {
            int n5 = mouseEvent.getPoint().x;
            n = mouseEvent.getPoint().y;
            int n6 = this.initialPointClick.x;
            int n7 = this.initialPointClick.y;
            if (n5 < this.initialPointClick.x) {
                n6 = n5;
            }
            if (n < this.initialPointClick.y) {
                n7 = n;
            }
            object2 = new Rectangle(n6, n7, Math.abs(n5 - this.initialPointClick.x), Math.abs(n - this.initialPointClick.y));
            if (!this.transitionInFlux) {
                this.getView().getDrawer().getAutomaton().selectStatesWithinBounds((Rectangle)object2);
                this.getView().getDrawer().setSelectionBounds((Rectangle)object2);
            }
            this.getView().repaint();
        }
        if (this.selectedTransition != null) {
            object2 = (CurvedArrow)this.getView().getDrawer().transitionToArrowMap.get(this.selectedTransition);
            object = mouseEvent.getPoint();
            Point2D point2D = ((CurvedArrow)object2).getCurve().getCtrlPt();
            if (this.transitionInFlux || Math.sqrt((point2D.getX() - (double)object.x) * (point2D.getX() - (double)object.x) + (point2D.getY() - (double)object.y) * (point2D.getY() - (double)object.y)) < 15.0) {
                this.selectedTransition.setControl((Point)object);
                ((CurvedArrow)object2).refreshCurve();
                this.transitionInFlux = true;
                return;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        this.transitionInFlux = false;
        if (mouseEvent.isPopupTrigger()) {
            this.showPopup(mouseEvent);
        }
        State[] stateArray = this.getView().getDrawer().getAutomaton().getStates();
        int n = 0;
        for (int i = 0; i < stateArray.length; ++i) {
            if (!stateArray[i].isSelected()) continue;
            ++n;
        }
        Rectangle rectangle = this.getView().getDrawer().getSelectionBounds();
        if (n == 1 && rectangle.isEmpty() && this.lastClickedState != null) {
            this.lastClickedState.setSelect(false);
        }
        rectangle = new Rectangle(0, 0, -1, -1);
        this.getView().getDrawer().setSelectionBounds(rectangle);
        this.lastClickedState = null;
        this.lastClickedTransition = null;
        this.getView().repaint();
    }

    @Override
    public KeyStroke getKey() {
        return KeyStroke.getKeyStroke('a');
    }

    public boolean shouldAllowOnlyFinalStateChange() {
        return false;
    }

    private class EmptyMenu
    extends JPopupMenu
    implements ActionListener {
        private Point myPoint;
        private JCheckBoxMenuItem stateLabels = new JCheckBoxMenuItem("Display State Labels");
        private Note curNote;
        private JMenuItem layoutGraph;
        private JMenuItem addNote;
        private JMenuItem renameStates;
        private JMenuItem adaptView;

        public EmptyMenu() {
            this.stateLabels.addActionListener(this);
            this.add(this.stateLabels);
            this.layoutGraph = new JMenuItem("Layout Graph");
            if (!(ArrowTool.this instanceof ArrowDisplayOnlyTool)) {
                this.layoutGraph.addActionListener(this);
                this.add(this.layoutGraph);
            }
            this.renameStates = new JMenuItem("Rename States");
            if (!(ArrowTool.this instanceof ArrowDisplayOnlyTool)) {
                this.renameStates.addActionListener(this);
                this.add(this.renameStates);
            }
            this.addNote = new JMenuItem("Add Note");
            if (!(ArrowTool.this instanceof ArrowDisplayOnlyTool)) {
                this.addNote.addActionListener(this);
                this.add(this.addNote);
            }
            this.adaptView = new JCheckBoxMenuItem("Auto-Zoom");
            if (!(ArrowTool.this instanceof ArrowDisplayOnlyTool)) {
                this.adaptView.addActionListener(this);
                this.add(this.adaptView);
            }
        }

        public void show(Component component, Point point) {
            this.stateLabels.setSelected(ArrowTool.this.getDrawer().doesDrawStateLabels());
            this.adaptView.setSelected(ArrowTool.this.getView().getAdapt());
            this.myPoint = point;
            this.show(component, point.x, point.y);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            JMenuItem jMenuItem = (JMenuItem)actionEvent.getSource();
            if (jMenuItem == this.stateLabels) {
                ArrowTool.this.getView().getDrawer().shouldDrawStateLabels(jMenuItem.isSelected());
            } else if (jMenuItem == this.layoutGraph) {
                AutomatonGraph automatonGraph = new AutomatonGraph(ArrowTool.this.getAutomaton());
                GEMLayoutAlgorithm gEMLayoutAlgorithm = new GEMLayoutAlgorithm();
                ((LayoutAlgorithm)gEMLayoutAlgorithm).layout(automatonGraph, null);
                automatonGraph.moveAutomatonStates();
                ArrowTool.this.getView().fitToBounds(30);
            } else if (jMenuItem == this.renameStates) {
                ((AutomatonEnvironment)ArrowTool.this.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
                StateRenamer.rename(ArrowTool.this.getAutomaton());
            } else if (jMenuItem == this.adaptView) {
                ArrowTool.this.getView().setAdapt(jMenuItem.isSelected());
            } else if (jMenuItem == this.addNote) {
                ((AutomatonEnvironment)ArrowTool.this.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
                Note note = new Note(this.myPoint, "insert_text");
                note.initializeForView(ArrowTool.this.getView());
                ArrowTool.this.getView().getDrawer().getAutomaton().addNote(note);
            }
            ArrowTool.this.getView().repaint();
            ArrowTool.this.emptyMenu = new EmptyMenu();
        }
    }

    private class TransitionMenu
    extends JPopupMenu {
        private TransitionMenu() {
        }
    }

    protected class StateMenu
    extends JPopupMenu
    implements ActionListener {
        private State state;
        protected JCheckBoxMenuItem makeFinal = new JCheckBoxMenuItem("Final");
        protected JCheckBoxMenuItem makeInitial;
        private JMenuItem changeLabel;
        private JMenuItem deleteLabel;
        private JMenuItem deleteAllLabels;
        private JMenuItem editBlock;
        private JMenuItem copyBlock;
        private JMenuItem replaceSymbol;
        private JMenuItem setName;
        private JMenuItem setColor = new JMenuItem("Color...");
        private JMenuItem resetColor = new JMenuItem("Reset color");

        public StateMenu() {
            this.makeFinal.addActionListener(this);
            this.add(this.makeFinal);
            this.makeInitial = new JCheckBoxMenuItem("Initial");
            this.changeLabel = new JMenuItem("Change Label");
            this.deleteLabel = new JMenuItem("Clear Label");
            this.deleteAllLabels = new JMenuItem("Clear All Labels");
            this.editBlock = new JMenuItem("Edit Block");
            this.copyBlock = new JMenuItem("Duplicate Block");
            this.replaceSymbol = new JMenuItem("Replace Symbol");
            this.setName = new JMenuItem("Set Name");
            if (ArrowTool.this.shouldAllowOnlyFinalStateChange()) {
                return;
            }
            this.makeInitial.addActionListener(this);
            this.changeLabel.addActionListener(this);
            this.deleteLabel.addActionListener(this);
            this.deleteAllLabels.addActionListener(this);
            this.editBlock.addActionListener(this);
            this.setName.addActionListener(this);
            this.copyBlock.addActionListener(this);
            this.replaceSymbol.addActionListener(this);
            this.add(this.makeInitial);
            this.add(this.changeLabel);
            this.add(this.deleteLabel);
            this.add(this.deleteAllLabels);
            this.add(this.setName);
            this.setColor.addActionListener(this);
            this.resetColor.addActionListener(this);
            this.add(this.setColor);
            this.add(this.resetColor);
        }

        public void show(State state, Component component, Point point) {
            this.remove(this.editBlock);
            this.state = state;
            if (state instanceof TMState) {
                this.add(this.editBlock);
                this.add(this.copyBlock);
                this.editBlock.setEnabled(true);
                this.copyBlock.setEnabled(true);
                this.add(this.replaceSymbol);
                this.replaceSymbol.setEnabled(true);
            }
            this.makeFinal.setSelected(ArrowTool.this.getAutomaton().isFinalState(state));
            this.makeInitial.setSelected(ArrowTool.this.getAutomaton().getInitialState() == state);
            this.deleteLabel.setEnabled(state.getLabel() != null);
            this.show(component, point.x, point.y);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            JMenuItem jMenuItem = (JMenuItem)actionEvent.getSource();
            if (ArrowTool.this.getDrawer().getAutomaton().getEnvironmentFrame() != null) {
                ((AutomatonEnvironment)ArrowTool.this.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
            }
            if (jMenuItem == this.makeFinal) {
                if (jMenuItem.isSelected()) {
                    ArrowTool.this.getAutomaton().addFinalState(this.state);
                } else {
                    ArrowTool.this.getAutomaton().removeFinalState(this.state);
                }
            } else if (jMenuItem == this.makeInitial) {
                if (!jMenuItem.isSelected()) {
                    this.state = null;
                }
                ArrowTool.this.getAutomaton().setInitialState(this.state);
            } else if (jMenuItem == this.changeLabel) {
                String string = this.state.getLabel();
                string = string == null ? "" : string;
                String string2 = (String)JOptionPane.showInputDialog(this, "Input a new label, or \nset blank to remove the label", "New Label", 3, null, null, string);
                if (string2 == null) {
                    return;
                }
                if (string2.equals("")) {
                    string2 = null;
                }
                this.state.setLabel(string2);
            } else if (jMenuItem == this.deleteLabel) {
                this.state.setLabel(null);
            } else if (jMenuItem == this.deleteAllLabels) {
                State[] stateArray = ArrowTool.this.getAutomaton().getStates();
                for (int i = 0; i < stateArray.length; ++i) {
                    stateArray[i].setLabel(null);
                }
            } else if (jMenuItem == this.editBlock) {
                TMState tMState = (TMState)this.state;
                while (((TuringMachine)tMState.getAutomaton()).getParent() != null) {
                    tMState = ((TuringMachine)tMState.getAutomaton()).getParent();
                }
                EditBlockPane editBlockPane = new EditBlockPane(((TMState)this.state).getInnerTM());
                EnvironmentFrame environmentFrame = tMState.getAutomaton().getEnvironmentFrame();
                editBlockPane.setBlock(this.state);
                Environment environment = environmentFrame.getEnvironment();
                environment.add((Component)editBlockPane, "Edit Block", new CriticalTag(){});
                environment.setActive(editBlockPane);
            } else if (jMenuItem == this.setColor || jMenuItem == this.resetColor) {
                java.awt.Color color = null;
                if (jMenuItem == this.setColor) {
                    java.awt.Color current = this.state.getFillColor() != null ? this.state.getFillColor() : plus.Theme.STATE_FILL;
                    color = javax.swing.JColorChooser.showDialog(this, "Node color", current);
                    if (color == null) {
                        return;
                    }
                }
                this.state.setFillColor(color);
                State[] all = ArrowTool.this.getAutomaton().getStates();
                for (int i = 0; i < all.length; ++i) {
                    if (all[i].isSelected()) {
                        all[i].setFillColor(color);
                    }
                }
            } else if (jMenuItem == this.setName) {
                String string = this.state.getName();
                string = string == null ? "" : string;
                String string3 = (String)JOptionPane.showInputDialog(this, "Input a new name, or \nset blank to remove the name", "New Name", 3, null, null, string);
                if (string3 == null) {
                    return;
                }
                if (string3.equals("")) {
                    string3 = null;
                }
                this.state.setName(string3);
            } else if (jMenuItem == this.copyBlock) {
                TMState tMState = ((TuringMachine)ArrowTool.this.getAutomaton()).createTMState(new Point(this.state.getPoint().x + 4, this.state.getPoint().y));
                tMState.setInnerTM((TuringMachine)((TMState)this.state).getInnerTM().clone());
            } else if (jMenuItem == this.replaceSymbol) {
                String string;
                assert (this.state instanceof TMState);
                String string4 = null;
                String string5 = null;
                String string6 = JOptionPane.showInputDialog(null, (Object)"Find");
                if (string6 == null) {
                    return;
                }
                if (string6 instanceof String) {
                    string5 = string6;
                }
                if ((string = JOptionPane.showInputDialog(null, (Object)"Replace With")) == null) {
                    return;
                }
                if (string instanceof String) {
                    string4 = string;
                }
                this.replaceCharactersInBlock((TMState)this.state, string5, string4);
            }
            ArrowTool.this.getView().repaint();
        }

        private void replaceCharactersInBlock(TMState tMState, String string, String string2) {
            TuringMachine turingMachine = tMState.getInnerTM();
            for (int i = 0; i < turingMachine.getStates().length; ++i) {
                this.replaceCharactersInBlock((TMState)turingMachine.getStates()[i], string, string2);
            }
            Transition[] transitionArray = turingMachine.getTransitions();
            for (int i = 0; i < transitionArray.length; ++i) {
                TMTransition tMTransition = (TMTransition)transitionArray[i];
                for (int j = 0; j < tMTransition.tapes(); ++j) {
                    String string3 = tMTransition.getRead(j);
                    tMTransition.setRead(j, string3.replaceAll(string, string2));
                    String string4 = tMTransition.getWrite(j);
                    tMTransition.setWrite(j, string4.replaceAll(string, string2));
                }
            }
        }
    }
}

