/*
 * Decompiled with CFR 0.152.
 */
package automata;

import automata.Automaton;
import automata.Note;
import automata.event.AutomataStateEvent;
import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class State
implements Serializable {
    private Note myNote;
    String internalName = null;
    private Point point;
    int id;
    String name = null;
    private static final char SS = '\u2080';
    private Automaton automaton = null;
    private String label;
    private String[] labels = new String[0];
    private boolean selected = false;
    private java.awt.Color fillColor = null;

    public java.awt.Color getFillColor() {
        return this.fillColor;
    }

    public void setFillColor(java.awt.Color color) {
        this.fillColor = color;
    }

    public State(int n, Point point, Automaton automaton) {
        this.point = point;
        this.id = n;
        this.automaton = automaton;
    }

    public Point getPoint() {
        return this.point;
    }

    public void setAutomaton(Automaton automaton) {
        this.automaton = automaton;
    }

    public Automaton getAutomaton() {
        return this.automaton;
    }

    public void setPoint(Point point) {
        this.point = point;
        this.getAutomaton().distributeStateEvent(new AutomataStateEvent(this.getAutomaton(), this, false, true, false));
    }

    public int getID() {
        return this.id;
    }

    protected void setID(int n) {
        if (("q" + this.id).equals(this.name)) {
            this.name = null;
        }
        this.id = n;
        this.getAutomaton().distributeStateEvent(new AutomataStateEvent(this.getAutomaton(), this, false, false, true));
    }

    public String toString() {
        return "q_" + Integer.toString(this.getID()) + " at (" + Integer.toString(this.getPoint().x) + "," + Integer.toString(this.getPoint().y) + ")" + " label: " + this.getLabel();
    }

    public void setName(String string) {
        this.name = string;
        this.getAutomaton().distributeStateEvent(new AutomataStateEvent(this.getAutomaton(), this, false, false, true));
    }

    public String getName() {
        if (this.name == null) {
            this.name = "q" + Integer.toString(this.getID());
        }
        return this.name;
    }

    private String digitizer(int n) {
        if (n == 0) {
            return "\u2080";
        }
        String string = this.digitizer(n / 10, 1);
        return string + (8320 + (char)(n % 10));
    }

    private String digitizer(int n, int n2) {
        if (n == 0) {
            return "";
        }
        String string = this.digitizer(n / 10, 1);
        return string + (8320 + (char)(n % 10));
    }

    public void setLabel(String string) {
        this.label = string;
        if (string == null) {
            this.labels = new String[0];
        } else {
            StringTokenizer stringTokenizer = new StringTokenizer(string, "\n");
            ArrayList<String> arrayList = new ArrayList<String>();
            while (stringTokenizer.hasMoreTokens()) {
                arrayList.add(stringTokenizer.nextToken());
            }
            this.labels = arrayList.toArray(new String[0]);
        }
        this.getAutomaton().distributeStateEvent(new AutomataStateEvent(this.getAutomaton(), this, false, false, true));
    }

    public String getLabel() {
        return this.label;
    }

    public String[] getLabels() {
        return this.labels;
    }

    public int specialHash() {
        return this.point.hashCode() + (this.myNote == null ? -1 : this.myNote.specialHash()) + (this.getLabel() == null ? -1 : this.getLabel().hashCode());
    }

    public void setNote(Note note) {
        this.myNote = note;
    }

    public void setSelect(boolean bl) {
        this.selected = bl;
    }

    public boolean isSelected() {
        return this.selected;
    }
}

