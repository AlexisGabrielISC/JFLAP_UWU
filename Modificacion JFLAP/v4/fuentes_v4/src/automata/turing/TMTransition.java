/*
 * Decompiled with CFR 0.152.
 */
package automata.turing;

import automata.State;
import automata.Transition;
import java.util.ArrayList;
import java.util.List;

public class TMTransition
extends Transition {
    private int tapes;
    private boolean blockTransition = false;
    private List<String> toRead;
    private List<String> toWrite;
    private List<String> direction;
    public static final String BLANK = "\u25a1";

    public TMTransition(State state, State state2, String string, String string2, String string3) {
        this(state, state2, new String[]{string}, new String[]{string2}, new String[]{string3});
    }

    public TMTransition(State state, State state2, String[] stringArray, String[] stringArray2, String[] stringArray3) {
        super(state, state2);
        if (stringArray.length != stringArray2.length || stringArray3.length != stringArray.length) {
            throw new IllegalArgumentException("Read symbols, write symbols, and directions must have equal numbers of elements!");
        }
        this.tapes = stringArray.length;
        if (this.tapes == 0) {
            throw new IllegalArgumentException("Attempted to create a transition with 0 tapes!");
        }
        this.toRead = new ArrayList();
        this.toWrite = new ArrayList();
        this.direction = new ArrayList();
        for (int i = 0; i < this.tapes; ++i) {
            this.toRead.add("");
            this.toWrite.add("");
            this.direction.add("");
            this.setRead(stringArray[i], i);
            this.setWrite(stringArray2[i], i);
            this.setDirection(stringArray3[i], i);
        }
    }

    public int getTapeLength() {
        return this.tapes;
    }

    @Override
    public Transition copy(State state, State state2) {
        String[] stringArray = new String[]{};
        return new TMTransition(state, state2, this.toRead.toArray(stringArray), this.toWrite.toArray(stringArray), this.direction.toArray(stringArray));
    }

    public String getRead(int n) {
        return (String)this.toRead.get(n);
    }

    public void setRead(int n, String string) {
        this.toRead.set(n, string);
    }

    public void setWrite(int n, String string) {
        this.toWrite.set(n, string);
    }

    protected void setRead(String string, int n) {
        if (string.length() == 0) {
            string = BLANK;
        }
        if (plus.LabelMatcher.isClassExpression(string)) {
            this.toRead.set(n, string);
            return;
        }
        if (string.equals("!")) {
            string = "!\u25a1";
        }
        if (string.length() != 1 && !string.startsWith("!") && !string.equals("~") && string.indexOf("}") == -1) {
            throw new IllegalArgumentException("Read string must have exactly one character!");
        }
        if (string.indexOf("}") != -1 && string.indexOf("!") != -1) {
            throw new IllegalArgumentException("Read string cannot cannot mix variable assignment in the NOT (!) operator.");
        }
        this.toRead.set(n, string);
    }

    public String getWrite(int n) {
        return (String)this.toWrite.get(n);
    }

    protected void setWrite(String string, int n) {
        if (string.length() == 0) {
            string = BLANK;
        }
        if (string.length() != 1) {
            throw new IllegalArgumentException("Write string must have exactly one character!");
        }
        this.toWrite.set(n, string);
    }

    public String getDirection(int n) {
        return (String)this.direction.get(n);
    }

    protected void setDirection(String string, int n) {
        if (!(string.equals("L") || string.equals("R") || string.equals("S"))) {
            throw new IllegalArgumentException("Direction must be L, R, or S!");
        }
        this.direction.set(n, string);
    }

    public int tapes() {
        return this.toRead.size();
    }

    @Override
    public String getDescription() {
        StringBuffer stringBuffer = new StringBuffer();
        int n = this.tapes();
        for (int i = 0; i < n; ++i) {
            if (i != 0) {
                stringBuffer.append(" | ");
            }
            stringBuffer.append((String)this.toRead.get(i));
            if (this.blockTransition) continue;
            stringBuffer.append(" ; ");
            stringBuffer.append((String)this.toWrite.get(i));
            stringBuffer.append(" , ");
            stringBuffer.append((String)this.direction.get(i));
        }
        return stringBuffer.toString();
    }

    @Override
    public String toString() {
        return super.toString() + ": \"" + this.getDescription() + "\"";
    }

    @Override
    public int hashCode() {
        int n = super.hashCode() ^ ((Object)this.toRead).hashCode() ^ ((Object)this.toWrite).hashCode() ^ ((Object)this.direction).hashCode();
        return n;
    }

    @Override
    public boolean equals(Object object) {
        try {
            TMTransition tMTransition = (TMTransition)object;
            return super.equals(object) && ((Object)this.toRead).equals(tMTransition.toRead) && ((Object)this.toWrite).equals(tMTransition.toWrite) && ((Object)this.direction).equals(tMTransition.direction);
        }
        catch (ClassCastException classCastException) {
            return false;
        }
    }

    public boolean isBlockTransition() {
        return this.blockTransition;
    }

    public void setBlockTransition(boolean bl) {
        this.blockTransition = bl;
    }
}

