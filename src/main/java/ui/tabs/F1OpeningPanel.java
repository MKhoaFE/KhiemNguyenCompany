package ui.tabs;

import ui.tabs.dao.CardTypeDAO;
import db.DBConnection;
import ui.tabs.model.CardType;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * F1 - Tồn Đầu (Opening Stock)
 *
 * Layout:
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  HEADER  "Tồn Đầu Ngày"          [Ngày: dd/MM/yyyy]    │
 *  ├──────────────────────────────────────────────────────── │
 *  │  FORM  [Sản phẩm ▾]  [Số lượng]  [Giá]  [Giá CK]  [+] │
 *  ├──────────────────────────────────────────────────────── │
 *  │  TABLE  (scrollable)                                    │
 *  │  #  | Sản phẩm | Mệnh giá | Số lượng | Giá | Giá CK | Xóa │
 *  ├──────────────────────────────────────────────────────── │
 *  │  FOOTER  Tổng SL: xxx   Tổng tiền: xxx,xxx đ  [Lưu tất cả] │
 *  └─────────────────────────────────────────────────────────┘
 */
public class F1OpeningPanel extends JPanel {

    // ── palette ──────────────────────────────────────────────────────────────
    private static final Color BG          = new Color(0xF5F6FA);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color ACCENT      = new Color(0x2563EB);   // blue-600
    private static final Color ACCENT_DARK = new Color(0x1D4ED8);
    private static final Color DANGER      = new Color(0xEF4444);
    private static final Color SUCCESS     = new Color(0x16A34A);
    private static final Color TEXT_MAIN   = new Color(0x1E293B);
    private static final Color TEXT_MUTED  = new Color(0x64748B);
    private static final Color BORDER_CLR  = new Color(0xE2E8F0);
    private static final Color ROW_ALT     = new Color(0xF8FAFC);
    private static final Color HEADER_BG   = new Color(0xEFF6FF);

    // ── fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_TABLE  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_TH     = new Font("Segoe UI", Font.BOLD, 12);

    // ── state ────────────────────────────────────────────────────────────────
    private List<CardType> cardTypes;
    private LocalDate selectedDate = LocalDate.now();

    // ── form widgets ─────────────────────────────────────────────────────────
    private JComboBox<CardType> cbCard;
    private JTextField txtQty;
    private JTextField txtPrice;
    private JTextField txtDiscount;
    private JLabel lblDate;

    // ── table ────────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable table;

    // ── footer labels ────────────────────────────────────────────────────────
    private JLabel lblTotalQty;
    private JLabel lblTotalAmount;

    private static final NumberFormat NF = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ─────────────────────────────────────────────────────────────────────────

    public F1OpeningPanel() {
        cardTypes = new CardTypeDAO().getAll();

        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        loadTodayData();
    }

    // ── HEADER ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(0, 0, 16, 0));

        // left: icon + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel icon = new JLabel("📦");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel title = new JLabel("Tồn Đầu Ngày");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_MAIN);

        left.add(icon);
        left.add(title);

        // right: date selector
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JLabel dateLbl = new JLabel("Ngày:");
        dateLbl.setFont(FONT_LABEL);
        dateLbl.setForeground(TEXT_MUTED);

