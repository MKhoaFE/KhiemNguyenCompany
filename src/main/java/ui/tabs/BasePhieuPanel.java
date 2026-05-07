package ui.tabs;

import ui.tabs.model.CardType;
import ui.tabs.dao.CardTypeDAO;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.SwingUtilities;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared UI constants, colors, fonts and helper widgets for all Phieu panels.
 * F1, F2, F3 extend this class.
 */
public abstract class BasePhieuPanel extends JPanel {

    // ── Colors ────────────────────────────────────────────────────────────────
    protected static final Color C_BG       = new Color(0xECF0F1);
    protected static final Color C_WHITE    = Color.WHITE;
    protected static final Color C_ACCENT   = new Color(0x1565C0);
    protected static final Color C_GREEN    = new Color(0x2E7D32);
    protected static final Color C_RED      = new Color(0xC62828);
    protected static final Color C_ORANGE   = new Color(0xE65100);
    protected static final Color C_YELLOW_H = new Color(0xFFF176);
    protected static final Color C_GRID     = new Color(0xB0BEC5);
    protected static final Color C_TH_BG    = new Color(0x1565C0);
    protected static final Color C_TH_FG    = Color.WHITE;
    protected static final Color C_ROW1     = Color.WHITE;
    protected static final Color C_ROW2     = new Color(0xE3F2FD);
    protected static final Color C_TOTAL_BG = new Color(0xBBDEFB);
    protected static final Color C_BORDER   = new Color(0x90A4AE);
    protected static final Color C_TT_RED   = new Color(0xCC0000);
    protected static final Color C_SEL_BILL = new Color(0xFFF9C4);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    protected static final Font F_TITLE  = new Font("Arial", Font.BOLD, 16);
    protected static final Font F_LABEL  = new Font("Arial", Font.PLAIN, 15);  // large for elderly
    protected static final Font F_BOLD   = new Font("Arial", Font.BOLD, 15);
    protected static final Font F_TABLE  = new Font("Arial", Font.PLAIN, 14);
    protected static final Font F_TH     = new Font("Arial", Font.BOLD, 13);
    protected static final Font F_BTN    = new Font("Arial", Font.BOLD, 14);
    protected static final Font F_TOTAL  = new Font("Arial", Font.BOLD, 16);
    protected static final Font F_LIST   = new Font("Arial", Font.PLAIN, 13);
    protected static final Font F_CODE_RED = new Font("Arial", Font.BOLD, 15);

    protected static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    protected static final NumberFormat NF = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ── Shared state ─────────────────────────────────────────────────────────
    protected List<CardType> allCards;
    protected Map<String, CardType> codeMap = new LinkedHashMap<>();
    protected LocalDate currentDate = LocalDate.now();

