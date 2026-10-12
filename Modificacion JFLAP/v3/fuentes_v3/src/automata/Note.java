/*
 * Decompiled with CFR 0.152.
 */
package automata;

import gui.editor.DeleteTool;
import gui.editor.EditorPane;
import gui.editor.Tool;
import gui.environment.AutomatonEnvironment;
import gui.viewer.AutomatonPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTextArea;
import plus.Theme;

public class Note
extends JTextArea {
    private Point myAutoPoint = null;
    public boolean moving = false;
    protected Point initialPointState;
    protected Point initialPointClick;
    protected AutomatonPane myView;
    public Point myViewPoint = new Point(0, 0);
    public static final Color DEFAULT_BG = new Color(255, 255, 150);
    public static final Color DEFAULT_FG = new Color(30, 30, 30);
    private boolean styled = false;
    private Font baseFont = null;
    private Dimension customSize = null;

    public boolean isStyled() {
        return this.styled;
    }

    public Font getBaseFont() {
        return this.baseFont;
    }

    public Dimension getCustomSize() {
        return this.customSize;
    }

    public void copyStyleFrom(Note note) {
        if (!note.styled) {
            return;
        }
        this.styled = true;
        this.baseFont = note.baseFont;
        this.customSize = note.customSize == null ? null : new Dimension(note.customSize);
        this.setFont(note.getFont());
        this.setForeground(note.getForeground());
        this.setBackground(note.getBackground());
        this.setLineWrap(note.getLineWrap());
        this.setWrapStyleWord(note.getWrapStyleWord());
    }

    /** Restaura el estilo guardado en el archivo (cualquier argumento puede ser null). */
    public void applyStyleStrings(Object fg, Object bg, Object font, Object w, Object h) {
        try {
            this.setForeground(DEFAULT_FG);
            this.setBackground(DEFAULT_BG);
            if (fg != null) {
                this.setForeground(Color.decode(fg.toString().trim()));
                this.styled = true;
            }
            if (bg != null) {
                this.setBackground(Color.decode(bg.toString().trim()));
                this.styled = true;
            }
            if (font != null) {
                String[] parts = font.toString().trim().split("\\|");
                this.baseFont = new Font(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                this.setFont(this.baseFont);
                this.styled = true;
            }
            if (w != null && h != null) {
                this.setBoxSize(new Dimension(Integer.parseInt(w.toString().trim()), Integer.parseInt(h.toString().trim())));
                this.styled = true;
            }
        }
        catch (RuntimeException runtimeException) {
            // estilo invalido: se ignora
        }
    }

    public void setBoxSize(Dimension dimension) {
        this.customSize = dimension == null ? null : new Dimension(dimension);
        this.setLineWrap(dimension != null);
        this.setWrapStyleWord(dimension != null);
        this.styled = true;
    }

    public void applyZoom(int n) {
        if (this.baseFont != null) {
            this.setFont(this.baseFont.deriveFont(Math.max(4.0f, this.baseFont.getSize2D() * (float)n / 11.0f)));
        } else {
            this.setFont(new Font("Default", 0, n));
        }
    }

    private void fitSize() {
        Dimension dimension;
        if (this.customSize != null) {
            dimension = new Dimension(this.customSize);
        } else {
            dimension = this.getPreferredSize();
            dimension = new Dimension(Math.max(60, dimension.width + 4), Math.max(24, dimension.height + 2));
        }
        if (!dimension.equals(this.getSize())) {
            this.setSize(dimension);
        }
    }

    private void saveForUndo() {
        if (this.myView != null && this.myView.getDrawer().getAutomaton().getEnvironmentFrame() != null) {
            AutomatonEnvironment automatonEnvironment = (AutomatonEnvironment)this.myView.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment();
            automatonEnvironment.saveStatus();
            automatonEnvironment.setDirty();
        }
    }

    private void changed() {
        this.styled = true;
        this.setDisabledTextColor(this.getForeground());
        this.setCaretColor(this.getForeground());
        this.fitSize();
        if (this.myView != null) {
            this.myView.repaint();
        }
    }

    private void chooseFont() {
        Font font = this.baseFont != null ? this.baseFont : this.getFont();
        JComboBox<String> families = new JComboBox<String>(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        families.setSelectedItem(font.getFamily());
        JSpinner size = new JSpinner(new SpinnerNumberModel(font.getSize(), 6, 96, 1));
        JCheckBox bold = new JCheckBox("Bold", font.isBold());
        JCheckBox italic = new JCheckBox("Italic", font.isItalic());
        JPanel jPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        jPanel.add(new JLabel("Font:"));
        jPanel.add(families);
        jPanel.add(new JLabel("Size:"));
        jPanel.add(size);
        jPanel.add(bold);
        jPanel.add(italic);
        if (JOptionPane.showConfirmDialog(this, jPanel, "Note font", 2, -1) != 0) {
            return;
        }
        this.saveForUndo();
        this.baseFont = new Font((String)families.getSelectedItem(), (bold.isSelected() ? 1 : 0) | (italic.isSelected() ? 2 : 0), ((Number)size.getValue()).intValue());
        this.setFont(this.baseFont);
        this.changed();
    }

    private void chooseBox() {
        Dimension dimension = this.customSize != null ? this.customSize : this.getSize();
        JSpinner width = new JSpinner(new SpinnerNumberModel(Math.max(60, dimension.width), 30, 3000, 10));
        JSpinner height = new JSpinner(new SpinnerNumberModel(Math.max(24, dimension.height), 16, 3000, 10));
        JPanel jPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        jPanel.add(new JLabel("Width (px):"));
        jPanel.add(width);
        jPanel.add(new JLabel("Height (px):"));
        jPanel.add(height);
        if (JOptionPane.showConfirmDialog(this, jPanel, "Note box size", 2, -1) != 0) {
            return;
        }
        this.saveForUndo();
        this.setBoxSize(new Dimension(((Number)width.getValue()).intValue(), ((Number)height.getValue()).intValue()));
        this.changed();
    }

    private void showStyleMenu(MouseEvent mouseEvent) {
        JPopupMenu jPopupMenu = new JPopupMenu();
        JMenuItem font = new JMenuItem("Font / size...");
        JMenuItem fg = new JMenuItem("Text color...");
        JMenuItem bg = new JMenuItem("Background color...");
        JMenuItem box = new JMenuItem("Box size...");
        JMenuItem auto = new JMenuItem("Auto box size");
        JMenuItem reset = new JMenuItem("Reset style");
        font.addActionListener(e -> this.chooseFont());
        fg.addActionListener(e -> {
            Color color = JColorChooser.showDialog(this, "Note text color", this.getForeground());
            if (color != null) {
                this.saveForUndo();
                this.setForeground(color);
                this.changed();
            }
        });
        bg.addActionListener(e -> {
            Color color = JColorChooser.showDialog(this, "Note background color", this.getBackground());
            if (color != null) {
                this.saveForUndo();
                this.setBackground(color);
                this.changed();
            }
        });
        box.addActionListener(e -> this.chooseBox());
        auto.addActionListener(e -> {
            this.saveForUndo();
            this.setBoxSize(null);
            this.changed();
        });
        reset.addActionListener(e -> {
            this.saveForUndo();
            this.baseFont = null;
            this.customSize = null;
            this.setLineWrap(false);
            this.setWrapStyleWord(false);
            this.setFont(javax.swing.UIManager.getFont("TextArea.font"));
            this.setForeground(DEFAULT_FG);
            this.setBackground(DEFAULT_BG);
            this.styled = false;
            this.changed();
            this.styled = false;
        });
        jPopupMenu.add(font);
        jPopupMenu.add(fg);
        jPopupMenu.add(bg);
        jPopupMenu.add(box);
        jPopupMenu.add(auto);
        jPopupMenu.addSeparator();
        jPopupMenu.add(reset);
        jPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
    }

    public Note(Point point, String string) {
        this.setLocationManually(point);
        this.setText(string);
    }

    public Note(String string) {
        this.setText(string);
    }

    public Note(Point point) {
        this.setLocationManually(point);
    }

    public void setView(AutomatonPane automatonPane) {
        this.myView = automatonPane;
    }

    public void initializeForView(AutomatonPane automatonPane) {
        this.myView = automatonPane;
        this.setLocationManually(this.myAutoPoint);
        if (!this.styled) {
            this.setForeground(DEFAULT_FG);
            this.setBackground(DEFAULT_BG);
        }
        this.setDisabledTextColor(this.getForeground());
        this.setCaretColor(this.getForeground());
        this.addMouseListener(new java.awt.event.MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    Note.this.showStyleMenu(mouseEvent);
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    Note.this.showStyleMenu(mouseEvent);
                }
            }
        });
        this.addMouseMotionListener(new MouseMotionListener(){

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    return;
                }
                if (!((Note)mouseEvent.getSource()).isEditable()) {
                    int n = mouseEvent.getPoint().x - Note.this.initialPointClick.x;
                    int n2 = mouseEvent.getPoint().y - Note.this.initialPointClick.y;
                    int n3 = Note.this.initialPointState.x + n;
                    int n4 = Note.this.initialPointState.y + n2;
                    ((Note)mouseEvent.getSource()).setLocationManually(new Point(n3, n4));
                    Note.this.initialPointState = new Point(((Note)mouseEvent.getSource()).getAutoPoint());
                }
                Note.this.myView.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
            }
        });
        this.addMouseListener(new MouseListener(){

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (Note.this.myView.getDrawer().getAutomaton().getEnvironmentFrame() != null) {
                    ((AutomatonEnvironment)Note.this.myView.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
                }
                ((Note)mouseEvent.getComponent()).setEnabled(true);
                ((Note)mouseEvent.getComponent()).setEditable(true);
                ((Note)mouseEvent.getComponent()).setCaretColor(((Note)mouseEvent.getComponent()).getForeground());
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                if (Note.this.myView.getDrawer().getAutomaton().getEnvironmentFrame() != null) {
                    ((AutomatonEnvironment)Note.this.myView.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
                    ((AutomatonEnvironment)Note.this.myView.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).setDirty();
                }
                Note.this.initialPointState = new Point(((Note)mouseEvent.getSource()).getAutoPoint());
                Note.this.initialPointClick = new Point(mouseEvent.getPoint().x, mouseEvent.getPoint().y);
                EditorPane editorPane = Note.this.myView.getCreator();
                Tool tool = editorPane.getToolBar().getCurrentTool();
                if (tool instanceof DeleteTool) {
                    Note.this.myView.remove((Note)mouseEvent.getSource());
                    Note.this.myView.getDrawer().getAutomaton().deleteNote((Note)mouseEvent.getSource());
                    Note.this.myView.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
            }
        });
        this.myView.add(this);
        this.setEnabled(true);
        this.setEditable(true);
        this.setCaretColor(this.getForeground());
        this.setSelectionStart(0);
        this.setSelectionEnd(this.getColumnWidth());
        this.requestFocus();
    }

    public Point getAutoPoint() {
        return this.myAutoPoint;
    }

    public AutomatonPane getView() {
        return this.myView;
    }

    public void setLocationManually(Point point) {
        this.moving = true;
        this.myAutoPoint = point;
        if (this.myView != null) {
            this.setLocation(this.myView.transformFromAutomatonToView(point));
        }
    }

    @Override
    public void setLocation(Point point) {
        if (this.moving && this.myView != null) {
            this.myViewPoint = point;
            super.setLocation(point);
        }
    }

    @Override
    public void setLocation(int n, int n2) {
        if (this.moving) {
            super.setLocation(n, n2);
        }
        this.moving = false;
    }

    public void updateView() {
        this.fitSize();
        this.setLocationManually(this.myAutoPoint);
    }

    public int specialHash() {
        return this.myAutoPoint == null ? -1 : this.myAutoPoint.hashCode() + this.getText().hashCode();
    }
}

