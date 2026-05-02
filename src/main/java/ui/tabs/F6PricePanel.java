package ui.tabs;

import ui.tabs.dao.CardTypeDAO;
import db.DBConnection;
import ui.tabs.model.CardType;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * F6 - Bảng Giá (Price List)
 *
 * Layout:
 *  ┌───────────────────────────────────────────────────────┐
 *  │  HEADER "Bảng Giá"          [🔍 Tìm kiếm...]         │
 *  ├──────────────────────────────────────────────────────  │
 *  │  TABLE (editable: Giá, Giá CK)                        │
 *  │  # | Tên | Mệnh giá | Giá bán | Giá CK | % CK        │
 *  ├──────────────────────────────────────────────────────  │
 *  │            [Hoàn tác]   [💾 Lưu thay đổi]             │
 *  └───────────────────────────────────────────────────────┘
 */
public class F6PricePanel extends JPanel {

    // ── palette ──────────────────────────────────────────────────────────────
    private static final Color BG          = new Color(0xF5F6FA);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color ACCENT      = new Color(0x2563EB);
    private static final Color SUCCESS     = new Color(0x16A34A);
    private static final Color DANGER      = new Color(0xEF4444);
    private static final Color WARNING     = new Color(0xD97706);
    private static final Color TEXT_MAIN   = new Color(0x1E293B);
    private static final Color TEXT_MUTED  = new Color(0x64748B);
    private static final Color BORDER_CLR  = new Color(0xE2E8F0);
    private static final Color ROW_ALT     = new Color(0xF8FAFC);
    private static final Color HEADER_BG   = new Color(0xEFF6FF);
    private static final Color EDIT_BG     = new Color(0xFFFBEB);   // warm yellow for editable cells
    private static final Color CHANGED_BG  = new Color(0xFEF3C7);   // highlight modified cells

    // ── fonts ────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_TABLE  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_TH     = new Font("Segoe UI", Font.BOLD, 12);

    private static final NumberFormat NF = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ── data ─────────────────────────────────────────────────────────────────
    private List<CardType> allCardTypes;
    private List<CardType> displayedTypes;

    // track which rows have been changed: index into displayedTypes
    private boolean[] changed;

    // ── widgets ───────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;
    private JLabel lblChangedCount;

    // ─────────────────────────────────────────────────────────────────────────

    public F6PricePanel() {
        allCardTypes = new CardTypeDAO().getAll();
        displayedTypes = new ArrayList<>(allCardTypes);
        changed = new boolean[allCardTypes.size()];

        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── HEADER ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(0, 0, 16, 0));