    protected void initCards() {
        allCards = new CardTypeDAO().getAll();
        for (CardType ct : allCards) {
            String code = (ct.code != null && !ct.code.isBlank()) ? ct.code : CardTypeDAO.deriveCode(ct.name);
            ct.code = code;
            codeMap.put(code.toUpperCase(), ct);
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    protected JLabel lbl(String t) {
        JLabel l = new JLabel(t); l.setFont(F_LABEL); return l;
    }

    protected JButton mkBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(F_BTN); b.setForeground(fg);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 14, 6, 14));
        return b;
    }

    protected JButton iconBtn(String t) {
        JButton b = new JButton(t);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setPreferredSize(new Dimension(28, 28));
        b.setMargin(new Insets(0, 0, 0, 0));
        return b;
    }

    protected void styleField(JTextField f) {
        f.setFont(F_LABEL);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(3, 6, 3, 6)));
    }

    protected void styleTable(JTable t) {
        t.setFont(F_TABLE);
        t.setRowHeight(26);
        t.setShowGrid(true);
        t.setGridColor(C_GRID);
        t.setBackground(C_ROW1);
        t.setSelectionBackground(C_YELLOW_H);
        t.setSelectionForeground(Color.BLACK);
        t.setIntercellSpacing(new Dimension(1, 1));
        JTableHeader th = t.getTableHeader();
        th.setFont(F_TH); th.setBackground(C_TH_BG); th.setForeground(C_TH_FG);
        th.setPreferredSize(new Dimension(0, 28));
        th.setReorderingAllowed(false);
    }

    // ── Calculation helpers ───────────────────────────────────────────────────

    protected int toInt(Object v) {
        if (v == null) return 0;
        try { return Integer.parseInt(v.toString().replace(",","").replace(".","").trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    protected long toLong(Object v) {
        if (v == null) return 0;
        try { return Long.parseLong(v.toString().replace(",","").replace(".","").trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    protected double toDbl(Object v) {
        if (v == null) return 0;
        try { return Double.parseDouble(v.toString().replace("%","").trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    protected void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /** Run Runnable while suppressing all table model listeners */
    protected void suppress(DefaultTableModel model, Runnable r) {
        javax.swing.event.TableModelListener[] ls = model.getTableModelListeners();
        for (javax.swing.event.TableModelListener l : ls) model.removeTableModelListener(l);
        try { r.run(); }
        finally { for (javax.swing.event.TableModelListener l : ls) model.addTableModelListener(l); }
    }

    protected CardType findById(int id) {
        for (CardType ct : allCards) if (ct.id == id) return ct;
        return null;
    }

    // ── Standard cell editors ─────────────────────────────────────────────────

    /** %CK editor: shows raw "4.5" not "4.5%" — fixes the 10.0→100 bug completely */
    protected class CKEditor extends DefaultCellEditor {
        protected final JTextField tf;
        public CKEditor() {
            super(new JTextField());
            tf = (JTextField) getComponent();
            tf.setFont(F_TABLE);
            tf.setHorizontalAlignment(SwingConstants.CENTER);
            tf.setBorder(BorderFactory.createLineBorder(new Color(0xE53935), 2));
            tf.setBackground(new Color(0xFFF8F8));
            setClickCountToStart(1);
        }
        @Override public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int row, int col) {
            // Strip "%" and display only the numeric value  — prevents "10.0%" → selectAll → "100"
            double d = toDbl(val);
            tf.setText(d == (long) d ? String.valueOf((long) d) : String.valueOf(d));
            SwingUtilities.invokeLater(tf::selectAll);
            return tf;
        }
        @Override public Object getCellEditorValue() {
            try { return Double.parseDouble(tf.getText().trim()); }
            catch (NumberFormatException e) { return 0.0; }
        }
    }

    /** Uppercase code editor */
    protected class CodeEditor extends DefaultCellEditor {
        private final JTextField tf;
        public CodeEditor() {
            super(new JTextField());
            tf = (JTextField) getComponent();
            tf.setFont(new Font("Arial", Font.BOLD, 14));
            tf.setBorder(BorderFactory.createLineBorder(C_ACCENT, 2));
            setClickCountToStart(1);
        }
        @Override public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int row, int col) {
            tf.setText(val != null ? val.toString() : "");
            SwingUtilities.invokeLater(tf::selectAll);
            return tf;
        }
        @Override public Object getCellEditorValue() { return tf.getText().trim().toUpperCase(); }
    }

    /** Integer editor */
    protected class IntEditor extends DefaultCellEditor {
        private final JTextField tf;
        public IntEditor() {
            super(new JTextField());
            tf = (JTextField) getComponent();
            tf.setFont(F_TABLE); tf.setHorizontalAlignment(SwingConstants.RIGHT);
            tf.setBorder(BorderFactory.createLineBorder(C_ACCENT, 2));
            setClickCountToStart(1);
        }
        @Override public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int row, int col) {
            tf.setText(val != null ? val.toString().replace(",","").replace(".","") : "0");
            SwingUtilities.invokeLater(tf::selectAll);
            return tf;
        }
        @Override public Object getCellEditorValue() {
            try { return Integer.parseInt(tf.getText().trim().replace(",","")); }
            catch (NumberFormatException e) { return 0; }
        }
    }

    protected class ReadOnlyEditor extends DefaultCellEditor {
        public ReadOnlyEditor() { super(new JTextField()); setClickCountToStart(999); }
    }

    // ── Standard item table renderer ──────────────────────────────────────────

    /** Columns: 0=code, 1=name, 2=dvt, 3=qty, 4=price, 5=ck%, 6=total */
    protected class ItemRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object val,
                                                                 boolean sel, boolean foc, int row, int col) {
            if (val != null) {
                if (col == 4 || col == 6) { try { val = NF.format(Long.parseLong(val.toString())); } catch (Exception ignore) {} }
                if (col == 5)             { try { val = toDbl(val) + "%"; } catch (Exception ignore) {} }
            }
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setFont(F_TABLE);
            setBorder(new EmptyBorder(0, 4, 0, 4));
            if (sel) { setBackground(C_YELLOW_H); setForeground(Color.BLACK); }
            else {
                setBackground(row % 2 == 0 ? C_ROW1 : C_ROW2);
                setForeground(col == 6 ? C_TT_RED : Color.BLACK);
            }
            setHorizontalAlignment(col >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (col == 5) setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }
}