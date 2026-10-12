/*
 * Decompiled with CFR 0.152.
 */
package automata.turing;

import automata.Automaton;
import automata.AutomatonSimulator;
import automata.Configuration;
import automata.Transition;
import automata.turing.AcceptByFinalStateFilter;
import automata.turing.AcceptByHaltingFilter;
import automata.turing.AcceptanceFilter;
import automata.turing.TMConfiguration;
import automata.turing.TMState;
import automata.turing.TMTransition;
import automata.turing.Tape;
import automata.turing.TuringMachine;
import debug.EDebug;
import gui.environment.Universe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;

public class TMSimulator
extends AutomatonSimulator {
    private String[] inputStrings;
    private Map<String, String> varToChar = new HashMap<String, String>();
    private AcceptanceFilter[] myFilters;

    public TMSimulator(Automaton automaton) {
        super(automaton);
        if (!(automaton instanceof TuringMachine)) {
            throw new IllegalArgumentException("Automaton is not a Turing machine, but a " + automaton.getClass());
        }
        ArrayList<AcceptanceFilter> arrayList = new ArrayList<AcceptanceFilter>();
        if (Universe.curProfile.getAcceptByFinalState()) {
            arrayList.add(new AcceptByFinalStateFilter());
        }
        if (Universe.curProfile.getAcceptByHalting()) {
            arrayList.add(new AcceptByHaltingFilter());
        }
        this.myFilters = arrayList.toArray(new AcceptanceFilter[0]);
    }

    @Override
    public Configuration[] getInitialConfigurations(String string) {
        int n = ((TuringMachine)this.myAutomaton).tapes();
        String[] stringArray = new String[n];
        for (int i = 0; i < n; ++i) {
            stringArray[i] = string;
        }
        return this.getInitialConfigurations(stringArray);
    }

    public Configuration[] getInitialConfigurations(String[] stringArray) {
        this.inputStrings = (String[])stringArray.clone();
        Tape[] tapeArray = new Tape[stringArray.length];
        for (int i = 0; i < tapeArray.length; ++i) {
            tapeArray[i] = new Tape(stringArray[i]);
        }
        Configuration[] configurationArray = new Configuration[1];
        TMState tMState = (TMState)this.myAutomaton.getInitialState();
        TuringMachine turingMachine = tMState.getInnerTM();
        configurationArray[0] = new TMConfiguration(tMState, null, tapeArray, this.myFilters);
        return configurationArray;
    }

    private boolean matches(Tape[] tapeArray, TMTransition tMTransition) {
        assert (tapeArray.length == tMTransition.tapes());
        if (tapeArray.length > 1) {
            for (int i = 0; i < tapeArray.length; ++i) {
                char c;
                char c2 = tapeArray[i].readChar();
                if (plus.LabelMatcher.isClassExpression(tMTransition.getRead(i))) {
                    if (plus.LabelMatcher.matchesChar(tMTransition.getRead(i), c2)) continue;
                    return false;
                }
                if (c2 == (c = tMTransition.getRead(i).charAt(0)) || c == '~') continue;
                return false;
            }
            return true;
        }
        assert (tapeArray.length == 1);
        char c = tapeArray[0].readChar();
        String string = tMTransition.getRead(0);
        if (plus.LabelMatcher.isClassExpression(string)) {
            return plus.LabelMatcher.matchesChar(string, c);
        }
        int n = string.indexOf(125);
        int n2 = string.indexOf(33);
        assert (n == -1 || n2 == -1);
        if (n == -1 && n2 == -1) {
            return c == string.charAt(0) || string.charAt(0) == '~';
        }
        if (n != -1) {
            String[] stringArray = string.substring(0, n).split(",");
            boolean bl = false;
            for (int i = 0; i < stringArray.length; ++i) {
                assert (stringArray[i].length() == 1);
                if (this.varToChar.containsKey(stringArray[i])) {
                    JOptionPane.showMessageDialog(null, "You cannot use a variable on the left side of the assignment operator!\n Please fix this and restart the simulation.", "Illegal Variable Location!\n", 0);
                }
                if (stringArray[i].charAt(0) != c) continue;
                bl = true;
            }
            if (bl) {
                return bl;
            }
        } else {
            assert (n2 == 0);
            return c != string.charAt(1);
        }
        assert (false);
        return true;
    }

    public List stepBlock(TMConfiguration tMConfiguration) {
        EDebug.print("Inside StepBlock");
        while (((TuringMachine)(tMConfiguration = (TMConfiguration)this.stepConfiguration(tMConfiguration).get(0)).getCurrentState().getAutomaton()).getParent() != null) {
        }
        return Arrays.asList(tMConfiguration);
    }

    @Override
    public ArrayList stepConfiguration(Configuration configuration) {
        int n;
        ArrayList<TMConfiguration> arrayList = new ArrayList<TMConfiguration>();
        TMConfiguration tMConfiguration = (TMConfiguration)configuration;
        TMState tMState = (TMState)tMConfiguration.getCurrentState();
        TuringMachine turingMachine = null;
        int n2 = 0;
        while ((turingMachine = tMState.getInnerTM()).getStates().length != 0) {
            EDebug.print(n2++);
            tMState = (TMState)turingMachine.getInitialState();
            if (tMState != null) continue;
            JOptionPane.showMessageDialog(null, "It appears that one of your building blocks, possibly nested, lacks an initial state.\n Please resolve this problem and restart the simulation.", "Missing Initial State", 0);
            return arrayList;
        }
        assert (turingMachine == tMState.getInnerTM());
        assert (turingMachine.getParent() == tMState);
        Transition[] transitionArray = tMState.getAutomaton().getTransitionsFromState(tMState);
        TMTransition tMTransition = null;
        boolean bl = false;
        block1: while (true) {
            Arrays.sort(transitionArray, new Comparator<Transition>(){

                @Override
                public int compare(Transition transition, Transition transition2) {
                    TMTransition tMTransition = (TMTransition)transition;
                    TMTransition tMTransition2 = (TMTransition)transition2;
                    char c = tMTransition.getRead(0).charAt(0);
                    char c2 = tMTransition2.getRead(0).charAt(0);
                    return c == '!' ? (c2 == '!' ? 0 : 1) : (c2 == '!' ? 1 : 0);
                }
            });
            for (n = 0; n < transitionArray.length; ++n) {
                tMTransition = (TMTransition)transitionArray[n];
                if (!this.matches(tMConfiguration.getTapes(), tMTransition)) continue;
                bl = true;
                break block1;
            }
            if (turingMachine.getParent() == null) break;
            tMState = turingMachine.getParent();
            turingMachine = (TuringMachine)tMState.getAutomaton();
            transitionArray = turingMachine.getTransitionsFromState(tMState);
        }
        if (bl) {
            if (tMConfiguration.getTapes().length > 1) {
                for (n = 0; n < tMConfiguration.getTapes().length; ++n) {
                    tMConfiguration.getTapes()[n].writeChar(tMTransition.getWrite(n).charAt(0) == '~' ? tMConfiguration.getTapes()[n].readChar() : tMTransition.getWrite(n).charAt(0));
                    tMConfiguration.getTapes()[n].moveHead(tMTransition.getDirection(n));
                    arrayList.add(new TMConfiguration(tMTransition.getToState(), null, tMConfiguration.getTapes(), this.myFilters));
                }
            } else {
                String string = tMTransition.getRead(0);
                int n3 = string.indexOf(125);
                if (n3 != -1) {
                    String string2 = "" + string.charAt(n3 + 1);
                    this.varToChar.put(string2, tMConfiguration.getTapes()[0].readChar() + "");
                }
                tMConfiguration.getTapes()[0].writeChar(tMTransition.getWrite(0).charAt(0) == '~' ? tMConfiguration.getTapes()[0].readChar() : (this.varToChar.containsKey(tMTransition.getWrite(0).charAt(0) + "") ? this.varToChar.get(tMTransition.getWrite(0).charAt(0) + "").charAt(0) : tMTransition.getWrite(0).charAt(0)));
                tMConfiguration.getTapes()[0].moveHead(tMTransition.getDirection(0));
                arrayList.add(new TMConfiguration(tMTransition.getToState(), null, tMConfiguration.getTapes(), this.myFilters));
            }
        } else if (!tMConfiguration.isHalted()) {
            tMConfiguration.setHalted(true);
            arrayList.add(tMConfiguration);
        }
        return arrayList;
    }

    @Override
    public boolean isAccepted() {
        return false;
    }

    @Override
    public boolean simulateInput(String string) {
        Object object;
        this.myConfigurations.clear();
        Configuration[] configurationArray = this.getInitialConfigurations(string);
        for (int i = 0; i < configurationArray.length; ++i) {
            object = (TMConfiguration)configurationArray[i];
            this.myConfigurations.add(object);
        }
        while (!this.myConfigurations.isEmpty()) {
            if (this.isAccepted()) {
                return true;
            }
            ArrayList arrayList = new ArrayList();
            java.util.Iterator cfgIterator = this.myConfigurations.iterator();
            while (cfgIterator.hasNext()) {
                TMConfiguration tMConfiguration = (TMConfiguration)cfgIterator.next();
                ArrayList arrayList2 = this.stepConfiguration(tMConfiguration);
                arrayList.addAll(arrayList2);
                cfgIterator.remove();
            }
            this.myConfigurations.addAll(arrayList);
        }
        return false;
    }

    public String[] getInputStrings() {
        return this.inputStrings;
    }
}

