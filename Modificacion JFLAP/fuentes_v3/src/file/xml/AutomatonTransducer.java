/*
 * Decompiled with CFR 0.152.
 */
package file.xml;

import automata.Automaton;
import automata.Note;
import automata.State;
import automata.Transition;
import automata.graph.AutomatonGraph;
import automata.graph.LayoutAlgorithm;
import automata.graph.layout.GEMLayoutAlgorithm;
import automata.mealy.MooreMachine;
import automata.turing.TMState;
import automata.turing.TuringMachine;
import file.DataException;
import file.xml.AbstractTransducer;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class AutomatonTransducer
extends AbstractTransducer {
    private Map automatonMap = new HashMap();
    private Automaton originalAutomaton = null;
    private static final String AUTOMATON_NAME = "automaton";
    private static final String COMMENT_AUTOMATA = "The list of automata";
    private static final String FILE_NAME = "tag";
    public static final String BLOCK_NAME = "block";
    public static final String STATE_NAME = "state";
    public static final String STATE_ID_NAME = "id";
    public static final String STATE_X_COORD_NAME = "x";
    public static final String STATE_Y_COORD_NAME = "y";
    public static final String STATE_LABEL_NAME = "label";
    public static final String STATE_NAME_NAME = "name";
    public static final String STATE_FINAL_NAME = "final";
    public static final String STATE_INITIAL_NAME = "initial";
    public static final String TRANSITION_NAME = "transition";
    public static final String TRANSITION_FROM_NAME = "from";
    public static final String TRANSITION_TO_NAME = "to";
    public static final String TRANSITION_CONTROL_X = "controlx";
    public static final String TRANSITION_CONTROL_Y = "controly";
    private static final String COMMENT_STATES = "The list of states.";
    private static final String COMMENT_TRANSITIONS = "The list of transitions.";
    public static final String NOTE_NAME = "note";
    public static final String NOTE_TEXT_NAME = "text";
    private static final String IS_BLOCK = "block";

    protected abstract Automaton createEmptyAutomaton(Document var1);

    protected Map readStates(Node node, Automaton automaton, Set set, Document document) {
        HashMap hashMap = new HashMap();
        if (node == null) {
            return hashMap;
        }
        NodeList nodeList = node.getChildNodes();
        ArrayList<Node> arrayList = new ArrayList<Node>();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            if (!nodeList.item(i).getNodeName().equals(STATE_NAME)) continue;
            arrayList.add(nodeList.item(i));
        }
        TreeMap treeMap = new TreeMap(new Comparator(){

            public int compare(Object object, Object object2) {
                if (object instanceof Integer && !(object2 instanceof Integer)) {
                    return -1;
                }
                if (object instanceof Integer) {
                    return (Integer)object - (Integer)object2;
                }
                if (object2 instanceof Integer) {
                    return 1;
                }
                return ((Comparable)object).compareTo(object2);
            }
        });
        this.createState(arrayList, treeMap, automaton, set, hashMap, false, document);
        return hashMap;
    }

    protected void createState(ArrayList arrayList, Map map, Automaton automaton, Set set, Map map2, boolean bl, Document document) {
        for (int i = 0; i < arrayList.size(); ++i) {
            Node node = (Node)arrayList.get(i);
            if (node.getNodeType() != 1) continue;
            String idText = ((Element)node).getAttribute(STATE_ID_NAME);
            if (idText == null) {
                throw new DataException("State without id attribute encountered!");
            }
            Object id = AutomatonTransducer.parseID(idText);
            if (map.put(id, node) == null) continue;
            throw new DataException("The state ID " + id + " appears twice!");
        }
        for (Object key : map.keySet()) {
            Element element = (Element)map.get(key);
            Map props = AutomatonTransducer.elementsToText((Node)element);
            Point point = new Point();
            boolean bl2 = true;
            double d = 0.0;
            double d2 = 0.0;
            try {
                d = Double.parseDouble(props.get(STATE_X_COORD_NAME).toString());
            }
            catch (NullPointerException nullPointerException) {
                bl2 = false;
            }
            catch (NumberFormatException numberFormatException) {
                throw new DataException("The x coordinate " + props.get(STATE_X_COORD_NAME) + " could not be read for state " + key + ".");
            }
            try {
                d2 = Double.parseDouble(props.get(STATE_Y_COORD_NAME).toString());
            }
            catch (NullPointerException nullPointerException) {
                bl2 = false;
            }
            catch (NumberFormatException numberFormatException) {
                throw new DataException("The y coordinate " + props.get(STATE_Y_COORD_NAME) + " could not be read for state " + key + ".");
            }
            point.setLocation(d, d2);
            State state = null;
            if (!(automaton instanceof TuringMachine)) {
                state = automaton.createStateWithId(point, (Integer)key);
            } else if (props.containsKey(FILE_NAME)) {
                String string = props.get(FILE_NAME).toString();
                Node inner = document.getDocumentElement().getElementsByTagName(string).item(0);
                TuringMachine turingMachine = (TuringMachine)this.readAutomaton(inner, document);
                state = ((TuringMachine)automaton).createInnerTM(point, turingMachine, string, (Integer)key);
            } else {
                state = ((TuringMachine)automaton).createTMStateWithID(point, (Integer)key);
            }
            if (bl2 && set != null) {
                set.add(state);
            }
            map2.put(key, state);
            String nameAttr = element.getAttribute(STATE_NAME_NAME);
            if (nameAttr.equals("")) {
                state.setName("q" + (Integer)key);
            } else {
                state.setName(nameAttr);
            }
            if (props.containsKey(STATE_NAME_NAME)) {
                state.setName((String)props.get(STATE_NAME_NAME));
            }
            if (props.containsKey(STATE_LABEL_NAME)) {
                state.setLabel((String)props.get(STATE_LABEL_NAME));
            }
            if (props.containsKey("color")) {
                try {
                    state.setFillColor(java.awt.Color.decode(props.get("color").toString().trim()));
                }
                catch (RuntimeException runtimeException) {
                    // color invalido: se ignora
                }
            }
            if (props.containsKey(STATE_FINAL_NAME)) {
                automaton.addFinalState(state);
            }
            if (props.containsKey(STATE_INITIAL_NAME)) {
                automaton.setInitialState(state);
            }
            if (!(automaton instanceof MooreMachine) || !props.containsKey("output")) continue;
            ((MooreMachine)automaton).setOutput(state, (String)props.get("output"));
        }
    }

    protected void addBlocks(Node node, Automaton automaton, Set set, Map map, Document document) {
        assert (automaton instanceof TuringMachine);
        if (node == null) {
            return;
        }
        if (!node.hasChildNodes()) {
            return;
        }
        NodeList nodeList = node.getChildNodes();
        ArrayList<Node> arrayList = new ArrayList<Node>();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            if (!nodeList.item(i).getNodeName().equals("block")) continue;
            arrayList.add(nodeList.item(i));
        }
        TreeMap treeMap = new TreeMap(new Comparator(){

            public int compare(Object object, Object object2) {
                if (object instanceof Integer && !(object2 instanceof Integer)) {
                    return -1;
                }
                if (object instanceof Integer) {
                    return (Integer)object - (Integer)object2;
                }
                if (object2 instanceof Integer) {
                    return 1;
                }
                return ((Comparable)object).compareTo(object2);
            }
        });
        this.createState(arrayList, treeMap, automaton, set, map, true, document);
    }

    protected abstract Transition createTransition(State var1, State var2, Node var3, Map var4, boolean var5);

    protected void readTransitions(Node node, Automaton automaton, Map map) {
        int n;
        HashMap hashMap = new HashMap();
        if (node == null || automaton == null) {
            return;
        }
        NodeList nodeList = node.getChildNodes();
        ArrayList<Node> arrayList = new ArrayList<Node>();
        boolean bl = false;
        for (n = 0; n < nodeList.getLength(); ++n) {
            if (!nodeList.item(n).getNodeName().equals(TRANSITION_NAME)) continue;
            arrayList.add(nodeList.item(n));
        }
        for (n = 0; n < arrayList.size(); ++n) {
            String string;
            Node node2 = (Node)arrayList.get(n);
            Map map2 = AutomatonTransducer.elementsToText(node2);
            String string2 = ((Element)node2).getAttribute("block");
            if (string2.equals("true")) {
                bl = true;
            }
            if ((string = (String)map2.get(TRANSITION_FROM_NAME)) == null) {
                throw new DataException("A transition has no from state!");
            }
            int n2 = AutomatonTransducer.parseID(string);
            State state = automaton.getStateWithID(n2);
            if (state == null) {
                throw new DataException("A transition is defined from non-existent state " + n2 + "!");
            }
            String string3 = (String)map2.get(TRANSITION_TO_NAME);
            if (string3 == null) {
                throw new DataException("A transition has no to state!");
            }
            n2 = AutomatonTransducer.parseID(string3);
            State state2 = automaton.getStateWithID(n2);
            if (state2 == null) {
                throw new DataException("A transition is defined to non-existent state " + n2 + "!");
            }
            Transition transition = this.createTransition(state, state2, node2, map2, bl);
            automaton.addTransition(transition);
            bl = false;
            String string4 = (String)map2.get(TRANSITION_CONTROL_X);
            String string5 = (String)map2.get(TRANSITION_CONTROL_Y);
            if (string4 != null && string5 != null) {
                Point point = new Point(Integer.parseInt(string4), Integer.parseInt(string5));
                transition.setControl(point);
                continue;
            }
            transition.setControl(null);
        }
    }

    protected static Integer parseID(String string) {
        try {
            int n = Integer.parseInt(string);
            return new Integer(n);
        }
        catch (NumberFormatException numberFormatException) {
            return new Integer(-1);
        }
    }

    private void performLayout(Automaton automaton, Set set) {
        if (set.size() == automaton.getStates().length) {
            return;
        }
        AutomatonGraph automatonGraph = new AutomatonGraph(automaton);
        GEMLayoutAlgorithm gEMLayoutAlgorithm = new GEMLayoutAlgorithm();
        for (int i = 0; i < 3; ++i) {
            ((LayoutAlgorithm)gEMLayoutAlgorithm).layout(automatonGraph, set);
        }
        if (set.size() < 2) {
            automatonGraph.moveWithinFrame(new Rectangle(20, 20, 425, 260));
        }
        automatonGraph.moveAutomatonStates();
    }

    @Override
    public Serializable fromDOM(Document document) {
        this.automatonMap.clear();
        Automaton automaton = this.createEmptyAutomaton(document);
        Node node = document.getDocumentElement().getElementsByTagName(AUTOMATON_NAME).item(0);
        if (node == null) {
            node = document.getDocumentElement();
        }
        return this.readAutomaton(node, document);
    }

    public Serializable readAutomaton(Node node, Document document) {
        HashSet hashSet = new HashSet();
        Automaton automaton = this.createEmptyAutomaton(document);
        if (node == null) {
            return automaton;
        }
        this.readBlocks(node, automaton, hashSet, document);
        this.readTransitions(node, automaton, this.readStates(node, automaton, hashSet, document));
        this.readnotes(node, automaton, document);
        this.performLayout(automaton, hashSet);
        this.automatonMap.put(node.getNodeName(), automaton);
        return automaton;
    }

    private void readnotes(Node node, Automaton automaton, Document document) {
        int n;
        NodeList nodeList = node.getChildNodes();
        ArrayList<Node> arrayList = new ArrayList<Node>();
        for (n = 0; n < nodeList.getLength(); ++n) {
            if (!nodeList.item(n).getNodeName().equals(NOTE_NAME)) continue;
            arrayList.add(nodeList.item(n));
        }
        for (n = 0; n < arrayList.size(); ++n) {
            Node node2 = (Node)arrayList.get(n);
            if (node2.getNodeType() != 1) continue;
            Map map = AutomatonTransducer.elementsToText(node2);
            Point point = new Point();
            boolean bl = true;
            Object v = map.get(NOTE_TEXT_NAME);
            if (v == null) continue;
            String string = v.toString();
            double d = 0.0;
            double d2 = 0.0;
            try {
                d = Double.parseDouble(map.get(STATE_X_COORD_NAME).toString());
            }
            catch (NullPointerException nullPointerException) {
                bl = false;
            }
            catch (NumberFormatException numberFormatException) {
                throw new DataException("The x coordinate " + map.get(STATE_X_COORD_NAME) + " could not be read for the note with text " + string + ".");
            }
            try {
                d2 = Double.parseDouble(map.get(STATE_Y_COORD_NAME).toString());
            }
            catch (NullPointerException nullPointerException) {
                bl = false;
            }
            catch (NumberFormatException numberFormatException) {
                throw new DataException("The y coordinate " + map.get(STATE_Y_COORD_NAME) + " could not be read for the note with text " + string + ".");
            }
            point.setLocation(d, d2);
            Note note = new Note(point, string);
            note.applyStyleStrings(map.get("fgcolor"), map.get("bgcolor"), map.get("font"), map.get("boxw"), map.get("boxh"));
            automaton.addNote(note);
        }
    }

    private void readBlocks(Node node, Automaton automaton, Set set, Document document) {
        HashMap hashMap = new HashMap();
        this.addBlocks(node, automaton, set, hashMap, document);
    }

    protected Element createStateElement(Document document, State state, Automaton automaton) {
        Automaton automaton2;
        Element element = AutomatonTransducer.createElement(document, STATE_NAME, null, null);
        element.setAttribute(STATE_ID_NAME, "" + state.getID());
        element.appendChild(AutomatonTransducer.createElement(document, STATE_X_COORD_NAME, null, "" + state.getPoint().getX()));
        element.appendChild(AutomatonTransducer.createElement(document, STATE_Y_COORD_NAME, null, "" + state.getPoint().getY()));
        if (state.getLabel() != null) {
            element.appendChild(AutomatonTransducer.createElement(document, STATE_LABEL_NAME, null, state.getLabel()));
        }
        if (state.getName() != null) {
            element.setAttribute(STATE_NAME_NAME, "" + state.getName());
        }
        if (state.getFillColor() != null) {
            element.appendChild(AutomatonTransducer.createElement(document, "color", null, String.format("#%06X", state.getFillColor().getRGB() & 0xFFFFFF)));
        }
        if ((automaton2 = state.getAutomaton()).getInitialState() == state) {
            element.appendChild(AutomatonTransducer.createElement(document, STATE_INITIAL_NAME, null, null));
        }
        if (automaton2.isFinalState(state)) {
            element.appendChild(AutomatonTransducer.createElement(document, STATE_FINAL_NAME, null, null));
        }
        return element;
    }

    protected Element createTransitionElement(Document document, Transition transition) {
        Element element = AutomatonTransducer.createElement(document, TRANSITION_NAME, null, null);
        element.appendChild(AutomatonTransducer.createElement(document, TRANSITION_FROM_NAME, null, "" + transition.getFromState().getID()));
        element.appendChild(AutomatonTransducer.createElement(document, TRANSITION_TO_NAME, null, "" + transition.getToState().getID()));
        if (transition.getControl() != null) {
            Point point = transition.getControl();
            element.appendChild(AutomatonTransducer.createElement(document, TRANSITION_CONTROL_X, null, point.x + ""));
            element.appendChild(AutomatonTransducer.createElement(document, TRANSITION_CONTROL_Y, null, point.y + ""));
        }
        return element;
    }

    protected Element createBlockElement(Document document, TMState tMState, Automaton automaton) {
        Element element = AutomatonTransducer.createElement(document, "block", null, null);
        element.setAttribute(STATE_ID_NAME, "" + tMState.getID());
        if (tMState.getName() != null) {
            element.setAttribute(STATE_NAME_NAME, "" + tMState.getName());
        }
        element.appendChild(AutomatonTransducer.createElement(document, FILE_NAME, null, "" + tMState.getInternalName()));
        element.appendChild(AutomatonTransducer.createElement(document, STATE_X_COORD_NAME, null, "" + tMState.getPoint().getX()));
        element.appendChild(AutomatonTransducer.createElement(document, STATE_Y_COORD_NAME, null, "" + tMState.getPoint().getY()));
        if (tMState.getFillColor() != null) {
            element.appendChild(AutomatonTransducer.createElement(document, "color", null, String.format("#%06X", tMState.getFillColor().getRGB() & 0xFFFFFF)));
        }
        Automaton automaton2 = tMState.getAutomaton();
        if (automaton2.getInitialState() == tMState) {
            element.appendChild(AutomatonTransducer.createElement(document, STATE_INITIAL_NAME, null, null));
        }
        if (automaton2.isFinalState(tMState)) {
            element.appendChild(AutomatonTransducer.createElement(document, STATE_FINAL_NAME, null, null));
        }
        return element;
    }

    protected Element createAutomatonElement(Document document, Automaton automaton, String string) {
        Element element = document.getDocumentElement();
        Element element2 = AutomatonTransducer.createElement(document, string, null, null);
        element.appendChild(element2);
        this.writeFields(document, automaton, element2);
        return element2;
    }

    @Override
    public Document toDOM(Serializable serializable) {
        Automaton automaton;
        this.originalAutomaton = automaton = (Automaton)serializable;
        Document document = this.newEmptyDocument();
        Element element = document.getDocumentElement();
        element.appendChild(this.createAutomatonElement(document, automaton, AUTOMATON_NAME));
        return document;
    }

    private Element writeFields(Document document, Automaton automaton, Element element) {
        Transition[] transitionArray;
        int n;
        State[] stateArray = automaton.getStates();
        if (stateArray.length > 0) {
            element.appendChild(AutomatonTransducer.createComment(document, COMMENT_STATES));
        }
        if (automaton instanceof TuringMachine) {
            for (n = 0; n < stateArray.length; ++n) {
                element.appendChild(this.createBlockElement(document, (TMState)stateArray[n], automaton));
            }
        } else {
            for (n = 0; n < stateArray.length; ++n) {
                element.appendChild(this.createStateElement(document, stateArray[n], automaton));
            }
        }
        if ((transitionArray = automaton.getTransitions()).length > 0) {
            element.appendChild(AutomatonTransducer.createComment(document, COMMENT_TRANSITIONS));
        }
        for (int i = 0; i < transitionArray.length; ++i) {
            element.appendChild(this.createTransitionElement(document, transitionArray[i]));
        }
        if (automaton instanceof TuringMachine) {
            Map<String, TuringMachine> map = ((TuringMachine)automaton).getBlockMap();
            Iterator<String> iterator = map.keySet().iterator();
            if (iterator.hasNext()) {
                element.appendChild(AutomatonTransducer.createComment(document, COMMENT_AUTOMATA));
            }
            while (iterator.hasNext()) {
                String string = iterator.next();
                if (this.automatonMap.containsKey(map.get(string))) continue;
                element.appendChild(this.createAutomatonElement(document, map.get(string), string));
                this.automatonMap.put(string, automaton);
            }
        }
        ArrayList arrayList = automaton.getNotes();
        for (int i = 0; i < arrayList.size(); ++i) {
            element.appendChild(this.createNoteElement(document, (Note)arrayList.get(i)));
        }
        return element;
    }

    private Node createNoteElement(Document document, Note note) {
        Element element = AutomatonTransducer.createElement(document, NOTE_NAME, null, null);
        element.appendChild(AutomatonTransducer.createElement(document, NOTE_TEXT_NAME, null, "" + note.getText()));
        element.appendChild(AutomatonTransducer.createElement(document, STATE_X_COORD_NAME, null, "" + note.getLocation().getX()));
        element.appendChild(AutomatonTransducer.createElement(document, STATE_Y_COORD_NAME, null, "" + note.getLocation().getY()));
        if (note.isStyled()) {
            element.appendChild(AutomatonTransducer.createElement(document, "fgcolor", null, String.format("#%06X", note.getForeground().getRGB() & 0xFFFFFF)));
            element.appendChild(AutomatonTransducer.createElement(document, "bgcolor", null, String.format("#%06X", note.getBackground().getRGB() & 0xFFFFFF)));
            java.awt.Font f = note.getBaseFont();
            if (f != null) {
                element.appendChild(AutomatonTransducer.createElement(document, "font", null, f.getFamily() + "|" + f.getStyle() + "|" + f.getSize()));
            }
            if (note.getCustomSize() != null) {
                element.appendChild(AutomatonTransducer.createElement(document, "boxw", null, "" + note.getCustomSize().width));
                element.appendChild(AutomatonTransducer.createElement(document, "boxh", null, "" + note.getCustomSize().height));
            }
        }
        return element;
    }
}

