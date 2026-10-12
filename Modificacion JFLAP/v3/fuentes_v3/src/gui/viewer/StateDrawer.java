/*
 * Decompiled with CFR 0.152.
 */
package gui.viewer;

import automata.Automaton;
import automata.State;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import plus.Theme;

public class StateDrawer {
    private int radius = 20;
    public static final int STATE_RADIUS = 20;
    public static final Color STATE_COLOR = new Color(255, 255, 150);
    public static final int STATE_LABEL_PAD = 3;

    public StateDrawer() {
        this.radius = 20;
    }

    public StateDrawer(int n) {
        this.radius = n;
    }

    public void drawState(Graphics graphics, Automaton automaton, State state) {
        this.drawState(graphics, automaton, state, state.getPoint());
    }

    public void drawState(Graphics graphics, Automaton automaton, State state, Point point) {
        this.drawState(graphics, automaton, state, point, state.getFillColor() != null ? state.getFillColor() : Theme.STATE_FILL);
    }

    public void drawState(Graphics graphics, Automaton automaton, State state, Point point, Color color) {
        this.drawArea(graphics, automaton, state, point, color);
    }

    private void drawArea(Graphics graphics, Automaton automaton, State state, Point point, Color color) {
        this.drawBackground(graphics, state, point, color);
        if (state.getFillColor() != null && !state.isSelected()) {
            double lum = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
            graphics.setColor(lum > 140.0 ? Color.black : Color.white);
        } else {
            graphics.setColor(Theme.BLACK);
        }
        int n = (int)graphics.getFontMetrics().getStringBounds(state.getName(), graphics).getWidth() >> 1;
        int n2 = graphics.getFontMetrics().getAscent() >> 1;
        graphics.drawString(state.getName(), point.x - n, point.y + n2);
        graphics.setColor(Theme.BLACK);
        graphics.drawOval(point.x - this.radius, point.y - this.radius, 2 * this.radius, 2 * this.radius);
        if (automaton.isFinalState(state)) {
            graphics.drawOval(point.x - this.radius + 3, point.y - this.radius + 3, this.radius - 3 << 1, this.radius - 3 << 1);
        }
        if (automaton.getInitialState() == state) {
            int[] nArray = new int[]{point.x - this.radius, point.x - (this.radius << 1), point.x - (this.radius << 1)};
            int[] nArray2 = new int[]{point.y, point.y - this.radius, point.y + this.radius};
            graphics.setColor(Theme.WHITE);
            graphics.fillPolygon(nArray, nArray2, 3);
            graphics.setColor(Theme.BLACK);
            graphics.drawPolygon(nArray, nArray2, 3);
        }
    }

    public void drawStateLabel(Graphics graphics, State state, Point point, Color color) {
        int n;
        int n2;
        String[] stringArray = state.getLabels();
        if (stringArray.length == 0) {
            return;
        }
        int n3 = graphics.getFontMetrics().getAscent();
        int n4 = 0;
        int n5 = 0;
        for (n2 = 0; n2 < stringArray.length; ++n2) {
            Rectangle2D rectangle2D = graphics.getFontMetrics().getStringBounds(stringArray[n2], graphics);
            n5 = Math.max((int)rectangle2D.getWidth(), n5);
            n4 += n3 + 3;
        }
        n2 = n5 + 6;
        int n6 = (n4 -= 3) + 6;
        int n7 = point.x - (n2 >> 1);
        int n8 = n = point.y + 20 - 3;
        graphics.setColor(color);
        graphics.fillRect(n7, n, n2, n6);
        graphics.setColor(Theme.BLACK);
        for (int i = 0; i < stringArray.length; ++i) {
            graphics.drawString(stringArray[i], n7 + 3, n8 += n3 + 3);
        }
        graphics.drawRect(n7, n, n2, n6);
    }

    public void drawBackground(Graphics graphics, State state, Point point, Color color) {
        graphics.setColor(color);
        if (state.isSelected()) {
            graphics.setColor(new Color(100, 200, 200));
        }
        graphics.fillOval(point.x - this.radius, point.y - this.radius, 2 * this.radius, 2 * this.radius);
    }

    public int getRadius() {
        return this.radius;
    }
}

