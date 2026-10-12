/*
 * Decompiled with CFR 0.152.
 */
package gui.sim;

import gui.TooltipAction;
import gui.sim.ConfigurationController;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JToolBar;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

public class ControlPanel
extends JToolBar {
    private boolean blockStep = false;
    private ConfigurationController controller;
    private Timer autoTimer;
    private JToggleButton autoButton;
    private JSpinner delaySpinner;
    private JLabel autoResult;

    public ControlPanel(ConfigurationController configurationController) {
        this.controller = configurationController;
        this.initView();
    }

    public ConfigurationController getController() {
        return this.controller;
    }

    protected void initView() {
        this.add(new TooltipAction("Step", "Moves existing valid configurations to the next configurations."){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ControlPanel.this.controller.step(ControlPanel.this.blockStep);
            }
        });
        this.add(new TooltipAction("Reset", "Resets the simulation to start conditions."){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ControlPanel.this.stopAuto(null);
                ControlPanel.this.controller.reset();
            }
        });
        if (this.controller.isTuringMachine()) {
            this.add(new AbstractAction("Focus"){

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    ControlPanel.this.controller.focus();
                }
            });
            this.add(new AbstractAction("Defocus"){

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    ControlPanel.this.controller.defocus();
                }
            });
        }
        this.add(new AbstractAction("Freeze"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ControlPanel.this.controller.freeze();
            }
        });
        this.add(new AbstractAction("Thaw"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ControlPanel.this.controller.thaw();
            }
        });
        this.add(new AbstractAction("Trace"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ControlPanel.this.controller.trace();
            }
        });
        this.add(new AbstractAction("Remove"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ControlPanel.this.controller.remove();
            }
        });
        this.initAuto();
    }

    private void initAuto() {
        this.addSeparator();
        this.autoButton = new JToggleButton("Auto \u25B6");
        this.autoButton.setToolTipText("Avanza automaticamente paso a paso hasta aceptar o rechazar la cadena.");
        this.delaySpinner = new JSpinner(new SpinnerNumberModel(500, 50, 10000, 50));
        this.delaySpinner.setToolTipText("Milisegundos entre pasos");
        this.delaySpinner.setMaximumSize(this.delaySpinner.getPreferredSize());
        this.autoResult = new JLabel(" ");
        this.autoTimer = new Timer(500, new java.awt.event.ActionListener(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ControlPanel.this.autoTick();
            }
        });
        this.autoButton.addActionListener(new java.awt.event.ActionListener(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (ControlPanel.this.autoButton.isSelected()) {
                    ControlPanel.this.startAuto();
                } else {
                    ControlPanel.this.stopAuto(null);
                }
            }
        });
        this.delaySpinner.addChangeListener(new javax.swing.event.ChangeListener(){

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent changeEvent) {
                ControlPanel.this.autoTimer.setDelay(((Number)ControlPanel.this.delaySpinner.getValue()).intValue());
            }
        });
        this.add(this.autoButton);
        this.add(this.delaySpinner);
        this.add(new JLabel(" ms "));
        this.add(this.autoResult);
    }

    private void startAuto() {
        if (this.controller.autoStatus() != 0) {
            this.stopAuto(this.controller.autoStatus() == 1 ? "Accepted" : "Rejected");
            return;
        }
        this.autoResult.setText(" ");
        this.autoTimer.setDelay(((Number)this.delaySpinner.getValue()).intValue());
        this.autoTimer.start();
        this.autoButton.setText("Pause \u23F8");
    }

    private void stopAuto(String string) {
        this.autoTimer.stop();
        this.autoButton.setSelected(false);
        this.autoButton.setText("Auto \u25B6");
        if (string != null) {
            this.autoResult.setText(" " + string);
        }
    }

    private void autoTick() {
        this.controller.step(this.blockStep);
        int n = this.controller.autoStatus();
        if (n == 1) {
            this.stopAuto("Accepted");
        } else if (n == 2) {
            this.stopAuto("Rejected");
        }
    }

    @Override
    public void removeNotify() {
        if (this.autoTimer != null) {
            this.autoTimer.stop();
        }
        super.removeNotify();
    }

    public void setBlock(boolean bl) {
        this.blockStep = bl;
    }
}

