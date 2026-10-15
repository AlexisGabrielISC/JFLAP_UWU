/*
 * Decompiled with CFR 0.152.
 */
package gui.editor;

import automata.Note;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;
import java.awt.Font;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class AutomatonSizeSlider
extends JSlider {
    static final int AUTOMATON_SIZE_MIN = 1;
    static final int AUTOMATON_SIZE_MAX = 800;
    static final int AUTOMATON_SIZE_INIT = 220;
    static final String AUTOMATON_SIZE_TITLE = "Automaton Size";
    private AutomatonPane view;
    private AutomatonDrawer drawer;
    private double currentScale = 1.0;

    public AutomatonSizeSlider(AutomatonPane automatonPane, AutomatonDrawer automatonDrawer) {
        super(1, 800, 220);
        this.view = automatonPane;
        this.drawer = automatonDrawer;
        this.addChangeListener(new SliderListener());
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), AUTOMATON_SIZE_TITLE));
    }

    class SliderListener
    implements ChangeListener {
        SliderListener() {
        }

        @Override
        public void stateChanged(ChangeEvent changeEvent) {
            JSlider jSlider = (JSlider)changeEvent.getSource();
            double d = (double)jSlider.getValue() * 1.0 / 220.0;
            AutomatonSizeSlider.this.view.setScale(d);
            AutomatonSizeSlider.this.view.requestTransform();
            java.util.List<Note> arrayList = (java.util.List<Note>)AutomatonSizeSlider.this.drawer.getAutomaton().getNotes();
            for (Note note : arrayList) {
                note.applyZoom(jSlider.getValue() / 20);
            }
        }
    }
}

