/*
 * Decompiled with CFR 0.152.
 */
package automata;

import automata.IncompatibleTransitionException;
import automata.Note;
import automata.State;
import automata.Transition;
import automata.event.AutomataNoteEvent;
import automata.event.AutomataNoteListener;
import automata.event.AutomataStateEvent;
import automata.event.AutomataStateListener;
import automata.event.AutomataTransitionEvent;
import automata.event.AutomataTransitionListener;
import automata.mealy.MooreMachine;
import gui.environment.EnvironmentFrame;
import gui.viewer.AutomatonPane;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.text.JTextComponent;

public class Automaton
implements Serializable,
Cloneable {
    private String fileName = "";
    private EnvironmentFrame myEnvFrame = null;
    protected Set<State> states;
    private State[] cachedStates = null;
    private Transition[] cachedTransitions = null;
    private State[] cachedFinalStates = null;
    protected Set<State> finalStates;
    protected State initialState = null;
    protected Set<Transition> transitions;
    private HashMap transitionFromStateMap = new HashMap();
    private HashMap transitionToStateMap = new HashMap();
    private HashMap transitionArrayFromStateMap = new HashMap();
    private HashMap transitionArrayToStateMap = new HashMap();
    private ArrayList myNotes = new ArrayList();
    public Color myColor = new Color(255, 255, 150);
    private transient HashSet<AutomataTransitionListener> transitionListeners = new HashSet();
    private transient HashSet<AutomataStateListener> stateListeners = new HashSet();
    private transient HashSet<AutomataNoteListener> noteListeners = new HashSet();

    public Automaton() {
        this.states = new HashSet();
        this.transitions = new HashSet();
        this.finalStates = new HashSet();
    }

    public Object clone() {
        Automaton automaton;
        try {
            automaton = (Automaton)this.getClass().newInstance();
        }
        catch (Throwable throwable) {
            System.err.println("Warning: clone of automaton failed!");
            return null;
        }
        automaton.setEnvironmentFrame(this.getEnvironmentFrame());
        copyContents(this, automaton, false);
        return automaton;
    }

    /** Copia estados, transiciones y notas de src a dst (usado por clone y become). */
    private static void copyContents(Automaton src, Automaton dst, boolean init) {
        HashMap<State, State> map = new HashMap<State, State>();
        for (State state : src.states) {
            State copy = new State(state.getID(), new Point(state.getPoint()), dst);
            copy.setLabel(state.getLabel());
            copy.setName(state.getName());
            copy.setFillColor(state.getFillColor());
            map.put(state, copy);
            dst.addState(copy);
            if (!(src instanceof MooreMachine)) continue;
            ((MooreMachine)dst).setOutput(copy, ((MooreMachine)src).getOutput(state));
        }
        for (State o : src.finalStates) {
            dst.addFinalState(map.get(o));
        }
        dst.setInitialState(map.get(src.getInitialState()));
        for (State state : src.states) {
            Transition[] transitions = src.getTransitionsFromState(state);
            State from = map.get(state);
            for (int i = 0; i < transitions.length; ++i) {
                State to = map.get(transitions[i].getToState());
                Transition transition = (Transition)transitions[i].clone();
                transition.setFromState(from);
                transition.setToState(to);
                dst.addTransition(transition);
            }
        }
        for (int i = 0; i < src.getNotes().size(); ++i) {
            Note note = (Note)src.getNotes().get(i);
            Note copy = new Note(note.getAutoPoint(), ((JTextComponent)note).getText());
            copy.copyStyleFrom(note);
            dst.addNote(copy);
            if (init) {
                ((Note)dst.getNotes().get(i)).initializeForView(note.getView());
            } else {
                ((Note)dst.getNotes().get(i)).setView(note.getView());
            }
        }
    }

    public static void become(Automaton automaton, Automaton automaton2) {
        automaton.clear();
        copyContents(automaton2, automaton, true);
        automaton.setEnvironmentFrame(automaton2.getEnvironmentFrame());
    }

    public Transition[] getTransitionsFromState(State state) {
        Transition[] transitionArray = (Transition[])this.transitionArrayFromStateMap.get(state);
        if (transitionArray == null) {
            List<Transition> list = (List<Transition>)this.transitionFromStateMap.get(state);
            transitionArray = list.toArray(new Transition[0]);
            this.transitionArrayFromStateMap.put(state, transitionArray);
        }
        return transitionArray;
    }

    public Transition[] getTransitionsToState(State state) {
        Transition[] transitionArray = (Transition[])this.transitionArrayToStateMap.get(state);
        if (transitionArray == null) {
            List<Transition> list = (List<Transition>)this.transitionToStateMap.get(state);
            transitionArray = list.toArray(new Transition[0]);
            this.transitionArrayToStateMap.put(state, transitionArray);
        }
        return transitionArray;
    }

    public Transition[] getTransitionsFromStateToState(State state, State state2) {
        Transition[] transitionArray = this.getTransitionsFromState(state);
        ArrayList<Transition> arrayList = new ArrayList<Transition>();
        for (int i = 0; i < transitionArray.length; ++i) {
            if (transitionArray[i].getToState() != state2) continue;
            arrayList.add(transitionArray[i]);
        }
        return arrayList.toArray(new Transition[0]);
    }

    public Transition[] getTransitions() {
        if (this.cachedTransitions == null) {
            this.cachedTransitions = this.transitions.toArray(new Transition[0]);
        }
        return this.cachedTransitions;
    }

    public void addTransition(Transition transition) {
        if (!this.getTransitionClass().isInstance(transition) || transition == null) {
            throw new IncompatibleTransitionException();
        }
        if (this.transitions.contains(transition)) {
            return;
        }
        if (transition.getToState() == null || transition.getFromState() == null) {
            return;
        }
        this.transitions.add(transition);
        if (this.transitionFromStateMap == null) {
            this.transitionFromStateMap = new HashMap();
        }
        List<Transition> list = (List<Transition>)this.transitionFromStateMap.get(transition.getFromState());
        list.add(transition);
        if (this.transitionToStateMap == null) {
            this.transitionToStateMap = new HashMap();
        }
        list = (List)this.transitionToStateMap.get(transition.getToState());
        list.add(transition);
        this.transitionArrayFromStateMap.remove(transition.getFromState());
        this.transitionArrayToStateMap.remove(transition.getToState());
        this.cachedTransitions = null;
        this.distributeTransitionEvent(new AutomataTransitionEvent(this, transition, true, false));
    }

    public void replaceTransition(Transition transition, Transition transition2) {
        if (!this.getTransitionClass().isInstance(transition2)) {
            throw new IncompatibleTransitionException();
        }
        if (transition.equals(transition2)) {
            return;
        }
        if (this.transitions.contains(transition2)) {
            this.removeTransition(transition);
            return;
        }
        if (!this.transitions.remove(transition)) {
            throw new IllegalArgumentException("Replacing transition that not already in the automaton!");
        }
        this.transitions.add(transition2);
        List<Transition> list = (List<Transition>)this.transitionFromStateMap.get(transition.getFromState());
        list.set(list.indexOf(transition), transition2);
        list = (List)this.transitionToStateMap.get(transition.getToState());
        list.set(list.indexOf(transition), transition2);
        this.transitionArrayFromStateMap.remove(transition.getFromState());
        this.transitionArrayToStateMap.remove(transition.getToState());
        this.cachedTransitions = null;
        this.distributeTransitionEvent(new AutomataTransitionEvent(this, transition2, true, false));
    }

    public void removeTransition(Transition transition) {
        this.transitions.remove(transition);
        List<Transition> list = (List<Transition>)this.transitionFromStateMap.get(transition.getFromState());
        list.remove(transition);
        list = (List)this.transitionToStateMap.get(transition.getToState());
        list.remove(transition);
        this.transitionArrayFromStateMap.remove(transition.getFromState());
        this.transitionArrayToStateMap.remove(transition.getToState());
        this.cachedTransitions = null;
        this.distributeTransitionEvent(new AutomataTransitionEvent(this, transition, false, false));
    }

    public static List makeListFromArray(Object[] objectArray) {
        ArrayList<Object> arrayList = new ArrayList<Object>();
        for (int i = 0; i < objectArray.length; ++i) {
            arrayList.add(objectArray[i]);
        }
        return arrayList;
    }

    public State createState(Point point) {
        int n = 0;
        while (this.getStateWithID(n) != null) {
            ++n;
        }
        State state = new State(n, point, this);
        this.addState(state);
        return state;
    }

    public final State createStateWithId(Point point, int n) {
        State state = new State(n, point, this);
        this.addState(state);
        return state;
    }

    protected final void addState(State state) {
        this.states.add(state);
        this.transitionFromStateMap.put(state, new LinkedList());
        this.transitionToStateMap.put(state, new LinkedList());
        this.cachedStates = null;
        this.distributeStateEvent(new AutomataStateEvent(this, state, true, false, false));
    }

    public void removeState(State state) {
        int n;
        Transition[] transitionArray = this.getTransitionsFromState(state);
        for (n = 0; n < transitionArray.length; ++n) {
            this.removeTransition(transitionArray[n]);
        }
        transitionArray = this.getTransitionsToState(state);
        for (n = 0; n < transitionArray.length; ++n) {
            this.removeTransition(transitionArray[n]);
        }
        this.distributeStateEvent(new AutomataStateEvent(this, state, false, false, false));
        this.states.remove(state);
        this.finalStates.remove(state);
        if (state == this.initialState) {
            this.initialState = null;
        }
        this.transitionFromStateMap.remove(state);
        this.transitionToStateMap.remove(state);
        this.transitionArrayFromStateMap.remove(state);
        this.transitionArrayToStateMap.remove(state);
        this.cachedStates = null;
    }

    public State setInitialState(State state) {
        State state2 = this.initialState;
        this.initialState = state;
        this.distributeStateEvent(new AutomataStateEvent(this, state, false, false, true));
        return state2;
    }

    public State getInitialState() {
        return this.initialState;
    }

    public State[] getStates() {
        if (this.cachedStates == null) {
            this.cachedStates = this.states.toArray(new State[0]);
            Arrays.sort(this.cachedStates, new Comparator(){

                public int compare(Object object, Object object2) {
                    return ((State)object).getID() - ((State)object2).getID();
                }

                @Override
                public boolean equals(Object object) {
                    return this == object;
                }
            });
        }
        return this.cachedStates;
    }

    public void selectStatesWithinBounds(Rectangle rectangle) {
        State[] stateArray = this.getStates();
        for (int i = 0; i < stateArray.length; ++i) {
            stateArray[i].setSelect(false);
            if (!rectangle.contains(stateArray[i].getPoint())) continue;
            stateArray[i].setSelect(true);
        }
    }

    public ArrayList getNotes() {
        return this.myNotes;
    }

    public void addNote(Note note) {
        this.myNotes.add(note);
        this.distributeNoteEvent(new AutomataNoteEvent(this, note, true, false));
    }

    public void deleteNote(Note note) {
        for (int i = 0; i < this.myNotes.size(); ++i) {
            if (note != this.myNotes.get(i)) continue;
            this.myNotes.remove(i);
        }
        this.distributeNoteEvent(new AutomataNoteEvent(this, note, true, false));
    }

    public void addFinalState(State state) {
        this.cachedFinalStates = null;
        this.finalStates.add(state);
        this.distributeStateEvent(new AutomataStateEvent(this, state, false, false, true));
    }

    public void removeFinalState(State state) {
        this.cachedFinalStates = null;
        this.finalStates.remove(state);
        this.distributeStateEvent(new AutomataStateEvent(this, state, false, false, true));
    }

    public State[] getFinalStates() {
        if (this.cachedFinalStates == null) {
            this.cachedFinalStates = this.finalStates.toArray(new State[0]);
        }
        return this.cachedFinalStates;
    }

    public boolean isFinalState(State state) {
        return this.finalStates.contains(state);
    }

    public boolean isInitialState(State state) {
        return state.equals(this.initialState);
    }

    public State getStateWithID(int n) {
        for (State state : this.states) {
            if (state.getID() != n) continue;
            return state;
        }
        return null;
    }

    public boolean isState(State state) {
        return this.states.contains(state);
    }

    protected Class getTransitionClass() {
        return Transition.class;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(super.toString());
        stringBuffer.append('\n');
        State[] stateArray = this.getStates();
        for (int i = 0; i < stateArray.length; ++i) {
            if (this.initialState == stateArray[i]) {
                stringBuffer.append("--> ");
            }
            stringBuffer.append(stateArray[i]);
            if (this.isFinalState(stateArray[i])) {
                stringBuffer.append(" **FINAL**");
            }
            stringBuffer.append('\n');
            Transition[] transitionArray = this.getTransitionsFromState(stateArray[i]);
            for (int j = 0; j < transitionArray.length; ++j) {
                stringBuffer.append('\t');
                stringBuffer.append(transitionArray[j]);
                stringBuffer.append('\n');
            }
        }
        return stringBuffer.toString();
    }

    public void addStateListener(AutomataStateListener automataStateListener) {
        this.stateListeners.add(automataStateListener);
    }

    public void addTransitionListener(AutomataTransitionListener automataTransitionListener) {
        this.transitionListeners.add(automataTransitionListener);
    }

    public void addNoteListener(AutomataNoteListener automataNoteListener) {
        this.noteListeners.add(automataNoteListener);
    }

    void distributeStateEvent(AutomataStateEvent automataStateEvent) {
        for (AutomataStateListener automataStateListener : this.stateListeners) {
            automataStateListener.automataStateChange(automataStateEvent);
        }
    }

    public void removeStateListener(AutomataStateListener automataStateListener) {
        this.stateListeners.remove(automataStateListener);
    }

    public void removeTransitionListener(AutomataTransitionListener automataTransitionListener) {
        this.transitionListeners.remove(automataTransitionListener);
    }

    public void removeNoteListener(AutomataNoteListener automataNoteListener) {
        this.noteListeners.remove(automataNoteListener);
    }

    void distributeTransitionEvent(AutomataTransitionEvent automataTransitionEvent) {
        for (AutomataTransitionListener automataTransitionListener : this.transitionListeners) {
            automataTransitionListener.automataTransitionChange(automataTransitionEvent);
        }
    }

    void distributeNoteEvent(AutomataNoteEvent automataNoteEvent) {
        for (AutomataNoteListener automataNoteListener : this.noteListeners) {
            automataNoteListener.automataNoteChange(automataNoteEvent);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
    }

    public EnvironmentFrame getEnvironmentFrame() {
        return this.myEnvFrame;
    }

    public void setEnvironmentFrame(EnvironmentFrame environmentFrame) {
        this.myEnvFrame = environmentFrame;
    }

    public void setFilePath(String string) {
        this.fileName = string;
    }

    public String getFileName() {
        int n = this.fileName.lastIndexOf("\\");
        if (n == -1) {
            n = this.fileName.lastIndexOf("/");
        }
        return this.fileName.substring(n + 1);
    }

    public String getFilePath() {
        int n = this.fileName.lastIndexOf("\\");
        if (n == -1) {
            n = this.fileName.lastIndexOf("/");
        }
        return this.fileName.substring(0, n + 1);
    }

    public int hashCode() {
        int n = 0;
        for (Object e : this.states) {
            n += ((State)e).specialHash();
        }
        for (Object e : this.transitions) {
            n += ((Transition)e).specialHash();
        }
        for (Object e : this.myNotes) {
            n += ((Note)e).specialHash();
        }
        n += ((Object)this.finalStates).hashCode();
        return n += this.initialState == null ? 0 : (int)((double)this.initialState.specialHash() * Math.PI);
    }

    protected void clear() {
        HashSet hashSet = new HashSet(this.transitions);
        for (Object e : hashSet) {
            this.removeTransition((Transition)e);
        }
        this.transitions = new HashSet();
        hashSet = new HashSet(this.states);
        for (Object e : hashSet) {
            this.removeState((State)e);
        }
        this.states = new HashSet();
        this.finalStates = new HashSet();
        this.initialState = null;
        this.cachedStates = null;
        this.cachedTransitions = null;
        this.cachedFinalStates = null;
        this.transitionFromStateMap = new HashMap();
        this.transitionToStateMap = new HashMap();
        this.transitionArrayFromStateMap = new HashMap();
        this.transitionArrayToStateMap = new HashMap();
        while (this.myNotes.size() != 0) {
            AutomatonPane automatonPane = ((Note)this.myNotes.get(0)).getView();
            automatonPane.remove((Note)this.myNotes.get(0));
            automatonPane.repaint();
            this.deleteNote((Note)this.myNotes.get(0));
        }
    }
}

