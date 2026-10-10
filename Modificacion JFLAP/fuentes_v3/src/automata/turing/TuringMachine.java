/*
 * Decompiled with CFR 0.152.
 */
package automata.turing;

import automata.Automaton;
import automata.Note;
import automata.State;
import automata.Transition;
import automata.turing.TMState;
import automata.turing.TMTransition;
import gui.action.OpenAction;
import java.awt.Point;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.text.JTextComponent;

public class TuringMachine
extends Automaton {
    public int tapes;
    public boolean isOuterMost;
    private TMState parent = null;

    public TuringMachine() {
        this(1);
    }

    public TuringMachine(int n) {
        this.tapes = n;
    }

    @Override
    protected Class getTransitionClass() {
        return TMTransition.class;
    }

    @Override
    public void addTransition(Transition transition) {
        try {
            int n = ((TMTransition)transition).tapes();
            if (this.tapes == 0) {
                this.tapes = n;
            }
            if (n != this.tapes) {
                throw new IllegalArgumentException("Transition has " + n + " tapes while TM has " + this.tapes);
            }
            super.addTransition(transition);
        }
        catch (ClassCastException classCastException) {
            // empty catch block
        }
    }

    public int tapes() {
        return this.tapes;
    }

    @Override
    public State createState(Point point) {
        return this.createTMState(point);
    }

    @Override
    public Object clone() {
        TuringMachine turingMachine = new TuringMachine(this.tapes());
        turingMachine.setEnvironmentFrame(this.getEnvironmentFrame());
        HashMap<State, TMState> map = new HashMap<State, TMState>();
        for (Object e : this.states) {
            TMState old = (TMState)e;
            TMState copy = new TMState(old);
            copy.setAutomaton(turingMachine);
            copy.setLabel(old.getLabel());
            copy.setName(old.getName());
            copy.setFillColor(old.getFillColor());
            map.put(old, copy);
            turingMachine.addState((State)copy);
        }
        for (Object e : this.finalStates) {
            turingMachine.addFinalState((State)map.get((TMState)e));
        }
        turingMachine.setInitialState((TMState)map.get((TMState)this.getInitialState()));
        for (Object e : this.states) {
            TMState old = (TMState)e;
            Transition[] transitions = this.getTransitionsFromState(old);
            TMState from = map.get(old);
            for (int i = 0; i < transitions.length; ++i) {
                TMState to = map.get(transitions[i].getToState());
                Transition transition = (Transition)transitions[i].clone();
                transition.setFromState(from);
                transition.setToState(to);
                turingMachine.addTransition(transition);
            }
        }
        for (int i = 0; i < this.getNotes().size(); ++i) {
            Note note = (Note)this.getNotes().get(i);
            Note copy = new Note(note.getAutoPoint(), ((JTextComponent)note).getText());
            copy.copyStyleFrom(note);
            turingMachine.addNote(copy);
            copy.setView(note.getView());
        }
        return turingMachine;
    }

    public final TMState createBlock(Point point) {
        int n = 0;
        while (this.getStateWithID(n) != null) {
            ++n;
        }
        OpenAction openAction = new OpenAction();
        OpenAction.setOpenOrRead(true);
        JButton jButton = new JButton(openAction);
        jButton.doClick();
        OpenAction.setOpenOrRead(false);
        return this.getAutomatonFromFile(n, point);
    }

    private TMState getAutomatonFromFile(int n, Point point) {
        TMState tMState = new TMState(n, point, this);
        Serializable serializable = OpenAction.getLastObjectOpened();
        File file = OpenAction.getLastFileOpened();
        if (file == null || !OpenAction.isOpened()) {
            return null;
        }
        assert (serializable instanceof TuringMachine);
        TuringMachine turingMachine = (TuringMachine)serializable;
        turingMachine.setEnvironmentFrame(this.getEnvironmentFrame());
        tMState.setInternalName(file.getName());
        tMState.setInnerTM(turingMachine);
        tMState.setName(file.getName().substring(0, file.getName().length() - 4));
        this.addState(tMState);
        return tMState;
    }

    public final TMState createTMStateWithID(Point point, int n) {
        TMState tMState = new TMState(n, point, this);
        this.addState(tMState);
        return tMState;
    }

    public final TMState createTMState(Point point) {
        int n = 0;
        while (this.getStateWithID(n) != null) {
            ++n;
        }
        TMState tMState = new TMState(n, point, this);
        this.addState(tMState);
        return tMState;
    }

    public TMState createInnerTM(Point point, Serializable serializable, String string, int n) {
        TMState tMState = new TMState(n, point, this);
        TuringMachine turingMachine = (TuringMachine)serializable;
        this.addState(tMState);
        tMState.setInnerTM(turingMachine);
        tMState.setInternalName(string);
        return tMState;
    }

    public static void become(TuringMachine turingMachine, TuringMachine turingMachine2) {
        System.out.println("Calling the real become");
        turingMachine.clear();
        HashMap<State, TMState> map = new HashMap<State, TMState>();
        for (Object object : turingMachine2.states) {
            TMState old = (TMState)object;
            TMState copy = new TMState(old.getID(), new Point(old.getPoint()), turingMachine);
            copy.setLabel(old.getLabel());
            copy.setName(old.getName());
            copy.setFillColor(old.getFillColor());
            map.put(old, copy);
            turingMachine.addState((State)copy);
        }
        for (Object object : turingMachine2.finalStates) {
            turingMachine.addFinalState((State)map.get((TMState)object));
        }
        turingMachine.setInitialState((TMState)map.get((TMState)turingMachine2.getInitialState()));
        for (Object object : turingMachine2.states) {
            TMState old = (TMState)object;
            Transition[] transitions = turingMachine2.getTransitionsFromState(old);
            TMState from = map.get(old);
            for (int i = 0; i < transitions.length; ++i) {
                TMState to = map.get(transitions[i].getToState());
                Transition transition = (Transition)transitions[i].clone();
                transition.setFromState(from);
                transition.setToState(to);
                turingMachine.addTransition(transition);
            }
        }
        for (int i = 0; i < turingMachine2.getNotes().size(); ++i) {
            Note note = (Note)turingMachine2.getNotes().get(i);
            Note copy = new Note(note.getAutoPoint(), ((JTextComponent)note).getText());
            copy.copyStyleFrom(note);
            turingMachine.addNote(copy);
            ((Note)turingMachine.getNotes().get(i)).initializeForView(note.getView());
        }
        turingMachine.setEnvironmentFrame(turingMachine2.getEnvironmentFrame());
    }

    public Map<String, TuringMachine> getBlockMap() {
        HashMap<String, TuringMachine> hashMap = new HashMap<String, TuringMachine>();
        for (State state : this.states) {
            TMState tMState = (TMState)state;
            hashMap.put(tMState.getInternalName(), tMState.getInnerTM());
        }
        return hashMap;
    }

    public void setParent(TMState tMState) {
        this.parent = tMState;
    }

    public TMState getParent() {
        return this.parent;
    }
}