        // left
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel icon = new JLabel("💰");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel title = new JLabel("Bảng Giá");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_MAIN);

        JLabel sub = new JLabel(" — click vào ô Giá / Giá CK để chỉnh sửa");
        sub.setFont(FONT_SMALL);
        sub.setForeground(TEXT_MUTED);

        left.add(icon);
        left.add(title);
        left.add(sub);

        // right: search
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        txtSearch = new JTextField(18);
        txtSearch.setFont(FONT_TABLE);
        txtSearch.setPreferredSize(new Dimension(200, 36));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        txtSearch.putClientProperty("JTextField.placeholderText", "🔍  Tìm theo tên...");

        // manual placeholder
        txtSearch.setForeground(TEXT_MUTED);
        txtSearch.setText("Tìm theo tên...");
        txtSearch.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (txtSearch.getText().equals("Tìm theo tên...")) {
                    txtSearch.setText("");
                    txtSearch.setForeground(TEXT_MAIN);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (txtSearch.getText().isBlank()) {
                    txtSearch.setForeground(TEXT_MUTED);
                    txtSearch.setText("Tìm theo tên...");
                }
            }
        });
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e)  { filterTable(); }
            public void insertUpdate(DocumentEvent e)  { filterTable(); }
        });

        right.add(new JLabel("🔍") {{ setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16)); }});
        right.add(txtSearch);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ── TABLE ─────────────────────────────────────────────────────────────────

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));

        String[] cols = {"#", "Tên sản phẩm", "Mệnh giá (đ)", "Giá bán (đ)", "Giá CK (đ)", "% CK"};

        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                // Only Giá (col 3) and Giá CK (col 4) are editable
                return c == 3 || c == 4;
            }
        };

        // Populate
        for (CardType ct : displayedTypes) {
            double ckPct = ct.denomination > 0
                    ? Math.round((1.0 - (double) ct.defaultDiscount / ct.denomination) * 1000.0) / 10.0
                    : 0.0;
            tableModel.addRow(new Object[]{
                    tableModel.getRowCount() + 1,
                    ct.name,
                    ct.denomination,
                    ct.defaultPrice,
                    ct.defaultDiscount,
                    ckPct + "%"
            });
        }

        // Listen for changes
        tableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if ((col == 3 || col == 4) && row >= 0 && row < displayedTypes.size()) {
                changed[row] = true;
                // Recalc % CK
                recalcCKPercent(row);
                updateChangedLabel();
            }
        });

        table = new JTable(tableModel);
        table.setFont(FONT_TABLE);
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER_CLR);
        table.setSelectionBackground(new Color(0xDBEAFE));
        table.setSelectionForeground(TEXT_MAIN);
        table.setBackground(CARD_BG);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Header
        JTableHeader th = table.getTableHeader();
        th.setFont(FONT_TH);
        th.setBackground(HEADER_BG);
        th.setForeground(new Color(0x3730A3));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0xC7D2FE)));
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(th.getWidth(), 42));

        // Widths
        int[] widths = {40, 220, 140, 140, 140, 80};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Renderers
        table.setDefaultRenderer(Object.class, new PriceTableRenderer());

        // Number editor for price columns
        for (int c : new int[]{3, 4}) {
            table.getColumnModel().getColumn(c).setCellEditor(new NumberCellEditor());
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        // Legend bar
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        legend.setBackground(new Color(0xFAFAFC));
        legend.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR));

        legend.add(legendDot(EDIT_BG, "Ô có thể chỉnh sửa"));
        legend.add(legendDot(CHANGED_BG, "Đã thay đổi (chưa lưu)"));

        card.add(scroll, BorderLayout.CENTER);
        card.add(legend, BorderLayout.SOUTH);
        return card;
    }

    private JPanel legendDot(Color color, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JLabel dot = new JLabel("■");
        dot.setForeground(color.darker());
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        p.add(dot);
        p.add(lbl);
        return p;
    }

    // ── FOOTER ────────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 0, 0, 0));

        lblChangedCount = new JLabel("Chưa có thay đổi");
        lblChangedCount.setFont(FONT_LABEL);
        lblChangedCount.setForeground(TEXT_MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnReset = styledButton("↺  Hoàn tác", WARNING, Color.WHITE, false);
        btnReset.addActionListener(e -> resetChanges());

        JButton btnSave = styledButton("💾  Lưu thay đổi", SUCCESS, Color.WHITE, true);
        btnSave.addActionListener(e -> saveChanges());

        actions.add(btnReset);
        actions.add(btnSave);

        p.add(lblChangedCount, BorderLayout.WEST);
        p.add(actions, BorderLayout.EAST);
        return p;
    }

    // ── LOGIC ─────────────────────────────────────────────────────────────────

    private void filterTable() {
        String query = txtSearch.getText().trim().toLowerCase();
        if (query.equals("tìm theo tên...")) query = "";

        // Stop editing
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        tableModel.setRowCount(0);
        displayedTypes.clear();

        for (CardType ct : allCardTypes) {
            if (query.isEmpty() || ct.name.toLowerCase().contains(query)) {
                displayedTypes.add(ct);
                double ckPct = ct.denomination > 0
                        ? Math.round((1.0 - (double) ct.defaultDiscount / ct.denomination) * 1000.0) / 10.0
                        : 0.0;
                tableModel.addRow(new Object[]{
                        tableModel.getRowCount() + 1,
                        ct.name,
                        ct.denomination,
                        ct.defaultPrice,
                        ct.defaultDiscount,
                        ckPct + "%"
                });
            }
        }

        changed = new boolean[displayedTypes.size()];
        updateChangedLabel();
    }

    private void recalcCKPercent(int row) {
        try {
            int denom    = toInt(tableModel.getValueAt(row, 2));
            int discount = toInt(tableModel.getValueAt(row, 4));
            double pct = denom > 0
                    ? Math.round((1.0 - (double) discount / denom) * 1000.0) / 10.0
                    : 0.0;
            // Temporarily remove listener to avoid recursive update
            tableModel.removeTableModelListener(tableModel.getTableModelListeners()[0]);
            tableModel.setValueAt(pct + "%", row, 5);
            tableModel.addTableModelListener(e -> {
                int r = e.getFirstRow();
                int c = e.getColumn();
                if ((c == 3 || c == 4) && r >= 0 && r < displayedTypes.size()) {
                    changed[r] = true;
                    recalcCKPercent(r);
                    updateChangedLabel();
                }
            });
        } catch (Exception ignore) {}
    }

    private void updateChangedLabel() {
        int count = 0;
        for (boolean b : changed) if (b) count++;
        if (count == 0) {
            lblChangedCount.setText("Chưa có thay đổi");
            lblChangedCount.setForeground(TEXT_MUTED);
        } else {
            lblChangedCount.setText("⚠  " + count + " mặt hàng đã thay đổi (chưa lưu)");
            lblChangedCount.setForeground(WARNING);
        }
    }

    private void resetChanges() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Hoàn tác toàn bộ thay đổi chưa lưu?", "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        // Reload from DB
        allCardTypes = new CardTypeDAO().getAll();
        filterTable();
    }

    private void saveChanges() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        boolean hasChanges = false;
        for (boolean b : changed) if (b) { hasChanges = true; break; }

        if (!hasChanges) {
            JOptionPane.showMessageDialog(this,
                    "Không có thay đổi nào để lưu.", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE card_types SET default_price=?, default_discount=? WHERE id=?");

            int savedCount = 0;
            for (int i = 0; i < displayedTypes.size(); i++) {
                if (!changed[i]) continue;

                CardType ct = displayedTypes.get(i);
                int price    = toInt(tableModel.getValueAt(i, 3));
                int discount = toInt(tableModel.getValueAt(i, 4));

                // validate
                if (price < 0 || discount < 0) {
                    showError("Giá không thể âm (dòng " + (i + 1) + ")");
                    return;
                }

                ps.setInt(1, price);
                ps.setInt(2, discount);
                ps.setInt(3, ct.id);
                ps.addBatch();

                // update local model
                ct.defaultPrice    = price;
                ct.defaultDiscount = discount;
                savedCount++;
            }

            ps.executeBatch();

            // clear changed flags
            changed = new boolean[displayedTypes.size()];
            updateChangedLabel();

            // Repaint to clear highlights
            table.repaint();

            JOptionPane.showMessageDialog(this,
                    "✅ Đã cập nhật " + savedCount + " mặt hàng!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Lỗi khi lưu: " + ex.getMessage());
        }
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        try { return Integer.parseInt(val.toString().replace(",", "").replace(".", "").trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // ── BUTTON FACTORY ────────────────────────────────────────────────────────

    private JButton styledButton(String text, Color bg, Color fg, boolean bold) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() :
                        getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(fg);
        b.setFont(bold ? FONT_BOLD : FONT_LABEL);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        return b;
    }

    // ── RENDERERS / EDITORS ───────────────────────────────────────────────────

    private class PriceTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                                                       boolean sel, boolean foc, int row, int col) {

            // Format numbers
            if (val != null && (col == 2 || col == 3 || col == 4)) {
                try {
                    long v = Long.parseLong(val.toString());
                    val = NF.format(v);
                } catch (NumberFormatException ignore) {}
            }

            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setFont(FONT_TABLE);
            setBorder(new EmptyBorder(0, 10, 0, 10));

            if (sel) {
                setBackground(new Color(0xDBEAFE));
                setForeground(TEXT_MAIN);
            } else {
                boolean isEditable = col == 3 || col == 4;
                boolean isChanged  = isEditable && row < changed.length && changed[row];

                if (isChanged) {
                    setBackground(CHANGED_BG);
                    setForeground(new Color(0x92400E));
                } else if (isEditable) {
                    setBackground(EDIT_BG);
                    setForeground(TEXT_MAIN);
                } else {
                    setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                    setForeground(TEXT_MAIN);
                }
            }

            // Right-align numbers
            if (col == 2 || col == 3 || col == 4) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(new EmptyBorder(0, 8, 0, 14));
            } else if (col == 5) {
                setHorizontalAlignment(SwingConstants.CENTER);
                // Color % CK
                if (!sel) {
                    String s = val != null ? val.toString().replace("%", "").trim() : "0";
                    try {
                        double pct = Double.parseDouble(s);
                        if (pct > 0) setForeground(new Color(0xDC2626));
                    } catch (NumberFormatException ignore) {}
                }
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            return this;
        }
    }

    /** Editor that accepts plain integer input */
    private class NumberCellEditor extends DefaultCellEditor {
        private final JTextField field;

        NumberCellEditor() {
            super(new JTextField());
            field = (JTextField) getComponent();
            field.setFont(FONT_TABLE);
            field.setHorizontalAlignment(SwingConstants.RIGHT);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT, 2, true),
                    new EmptyBorder(2, 8, 2, 8)
            ));
            field.setBackground(new Color(0xEFF6FF));
            setClickCountToStart(1);
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                                                     boolean sel, int row, int col) {
            // Show raw number without formatting for easy editing
            String raw = val != null ? val.toString().replace(",", "").replace(".", "").trim() : "0";
            field.setText(raw);
            SwingUtilities.invokeLater(field::selectAll);
            return field;
        }

        @Override
        public Object getCellEditorValue() {
            String text = field.getText().trim().replace(",", "").replace(".", "");
            try { return Integer.parseInt(text); }
            catch (NumberFormatException e) { return 0; }
        }

        @Override
        public boolean stopCellEditing() {
            try {
                String t = field.getText().trim().replace(",", "").replace(".", "");
                Integer.parseInt(t);
            } catch (NumberFormatException e) {
                field.setBackground(new Color(0xFEE2E2));
                return false;
            }
            return super.stopCellEditing();
        }
    }
}