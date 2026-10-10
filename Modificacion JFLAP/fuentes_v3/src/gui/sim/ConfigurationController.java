/*
 * Decompiled with CFR 0.152.
 */
package gui.sim;

import automata.AutomatonSimulator;
import automata.Configuration;
import automata.turing.TMConfiguration;
import automata.turing.TMSimulator;
import automata.turing.TMState;
import automata.turing.TuringMachine;
import gui.sim.ConfigurationPane;
import gui.sim.ConfigurationSelectionEvent;
import gui.sim.ConfigurationSelectionListener;
import gui.sim.TraceWindow;
import gui.viewer.SelectionDrawer;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

public class ConfigurationController
implements ConfigurationSelectionListener {
    private ConfigurationPane configurations;
    private AutomatonSimulator simulator;
    private SelectionDrawer drawer;
    private Component component;
    private HashMap configurationToTraceWindow = new HashMap();
    private Configuration[] originalConfigurations = new Configuration[0];
    private static final String NO_CONFIGURATION_ERROR = "Select at least one configuration!";
    private static final String NO_CONFIGURATION_ERROR_TITLE = "No Configuration Selected";
    private static final String FOCUS_CONFIGURATION_ERROR = "JFLAP can only focus on one configuration at a time!";
    private static final String FOCUS_CONFIGURATION_ERROR_TITLE = "Too many configurations selected";

    public ConfigurationController(ConfigurationPane configurationPane, AutomatonSimulator automatonSimulator, SelectionDrawer selectionDrawer, Component component) {
        this.configurations = configurationPane;
        this.simulator = automatonSimulator;
        this.drawer = selectionDrawer;
        this.component = component;
        this.changeSelection();
        this.configurations.addSelectionListener(this);
        this.originalConfigurations = this.configurations.getConfigurations();
    }

    public void reset() {
        this.configurations.clear();
        if (this.simulator instanceof TMSimulator) {
            TMSimulator tMSimulator = (TMSimulator)this.simulator;
            Configuration[] configurationArray = tMSimulator.getInitialConfigurations(tMSimulator.getInputStrings());
            for (int i = 0; i < configurationArray.length; ++i) {
                this.configurations.add(configurationArray[i]);
            }
        } else {
            for (int i = 0; i < this.originalConfigurations.length; ++i) {
                this.originalConfigurations[i].reset();
                this.configurations.add(this.originalConfigurations[i]);
            }
        }
        this.configurations.validate();
        this.configurations.repaint();
        this.changeSelection();
    }

    public void cleanup() {
        Collection collection = this.configurationToTraceWindow.values();
        Iterator iterator = collection.iterator();
        while (iterator.hasNext()) {
            ((TraceWindow)iterator.next()).dispose();
        }
        this.configurationToTraceWindow.clear();
    }

    public void step(boolean bl) {
        Configuration[] configurationArray = this.configurations.getValidConfigurations();
        ArrayList<Configuration> arrayList = new ArrayList<Configuration>();
        HashSet<Configuration> hashSet = new HashSet<Configuration>();
        this.configurations.clearThawed();
        if (!bl) {
            for (int i = 0; i < configurationArray.length; ++i) {
                ArrayList object2 = this.simulator.stepConfiguration(configurationArray[i]);
                if (object2.size() == 0) {
                    hashSet.add(configurationArray[i]);
                    arrayList.add(configurationArray[i]);
                    continue;
                }
                arrayList.addAll(object2);
            }
        } else {
            assert (configurationArray.length == 1);
            assert (configurationArray[0] instanceof TMConfiguration);
            assert (this.simulator instanceof TMSimulator);
            if (configurationArray.length != 0) {
                List list = ((TMSimulator)this.simulator).stepBlock((TMConfiguration)configurationArray[0]);
                if (list.size() == 0) {
                    hashSet.add(configurationArray[0]);
                    arrayList.add(configurationArray[0]);
                } else {
                    arrayList.addAll(list);
                }
            }
        }
        for (Configuration configuration : arrayList) {
            this.configurations.add(configuration);
            if (!hashSet.contains(configuration)) continue;
            this.configurations.setReject(configuration);
        }
        this.configurations.validate();
        this.configurations.repaint();
        this.changeSelection();
        try {
            JSplitPane jSplitPane = (JSplitPane)this.configurations.getParent().getParent().getParent().getParent();
            int n = jSplitPane.getDividerLocation();
            jSplitPane.setDividerLocation(n - 1);
            jSplitPane.setDividerLocation(n);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void freeze() {
        Configuration[] configurationArray = this.configurations.getSelected();
        if (configurationArray.length == 0) {
            JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR, NO_CONFIGURATION_ERROR_TITLE, 0);
            return;
        }
        for (int i = 0; i < configurationArray.length; ++i) {
            this.configurations.setFrozen(configurationArray[i]);
        }
        this.configurations.deselectAll();
        this.configurations.repaint();
    }

    public void remove() {
        Configuration[] configurationArray = this.configurations.getSelected();
        if (configurationArray.length == 0) {
            JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR, NO_CONFIGURATION_ERROR_TITLE, 0);
            return;
        }
        for (int i = 0; i < configurationArray.length; ++i) {
            this.configurations.remove(configurationArray[i]);
        }
        this.configurations.validate();
        this.configurations.repaint();
    }

    public void focus() {
        Configuration[] configurationArray = this.configurations.getSelected();
        if (configurationArray.length == 0) {
            JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR, NO_CONFIGURATION_ERROR_TITLE, 0);
            return;
        }
        if (configurationArray.length > 1) {
            JOptionPane.showMessageDialog(this.configurations, FOCUS_CONFIGURATION_ERROR, FOCUS_CONFIGURATION_ERROR_TITLE, 0);
            return;
        }
        Configuration configuration = configurationArray[0];
        this.configurations.setFocused(configuration);
        configuration.setFocused(true);
        this.component.repaint();
    }

    public void changeSelection() {
        this.drawer.clearSelected();
        Configuration[] configurationArray = this.configurations.getConfigurations();
        boolean bl = false;
        for (int i = 0; i < configurationArray.length; ++i) {
            Configuration configuration = configurationArray[i];
            bl = this.setFocusIfNeeded(configuration, bl);
            if (configuration instanceof TMConfiguration) {
                TMState tMState = (TMState)configuration.getCurrentState();
                while (((TuringMachine)tMState.getAutomaton()).getParent() != null) {
                    tMState = ((TuringMachine)tMState.getAutomaton()).getParent();
                }
                this.drawer.addSelected(tMState);
            }
            this.drawer.addSelected(configurationArray[i].getCurrentState());
        }
        this.component.repaint();
    }

    private boolean setFocusIfNeeded(Configuration configuration, boolean bl) {
        Configuration configuration2 = configuration.getParent();
        if (configuration2 == null) {
            return bl;
        }
        if (configuration2.getFocused()) {
            configuration.setFocused(true);
            if (!bl) {
                this.configurations.setFocused(configuration);
                configuration.setFocused(true);
                bl = true;
            }
        }
        return bl;
    }

    public void defocus() {
        Configuration[] configurationArray = this.configurations.getConfigurations();
        for (int i = 0; i < configurationArray.length; ++i) {
            if (!configurationArray[i].getFocused()) continue;
            this.configurations.defocus(configurationArray[i]);
        }
        this.drawer.setAutomaton(this.simulator.getAutomaton());
        this.drawer.invalidate();
        this.component.repaint();
    }

    public void thaw() {
        Configuration[] configurationArray = this.configurations.getSelected();
        if (configurationArray.length == 0) {
            JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR, NO_CONFIGURATION_ERROR_TITLE, 0);
            return;
        }
        for (int i = 0; i < configurationArray.length; ++i) {
            this.configurations.setNormal(configurationArray[i]);
        }
        this.configurations.deselectAll();
        this.configurations.repaint();
    }

    public void trace() {
        Configuration[] configurationArray = this.configurations.getSelected();
        if (configurationArray.length == 0) {
            JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR, NO_CONFIGURATION_ERROR_TITLE, 0);
            return;
        }
        for (int i = 0; i < configurationArray.length; ++i) {
            TraceWindow traceWindow = (TraceWindow)this.configurationToTraceWindow.get(configurationArray[i]);
            if (traceWindow == null) {
                this.configurationToTraceWindow.put(configurationArray[i], new TraceWindow(configurationArray[i]));
                continue;
            }
            traceWindow.setVisible(true);
            traceWindow.toFront();
        }
    }

    /** 0 = sigue en curso, 1 = alguna configuracion acepto, 2 = ya no quedan configuraciones validas (rechazo). */
    public int autoStatus() {
        Configuration[] all = this.configurations.getConfigurations();
        for (int i = 0; i < all.length; ++i) {
            if (this.configurations.getState(all[i]) != 1) continue;
            return 1;
        }
        return this.configurations.getValidConfigurations().length == 0 ? 2 : 0;
    }

    public boolean isTuringMachine() {
        return this.simulator instanceof TMSimulator;
    }

    @Override
    public void configurationSelectionChange(ConfigurationSelectionEvent configurationSelectionEvent) {
    }
}