        lblDate = new JLabel(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblDate.setFont(FONT_BOLD);
        lblDate.setForeground(ACCENT);

        JButton btnDate = styledButton("Chọn ngày", ACCENT, Color.WHITE, false);
        btnDate.setFont(FONT_SMALL);
        btnDate.addActionListener(e -> pickDate());

        right.add(dateLbl);
        right.add(lblDate);
        right.add(btnDate);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ── CENTER (form + table) ─────────────────────────────────────────────────

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);
        p.add(buildFormCard(), BorderLayout.NORTH);
        p.add(buildTableCard(), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFormCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 10));

        JLabel lbl = new JLabel("Thêm mặt hàng");
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(TEXT_MUTED);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        card.add(lbl, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);

        // Product combo
        cbCard = new JComboBox<>(cardTypes.toArray(new CardType[0]));
        cbCard.setFont(FONT_TABLE);
        cbCard.setPreferredSize(new Dimension(200, 36));
        styleCombo(cbCard);
        cbCard.addActionListener(e -> onCardSelected());

        // Qty
        txtQty = styledField("0", 70);

        // Price
        txtPrice = styledField("0", 110);

        // Discount
        txtDiscount = styledField("0", 110);

        // Add button
        JButton btnAdd = styledButton("+ Thêm", ACCENT, Color.WHITE, true);
        btnAdd.addActionListener(e -> addRow());

        row.add(fieldGroup("Sản phẩm", cbCard));
        row.add(fieldGroup("Số lượng", txtQty));
        row.add(fieldGroup("Giá bán (đ)", txtPrice));
        row.add(fieldGroup("Giá CK (đ)", txtDiscount));
        row.add(fieldGroup(" ", btnAdd));

        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTableCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 0));

        // table model — columns: #, Sản phẩm, Mệnh giá, SL, Giá, Giá CK, [Xóa]
        String[] cols = {"#", "Sản phẩm", "Mệnh giá (đ)", "Số lượng", "Giá bán (đ)", "Giá CK (đ)", "Xóa"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == 3 || c == 4 || c == 5 || c == 6; // SL, Giá, Giá CK, Xóa btn
            }
            @Override public Class<?> getColumnClass(int c) {
                return c == 6 ? JButton.class : Object.class;
            }
        };

        table = new JTable(tableModel);
        table.setFont(FONT_TABLE);
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER_CLR);
        table.setSelectionBackground(new Color(0xDBEAFE));
        table.setSelectionForeground(TEXT_MAIN);
        table.setBackground(CARD_BG);
        table.setIntercellSpacing(new Dimension(0, 0));

        // header style
        JTableHeader th = table.getTableHeader();
        th.setFont(FONT_TH);
        th.setBackground(HEADER_BG);
        th.setForeground(new Color(0x3730A3));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0xC7D2FE)));
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(th.getWidth(), 40));

        // column widths
        int[] widths = {40, 200, 130, 90, 130, 130, 70};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(6).setMaxWidth(80);

        // alternating rows
        table.setDefaultRenderer(Object.class, new AlternatingRowRenderer());

        // delete button column
        table.getColumn("Xóa").setCellRenderer(new DeleteButtonRenderer());
        table.getColumn("Xóa").setCellEditor(new DeleteButtonEditor());

        // right-align numbers
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{2, 3, 4, 5}) {
            table.getColumnModel().getColumn(c).setCellRenderer(new NumberCellRenderer());
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── FOOTER ────────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 0, 0, 0));

        // summary
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        summary.setOpaque(false);

        lblTotalQty = summaryLabel("Tổng SL: 0");
        lblTotalAmount = summaryLabel("Tổng tiền: 0 đ");

        summary.add(lblTotalQty);
        summary.add(new JSeparator(JSeparator.VERTICAL) {{
            setPreferredSize(new Dimension(1, 20));
            setForeground(BORDER_CLR);
        }});
        summary.add(lblTotalAmount);

        // actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnClear = styledButton("Xóa tất cả", DANGER, Color.WHITE, false);
        btnClear.addActionListener(e -> clearAll());

        JButton btnSave = styledButton("💾  Lưu tất cả", SUCCESS, Color.WHITE, true);
        btnSave.addActionListener(e -> saveAll());

        actions.add(btnClear);
        actions.add(btnSave);

        p.add(summary, BorderLayout.WEST);
        p.add(actions, BorderLayout.EAST);
        return p;
    }

    // ── LOGIC ─────────────────────────────────────────────────────────────────

    private void onCardSelected() {
        CardType c = (CardType) cbCard.getSelectedItem();
        if (c != null) {
            txtPrice.setText(String.valueOf(c.defaultPrice));
            txtDiscount.setText(String.valueOf(c.defaultDiscount));
        }
    }

    private void addRow() {
        try {
            CardType c = (CardType) cbCard.getSelectedItem();
            if (c == null) return;

            int qty = Integer.parseInt(txtQty.getText().trim());
            int price = Integer.parseInt(txtPrice.getText().trim().replace(",", "").replace(".", ""));
            int discount = Integer.parseInt(txtDiscount.getText().trim().replace(",", "").replace(".", ""));

            if (qty <= 0) {
                showError("Số lượng phải lớn hơn 0");
                return;
            }

            int row = tableModel.getRowCount() + 1;
            tableModel.addRow(new Object[]{
                    row,
                    c.name,
                    c.denomination,
                    qty,
                    price,
                    discount,
                    "Xóa"
            });

            // store card_type_id as client property so we can save later
            // We store it in column hidden data via row tag
            txtQty.setText("0");
            refreshFooter();

        } catch (NumberFormatException ex) {
            showError("Vui lòng nhập số hợp lệ");
        }
    }

    private void deleteRow(int viewRow) {
        if (viewRow >= 0 && viewRow < tableModel.getRowCount()) {
            tableModel.removeRow(viewRow);
            // re-number
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(i + 1, i, 0);
            }
            refreshFooter();
        }
    }

    private void clearAll() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa toàn bộ danh sách?", "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            tableModel.setRowCount(0);
            refreshFooter();
        }
    }

    private void saveAll() {
        if (tableModel.getRowCount() == 0) {
            showError("Chưa có mặt hàng nào để lưu");
            return;
        }

        // Stop any cell editing
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        try (Connection conn = DBConnection.getConnection()) {

            // Delete existing opening entries for this date first
            PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM inventory_entries WHERE date=? AND type='opening'");
            del.setString(1, selectedDate.toString());
            del.executeUpdate();

            // Insert all rows
            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO inventory_entries(date, type, card_type_id, quantity, price, discount_price)
                VALUES (?, 'opening', ?, ?, ?, ?)
            """);

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String name = tableModel.getValueAt(i, 1).toString();
                CardType ct = findCardByName(name);
                if (ct == null) continue;

                int qty      = toInt(tableModel.getValueAt(i, 3));
                int price    = toInt(tableModel.getValueAt(i, 4));
                int discount = toInt(tableModel.getValueAt(i, 5));

                ps.setString(1, selectedDate.toString());
                ps.setInt(2, ct.id);
                ps.setInt(3, qty);
                ps.setInt(4, price);
                ps.setInt(5, discount);
                ps.addBatch();
            }

            ps.executeBatch();

            JOptionPane.showMessageDialog(this,
                    "✅ Đã lưu " + tableModel.getRowCount() + " mặt hàng!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Lỗi khi lưu: " + ex.getMessage());
        }
    }

    private void pickDate() {
        String input = JOptionPane.showInputDialog(this,
                "Nhập ngày (dd/MM/yyyy):",
                selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        if (input == null || input.isBlank()) return;
        try {
            selectedDate = LocalDate.parse(input.trim(),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            lblDate.setText(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            loadTodayData();
        } catch (Exception ex) {
            showError("Ngày không hợp lệ. Định dạng: dd/MM/yyyy");
        }
    }

    private void loadTodayData() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT ie.quantity, ie.price, ie.discount_price, ct.name, ct.denomination
                FROM inventory_entries ie
                JOIN card_types ct ON ct.id = ie.card_type_id
                WHERE ie.date = ? AND ie.type = 'opening'
                ORDER BY ct.id
             """)) {

            ps.setString(1, selectedDate.toString());
            ResultSet rs = ps.executeQuery();
            int row = 1;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        row++,
                        rs.getString("name"),
                        rs.getInt("denomination"),
                        rs.getInt("quantity"),
                        rs.getInt("price"),
                        rs.getInt("discount_price"),
                        "Xóa"
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        refreshFooter();
    }

    private void refreshFooter() {
        int totalQty = 0;
        long totalAmt = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            int qty   = toInt(tableModel.getValueAt(i, 3));
            int price = toInt(tableModel.getValueAt(i, 4));
            totalQty += qty;
            totalAmt += (long) qty * price;
        }
        lblTotalQty.setText("Tổng SL: " + NF.format(totalQty));
        lblTotalAmount.setText("Tổng tiền: " + NF.format(totalAmt) + " đ");
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private CardType findCardByName(String name) {
        for (CardType c : cardTypes) {
            if (c.name.equals(name)) return c;
        }
        return null;
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        try { return Integer.parseInt(val.toString().replace(",", "").replace(".", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // ── UI FACTORY ────────────────────────────────────────────────────────────

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));
        return p;
    }

    private JTextField styledField(String placeholder, int width) {
        JTextField f = new JTextField(placeholder);
        f.setFont(FONT_TABLE);
        f.setPreferredSize(new Dimension(width, 36));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        f.setForeground(TEXT_MAIN);
        return f;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(2, 4, 2, 4)
        ));
        cb.setBackground(Color.WHITE);
    }

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
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        return b;
    }

    private JLabel summaryLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BOLD);
        l.setForeground(TEXT_MAIN);
        return l;
    }

    private JPanel fieldGroup(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        if (!label.isBlank()) {
            JLabel lbl = new JLabel(label);
            lbl.setFont(FONT_SMALL);
            lbl.setForeground(TEXT_MUTED);
            p.add(lbl, BorderLayout.NORTH);
        }
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    // ── RENDERERS / EDITORS ───────────────────────────────────────────────────

    private class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                                                       boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setFont(FONT_TABLE);
            if (sel) {
                setBackground(new Color(0xDBEAFE));
            } else {
                setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
            }
            setBorder(new EmptyBorder(0, 8, 0, 8));
            return this;
        }
    }

    private class NumberCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                                                       boolean sel, boolean foc, int row, int col) {
            if (val != null) {
                try {
                    long v = Long.parseLong(val.toString());
                    val = NF.format(v);
                } catch (NumberFormatException ignore) {}
            }
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(FONT_TABLE);
            if (sel) {
                setBackground(new Color(0xDBEAFE));
            } else {
                setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
            }
            setBorder(new EmptyBorder(0, 8, 0, 12));
            return this;
        }
    }

    private class DeleteButtonRenderer extends JButton implements TableCellRenderer {
        DeleteButtonRenderer() {
            setText("✕");
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                                                       boolean sel, boolean foc, int row, int col) {
            setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(DANGER);
            int pad = 4;
            g2.fillRoundRect(pad, pad, getWidth() - pad*2, getHeight() - pad*2, 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class DeleteButtonEditor extends DefaultCellEditor {
        private JButton btn;
        private int editRow;

        DeleteButtonEditor() {
            super(new JCheckBox());
            btn = new JButton("✕") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(DANGER);
                    int pad = 4;
                    g2.fillRoundRect(pad, pad, getWidth()-pad*2, getHeight()-pad*2, 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btn.setForeground(Color.WHITE);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.addActionListener(e -> {
                fireEditingStopped();
                deleteRow(editRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                                                     boolean sel, int row, int col) {
            editRow = row;
            return btn;
        }

        @Override public Object getCellEditorValue() { return "Xóa"; }
    }
}