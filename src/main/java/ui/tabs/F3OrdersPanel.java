package ui.tabs;

import db.DBConnection;
import ui.tabs.dao.CardTypeDAO;
import ui.tabs.model.CardType;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * F3 - Bán Hàng (Orders / Sales)
 *
 * Layout (BorderLayout):
 *  ┌────────────────┬──────────────────────────────────────────┐
 *  │  BILL LIST     │  BILL FORM                               │
 *  │  (left ~280px) │  Header: Ngày  [Chọn ngày]              │
 *  │                │  ┌─ Thông tin khách ───────────────────┐ │
 *  │  [+ Tạo mới]   │  │ Tên  Phone  Địa chỉ                │ │
 *  │                │  └────────────────────────────────────┘ │
 *  │  Bill #1       │  ┌─ Chi tiết đơn ──────────────────────┐ │
 *  │  Bill #2       │  │ TABLE: SP | MG | SL | %CK | Giá CK  │ │
 *  │  ...           │  │        | Tổng | [+dòng] | [Xóa]    │ │
 *  │                │  └────────────────────────────────────┘ │
 *  │                │  TỔNG HOÁ ĐƠN: xxx đ                   │
 *  │                │  [In bill]  [Lưu bill]                  │
 *  └────────────────┴──────────────────────────────────────────┘
 */
public class F3OrdersPanel extends JPanel {

    // ── palette ──────────────────────────────────────────────────────────────
    private static final Color BG         = new Color(0xF5F6FA);
    private static final Color CARD_BG    = Color.WHITE;
    private static final Color SIDEBAR_BG = new Color(0xF0F4FF);
    private static final Color ACCENT     = new Color(0x2563EB);
    private static final Color SUCCESS    = new Color(0x16A34A);
    private static final Color DANGER     = new Color(0xEF4444);
    private static final Color WARNING    = new Color(0xD97706);
    private static final Color TEXT_MAIN  = new Color(0x1E293B);
    private static final Color TEXT_MUTED = new Color(0x64748B);
    private static final Color BORDER_CLR = new Color(0xE2E8F0);
    private static final Color ROW_ALT    = new Color(0xF8FAFC);
    private static final Color HEADER_BG  = new Color(0xEFF6FF);
    private static final Color SELECTED_BILL = new Color(0xDBEAFE);

    // ── fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_TABLE  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_TH     = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_TOTAL  = new Font("Segoe UI", Font.BOLD, 16);

    private static final NumberFormat NF = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── state ────────────────────────────────────────────────────────────────
    private LocalDate selectedDate = LocalDate.now();
    private List<CardType> cardTypes;
    private Integer editingOrderId = null; // null = new bill

    // ── LEFT: bill list ───────────────────────────────────────────────────────
    private DefaultListModel<BillEntry> billListModel;
    private JList<BillEntry> billList;
    private JLabel lblDateHeader;

    // ── RIGHT: bill form ──────────────────────────────────────────────────────
    private JTextField txtBuyerName, txtBuyerPhone, txtBuyerAddress;
    private DefaultTableModel itemsModel;
    private JTable itemsTable;
    private JLabel lblGrandTotal;
    private JLabel lblBillTitle;
    private JButton btnSave, btnPrintForm;

    // ─────────────────────────────────────────────────────────────────────────

    public F3OrdersPanel() {
        cardTypes = new CardTypeDAO().getAll();
        setLayout(new BorderLayout());
        setBackground(BG);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSidebar(), buildFormPanel());
        split.setDividerLocation(280);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(BG);

        add(split, BorderLayout.CENTER);
        loadBillList();
        resetForm();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SIDEBAR — list of bills
    // ═══════════════════════════════════════════════════════════════════════

    private JPanel buildSidebar() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(SIDEBAR_BG);
        panel.setBorder(new EmptyBorder(16, 12, 16, 8));

        // ── top: title + date selector ──
        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.setOpaque(false);

        JLabel title = new JLabel("📋  Đơn bán hàng");
        title.setFont(FONT_BOLD);
        title.setForeground(TEXT_MAIN);

        // Date row
        JPanel dateRow = new JPanel(new BorderLayout(6, 0));
        dateRow.setOpaque(false);

        lblDateHeader = new JLabel(selectedDate.format(DATE_FMT));
        lblDateHeader.setFont(FONT_BOLD);
        lblDateHeader.setForeground(ACCENT);

        // Inline editable date field that also supports a picker
        JButton btnPickDate = styledButton("📅", new Color(0xEFF6FF), ACCENT, false);
        btnPickDate.setPreferredSize(new Dimension(32, 28));
        btnPickDate.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnPickDate.addActionListener(e -> showDatePicker());

        // Make the date label also directly editable on double-click
        lblDateHeader.setToolTipText("Double-click để nhập ngày, hoặc nhấn 📅");
        lblDateHeader.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showDatePicker();
            }
        });

        dateRow.add(lblDateHeader, BorderLayout.CENTER);
        dateRow.add(btnPickDate, BorderLayout.EAST);

        top.add(title, BorderLayout.NORTH);
        top.add(dateRow, BorderLayout.CENTER);

        // ── new bill button ──
        JButton btnNew = styledButton("+ Tạo đơn mới", ACCENT, Color.WHITE, true);
        btnNew.addActionListener(e -> {
            editingOrderId = null;
            resetForm();
        });

        // ── bill list ──
        billListModel = new DefaultListModel<>();
        billList = new JList<>(billListModel);
        billList.setFont(FONT_TABLE);
        billList.setBackground(SIDEBAR_BG);
        billList.setSelectionBackground(SELECTED_BILL);
        billList.setFixedCellHeight(64);
        billList.setCellRenderer(new BillCellRenderer());
        billList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadBillIntoForm();
        });

        JScrollPane scroll = new JScrollPane(billList);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1, true));
        scroll.getViewport().setBackground(SIDEBAR_BG);

        panel.add(top, BorderLayout.NORTH);
        panel.add(btnNew, BorderLayout.AFTER_LAST_LINE); // below top temporarily
        panel.add(scroll, BorderLayout.CENTER);

        // re-layout properly
        panel.removeAll();
        JPanel topBlock = new JPanel(new BorderLayout(0, 8));
        topBlock.setOpaque(false);
        topBlock.add(top, BorderLayout.NORTH);
        topBlock.add(Box.createVerticalStrut(8));
        topBlock.add(btnNew, BorderLayout.SOUTH);

        panel.add(topBlock, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FORM PANEL — bill details
    // ═══════════════════════════════════════════════════════════════════════

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(16, 12, 16, 16));

        // ── header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));

        lblBillTitle = new JLabel("Đơn hàng mới");
        lblBillTitle.setFont(FONT_TITLE);
        lblBillTitle.setForeground(TEXT_MAIN);
        header.add(lblBillTitle, BorderLayout.WEST);

        panel.add(header, BorderLayout.NORTH);

        // ── center: buyer info + items table ──
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        center.add(buildBuyerCard());
        center.add(Box.createVerticalStrut(12));
        center.add(buildItemsCard());

        JScrollPane centerScroll = new JScrollPane(center);
        centerScroll.setBorder(null);
        centerScroll.getViewport().setBackground(BG);
        panel.add(centerScroll, BorderLayout.CENTER);

        // ── footer: total + buttons ──
        panel.add(buildFormFooter(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildBuyerCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel lbl = sectionLabel("👤  Thông tin khách hàng (tùy chọn)");
        card.add(lbl, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(1, 3, 12, 0));
        fields.setOpaque(false);

        txtBuyerName    = styledField("Tên khách hàng");
        txtBuyerPhone   = styledField("Số điện thoại");
        txtBuyerAddress = styledField("Địa chỉ");

        fields.add(fieldGroup("Tên", txtBuyerName));
        fields.add(fieldGroup("Điện thoại", txtBuyerPhone));
        fields.add(fieldGroup("Địa chỉ", txtBuyerAddress));

        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildItemsCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));

        JLabel lbl = sectionLabel("🛒  Chi tiết đơn hàng");
        card.add(lbl, BorderLayout.NORTH);

        // Columns: Sản phẩm | Mệnh giá | Số lượng | %CK | Giá CK (đ/cái) | Thành tiền | Xóa
        String[] cols = {"Sản phẩm", "Mệnh giá (đ)", "Số lượng", "%CK", "Giá CK (đ/cái)", "Thành tiền (đ)", ""};
        itemsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == 0 || c == 2 || c == 3 || c == 6; // product, qty, %CK, delete
            }
        };
        itemsModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0) return;
            if (col == 0) onProductSelected(row);       // recalc when product changes
            if (col == 2 || col == 3) recalcRow(row);   // recalc when qty or %CK changes
            refreshGrandTotal();
        });

        itemsTable = new JTable(itemsModel);
        itemsTable.setFont(FONT_TABLE);
        itemsTable.setRowHeight(38);
        itemsTable.setShowVerticalLines(false);
        itemsTable.setShowHorizontalLines(true);
        itemsTable.setGridColor(BORDER_CLR);
        itemsTable.setBackground(CARD_BG);
        itemsTable.setIntercellSpacing(new Dimension(0, 0));
        itemsTable.setSelectionBackground(new Color(0xDBEAFE));

        JTableHeader th = itemsTable.getTableHeader();
        th.setFont(FONT_TH);
        th.setBackground(HEADER_BG);
        th.setForeground(new Color(0x3730A3));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0xC7D2FE)));
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(0, 38));

        // column widths
        int[] widths = {180, 110, 75, 65, 110, 120, 42};
        for (int i = 0; i < widths.length; i++) {
            itemsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        itemsTable.getColumnModel().getColumn(6).setMaxWidth(46);

        // Product combo editor
        JComboBox<CardType> cbProduct = new JComboBox<>(cardTypes.toArray(new CardType[0]));
        cbProduct.setFont(FONT_TABLE);
        itemsTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(cbProduct));

        // Renderers
        itemsTable.setDefaultRenderer(Object.class, new ItemsTableRenderer());
        itemsTable.getColumnModel().getColumn(6).setCellRenderer(new DeleteBtnRenderer());
        itemsTable.getColumnModel().getColumn(6).setCellEditor(new DeleteBtnEditor());

        // Number editors for qty and %CK
        itemsTable.getColumnModel().getColumn(2).setCellEditor(new NumberCellEditor(false));
        itemsTable.getColumnModel().getColumn(3).setCellEditor(new NumberCellEditor(true));

        JScrollPane scroll = new JScrollPane(itemsTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR));
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setPreferredSize(new Dimension(0, 260));

        // Add row button
        JButton btnAddRow = styledButton("+ Thêm dòng", new Color(0xF1F5F9), ACCENT, false);
        btnAddRow.setFont(FONT_SMALL);
        btnAddRow.addActionListener(e -> addEmptyRow());

        JPanel tableArea = new JPanel(new BorderLayout(0, 6));
        tableArea.setOpaque(false);
        tableArea.add(scroll, BorderLayout.CENTER);
        tableArea.add(btnAddRow, BorderLayout.SOUTH);

        card.add(tableArea, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildFormFooter() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(12, 0, 0, 0));

        // Grand total
        lblGrandTotal = new JLabel("TỔNG HOÁ ĐƠN: 0 đ");
        lblGrandTotal.setFont(FONT_TOTAL);
        lblGrandTotal.setForeground(new Color(0x1D4ED8));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        btnPrintForm = styledButton("🖨  In bill", WARNING, Color.WHITE, false);
        btnPrintForm.addActionListener(e -> printBill());

        btnSave = styledButton("💾  Lưu bill", SUCCESS, Color.WHITE, true);
        btnSave.addActionListener(e -> saveBill());

        btnRow.add(btnPrintForm);
        btnRow.add(btnSave);

        p.add(lblGrandTotal, BorderLayout.WEST);
        p.add(btnRow, BorderLayout.EAST);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DATE PICKER
    // ═══════════════════════════════════════════════════════════════════════

    private void showDatePicker() {
        // Popup with a spinner-style date input + optional manual entry
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chọn ngày", true);
        dlg.setLayout(new BorderLayout(12, 12));
        dlg.getRootPane().setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);

        SpinnerNumberModel dayModel   = new SpinnerNumberModel(selectedDate.getDayOfMonth(), 1, 31, 1);
        SpinnerNumberModel monthModel = new SpinnerNumberModel(selectedDate.getMonthValue(), 1, 12, 1);
        SpinnerNumberModel yearModel  = new SpinnerNumberModel(selectedDate.getYear(), 2020, 2100, 1);

        JSpinner spDay   = styledSpinner(dayModel);
        JSpinner spMonth = styledSpinner(monthModel);
        JSpinner spYear  = styledSpinner(yearModel);

        // Or type directly
        JTextField txtManual = styledField(selectedDate.format(DATE_FMT));
        txtManual.setPreferredSize(new Dimension(130, 36));

        gc.gridx = 0; gc.gridy = 0; content.add(label("Ngày:"), gc);
        gc.gridx = 1; content.add(spDay, gc);
        gc.gridx = 2; content.add(label("/"), gc);
        gc.gridx = 3; content.add(spMonth, gc);
        gc.gridx = 4; content.add(label("/"), gc);
        gc.gridx = 5; content.add(spYear, gc);

        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2;
        content.add(label("Hoặc nhập:"), gc);
        gc.gridx = 2; gc.gridwidth = 4;
        content.add(txtManual, gc);

        JButton btnOk = styledButton("Chọn", ACCENT, Color.WHITE, true);
        btnOk.addActionListener(e -> {
            try {
                String manual = txtManual.getText().trim();
                if (!manual.isBlank() && !manual.equals(selectedDate.format(DATE_FMT))) {
                    selectedDate = LocalDate.parse(manual, DATE_FMT);
                } else {
                    int d = (int) spDay.getValue();
                    int m = (int) spMonth.getValue();
                    int y = (int) spYear.getValue();
                    selectedDate = LocalDate.of(y, m, d);
                }
                lblDateHeader.setText(selectedDate.format(DATE_FMT));
                dlg.dispose();
                loadBillList();
                resetForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Ngày không hợp lệ", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = styledButton("Hủy", new Color(0xE2E8F0), TEXT_MUTED, false);
        btnCancel.addActionListener(e -> dlg.dispose());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnCancel);
        btns.add(btnOk);

        dlg.add(content, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BILL LIST LOGIC
    // ═══════════════════════════════════════════════════════════════════════

    private void loadBillList() {
        billListModel.clear();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT o.id, o.buyer_name, o.buyer_phone, o.created_at,
                       COALESCE(SUM(oi.discount_price * oi.quantity), 0) AS total
                FROM orders o
                LEFT JOIN order_items oi ON oi.order_id = o.id
                WHERE o.date = ?
                GROUP BY o.id
                ORDER BY o.created_at DESC
             """)) {
            ps.setString(1, selectedDate.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                billListModel.addElement(new BillEntry(
                        rs.getInt("id"),
                        rs.getString("buyer_name"),
                        rs.getString("buyer_phone"),
                        rs.getLong("total"),
                        rs.getString("created_at")
                ));
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadBillIntoForm() {
        BillEntry selected = billList.getSelectedValue();
        if (selected == null) return;

        editingOrderId = selected.id;
        lblBillTitle.setText("Đơn #" + selected.id);

        try (Connection conn = DBConnection.getConnection()) {
            // Load buyer info
            PreparedStatement ps1 = conn.prepareStatement(
                    "SELECT buyer_name, buyer_phone, buyer_address FROM orders WHERE id=?");
            ps1.setInt(1, selected.id);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                txtBuyerName.setText(nullToEmpty(rs1.getString("buyer_name")));
                txtBuyerPhone.setText(nullToEmpty(rs1.getString("buyer_phone")));
                txtBuyerAddress.setText(nullToEmpty(rs1.getString("buyer_address")));
            }

            // Load items
            itemsModel.setRowCount(0);
            PreparedStatement ps2 = conn.prepareStatement("""
                SELECT oi.quantity, oi.discount_price, oi.ck_percent,
                       ct.name, ct.denomination
                FROM order_items oi
                JOIN card_types ct ON ct.id = oi.card_type_id
                WHERE oi.order_id = ?
                ORDER BY oi.id
            """);
            ps2.setInt(1, selected.id);
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                int qty        = rs2.getInt("quantity");
                int discPrice  = rs2.getInt("discount_price");
                double ckPct   = rs2.getDouble("ck_percent");
                String name    = rs2.getString("name");
                int denom      = rs2.getInt("denomination");
                long total     = (long) qty * discPrice;

                // Find CardType object
                CardType ct = findCardByName(name);
                itemsModel.addRow(new Object[]{
                        ct != null ? ct : name,
                        denom,
                        qty,
                        ckPct,
                        discPrice,
                        total,
                        "✕"
                });
            }
            if (itemsModel.getRowCount() == 0) addEmptyRow();

        } catch (Exception ex) { ex.printStackTrace(); }

        refreshGrandTotal();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FORM LOGIC
    // ═══════════════════════════════════════════════════════════════════════

    private void resetForm() {
        editingOrderId = null;
        lblBillTitle.setText("Đơn hàng mới");
        txtBuyerName.setText("");
        txtBuyerPhone.setText("");
        txtBuyerAddress.setText("");
        itemsModel.setRowCount(0);
        addEmptyRow();
        billList.clearSelection();
        refreshGrandTotal();
    }

    private void addEmptyRow() {
        CardType first = cardTypes.isEmpty() ? null : cardTypes.get(0);
        double ckPct = 0;
        int discPrice = 0, denom = 0;
        if (first != null) {
            denom = first.denomination;
            ckPct = denom > 0 ? Math.round((1.0 - (double)first.defaultDiscount / denom) * 1000.0) / 10.0 : 0;
            discPrice = first.defaultDiscount;
        }
        itemsModel.addRow(new Object[]{first, denom, 1, ckPct, discPrice, discPrice, "✕"});
    }

    private void onProductSelected(int row) {
        Object val = itemsModel.getValueAt(row, 0);
        CardType ct = null;
        if (val instanceof CardType) ct = (CardType) val;
        else if (val != null) ct = findCardByName(val.toString());
        if (ct == null) return;

        int denom = ct.denomination;
        double ckPct = denom > 0
                ? Math.round((1.0 - (double)ct.defaultDiscount / denom) * 1000.0) / 10.0
                : 0;
        int discPrice = ct.defaultDiscount;
        int qty = toInt(itemsModel.getValueAt(row, 2));

        // Temporarily block recursive listener
        itemsModel.setValueAt(denom,       row, 1);
        itemsModel.setValueAt(ckPct,       row, 3);
        itemsModel.setValueAt(discPrice,   row, 4);
        itemsModel.setValueAt((long) qty * discPrice, row, 5);
    }

    private void recalcRow(int row) {
        int denom = toInt(itemsModel.getValueAt(row, 1));
        int qty   = toInt(itemsModel.getValueAt(row, 2));
        Object ckObj = itemsModel.getValueAt(row, 3);
        double ckPct = 0;
        try { ckPct = Double.parseDouble(ckObj != null ? ckObj.toString() : "0"); }
        catch (NumberFormatException ignore) {}

        int discPrice = denom > 0 ? (int) Math.round(denom * (1.0 - ckPct / 100.0)) : 0;
        long total    = (long) qty * discPrice;

        itemsModel.setValueAt(discPrice, row, 4);
        itemsModel.setValueAt(total,     row, 5);
    }

    private void refreshGrandTotal() {
        if (lblGrandTotal == null) return;
        long total = 0;
        for (int i = 0; i < itemsModel.getRowCount(); i++) {
            total += toLong(itemsModel.getValueAt(i, 5));
        }
        lblGrandTotal.setText("TỔNG HOÁ ĐƠN: " + NF.format(total) + " đ");
    }

    private void saveBill() {
        if (itemsTable.isEditing()) itemsTable.getCellEditor().stopCellEditing();

        // Validate: at least one item
        boolean hasItem = false;
        for (int i = 0; i < itemsModel.getRowCount(); i++) {
            int qty = toInt(itemsModel.getValueAt(i, 2));
            if (qty > 0) { hasItem = true; break; }
        }
        if (!hasItem) { showError("Đơn hàng chưa có sản phẩm nào."); return; }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orderId;
                if (editingOrderId != null) {
                    // Update existing
                    PreparedStatement upd = conn.prepareStatement("""
                        UPDATE orders SET buyer_name=?, buyer_phone=?, buyer_address=?
                        WHERE id=?
                    """);
                    upd.setString(1, txtBuyerName.getText().trim());
                    upd.setString(2, txtBuyerPhone.getText().trim());
                    upd.setString(3, txtBuyerAddress.getText().trim());
                    upd.setInt(4, editingOrderId);
                    upd.executeUpdate();

                    // Delete old items
                    PreparedStatement del = conn.prepareStatement(
                            "DELETE FROM order_items WHERE order_id=?");
                    del.setInt(1, editingOrderId);
                    del.executeUpdate();
                    orderId = editingOrderId;
                } else {
                    // Insert new order
                    PreparedStatement ins = conn.prepareStatement("""
                        INSERT INTO orders(date, seller_name, buyer_name, buyer_phone, buyer_address)
                        VALUES(?, '', ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
                    ins.setString(1, selectedDate.toString());
                    ins.setString(2, txtBuyerName.getText().trim());
                    ins.setString(3, txtBuyerPhone.getText().trim());
                    ins.setString(4, txtBuyerAddress.getText().trim());
                    ins.executeUpdate();
                    ResultSet keys = ins.getGeneratedKeys();
                    keys.next();
                    orderId = keys.getInt(1);
                }

                // Insert items
                PreparedStatement insItem = conn.prepareStatement("""
                    INSERT INTO order_items(order_id, card_type_id, quantity, price, discount_price, ck_percent)
                    VALUES(?, ?, ?, ?, ?, ?)
                """);
                for (int i = 0; i < itemsModel.getRowCount(); i++) {
                    int qty = toInt(itemsModel.getValueAt(i, 2));
                    if (qty <= 0) continue;

                    Object prodVal = itemsModel.getValueAt(i, 0);
                    CardType ct = (prodVal instanceof CardType) ? (CardType) prodVal
                            : findCardByName(prodVal != null ? prodVal.toString() : "");
                    if (ct == null) continue;

                    double ckPct = 0;
                    try { ckPct = Double.parseDouble(itemsModel.getValueAt(i, 3).toString()); }
                    catch (Exception ignore) {}
                    int discPrice = toInt(itemsModel.getValueAt(i, 4));

                    insItem.setInt(1, orderId);
                    insItem.setInt(2, ct.id);
                    insItem.setInt(3, qty);
                    insItem.setInt(4, ct.defaultPrice);
                    insItem.setInt(5, discPrice);
                    insItem.setDouble(6, ckPct);
                    insItem.addBatch();
                }
                insItem.executeBatch();
                conn.commit();

                editingOrderId = orderId;
                lblBillTitle.setText("Đơn #" + orderId);

                loadBillList();
                // Re-select the bill
                for (int i = 0; i < billListModel.size(); i++) {
                    if (billListModel.get(i).id == orderId) {
                        billList.setSelectedIndex(i);
                        break;
                    }
                }

                JOptionPane.showMessageDialog(this, "✅ Đã lưu đơn hàng #" + orderId,
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Lỗi khi lưu: " + ex.getMessage());
        }
    }

    private void deleteBill(int listIndex) {
        if (listIndex < 0 || listIndex >= billListModel.size()) return;
        BillEntry bill = billListModel.get(listIndex);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa đơn #" + bill.id + "?", "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE id=?")) {
            ps.setInt(1, bill.id);
            ps.executeUpdate();
        } catch (Exception ex) { ex.printStackTrace(); }

        if (editingOrderId != null && editingOrderId == bill.id) resetForm();
        loadBillList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRINT
    // ═══════════════════════════════════════════════════════════════════════

    private void printBill() {
        if (itemsTable.isEditing()) itemsTable.getCellEditor().stopCellEditing();

        // Build print content
        StringBuilder sb = new StringBuilder();
        sb.append("        ĐẠI LÝ 66 LÝ NAM ĐẾ\n");
        sb.append("      HOÁ ĐƠN BÁN HÀNG\n");
        sb.append("Ngày: ").append(selectedDate.format(DATE_FMT)).append("\n");
        if (editingOrderId != null) sb.append("Đơn #: ").append(editingOrderId).append("\n");
        sb.append("─────────────────────────────\n");
        if (!txtBuyerName.getText().isBlank())
            sb.append("Khách: ").append(txtBuyerName.getText()).append("\n");
        if (!txtBuyerPhone.getText().isBlank())
            sb.append("SĐT:   ").append(txtBuyerPhone.getText()).append("\n");
        if (!txtBuyerAddress.getText().isBlank())
            sb.append("Địa chỉ: ").append(txtBuyerAddress.getText()).append("\n");
        sb.append("─────────────────────────────\n");

        long grand = 0;
        for (int i = 0; i < itemsModel.getRowCount(); i++) {
            int qty = toInt(itemsModel.getValueAt(i, 2));
            if (qty <= 0) continue;
            Object prod = itemsModel.getValueAt(i, 0);
            String name = prod instanceof CardType ? ((CardType)prod).name : prod != null ? prod.toString() : "?";
            int disc  = toInt(itemsModel.getValueAt(i, 4));
            long line = (long) qty * disc;
            grand += line;
            sb.append(String.format("%-18s x%d\n", shorten(name, 18), qty));
            sb.append(String.format("  %s đ/cái = %s đ\n",
                    NF.format(disc), NF.format(line)));
        }
        sb.append("─────────────────────────────\n");
        sb.append(String.format("TỔNG: %s đ\n", NF.format(grand)));
        sb.append("\n     Cảm ơn quý khách!\n");

        final String text = sb.toString();

        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        // A5 approximate
        Paper paper = new Paper();
        double a5w = 5.83 * 72, a5h = 8.27 * 72;
        paper.setSize(a5w, a5h);
        paper.setImageableArea(36, 36, a5w - 72, a5h - 72);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();
            int lineH = fm.getHeight();
            int y = lineH;
            for (String line : text.split("\n")) {
                g2.drawString(line, 0, y);
                y += lineH;
            }
            return Printable.PAGE_EXISTS;
        }, pf);

        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) { showError("Lỗi in: " + ex.getMessage()); }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private CardType findCardByName(String name) {
        for (CardType c : cardTypes) if (c.name.equals(name)) return c;
        return null;
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        try { return Integer.parseInt(val.toString().replace(",","").replace(".","").trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private long toLong(Object val) {
        if (val == null) return 0;
        try { return Long.parseLong(val.toString().replace(",","").replace(".","").trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }

    private String shorten(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // ── UI factory ────────────────────────────────────────────────────────────

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(14, 16, 14, 16)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BOLD);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_MAIN);
        return l;
    }

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(FONT_TABLE);
        f.setPreferredSize(new Dimension(160, 36));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(4, 8, 4, 8)));
        f.setForeground(TEXT_MAIN);
        // placeholder hint
        f.putClientProperty("JTextField.placeholderText", placeholder);
        return f;
    }

    private JPanel fieldGroup(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
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
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        return b;
    }

    private JSpinner styledSpinner(SpinnerNumberModel model) {
        JSpinner sp = new JSpinner(model);
        sp.setFont(FONT_TABLE);
        sp.setPreferredSize(new Dimension(65, 36));
        return sp;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════════════

    private static class BillEntry {
        int id;
        String buyerName, buyerPhone, createdAt;
        long total;
        BillEntry(int id, String buyerName, String buyerPhone, long total, String createdAt) {
            this.id = id;
            this.buyerName  = buyerName  != null ? buyerName  : "";
            this.buyerPhone = buyerPhone != null ? buyerPhone : "";
            this.total      = total;
            this.createdAt  = createdAt != null ? createdAt : "";
        }
        @Override public String toString() { return "Đơn #" + id; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RENDERERS / EDITORS
    // ═══════════════════════════════════════════════════════════════════════

    /** Renders each bill in the sidebar list */
    private class BillCellRenderer extends JPanel implements ListCellRenderer<BillEntry> {
        private final JLabel lblId, lblName, lblTotal;

        BillCellRenderer() {
            setLayout(new BorderLayout(8, 2));
            setBorder(new CompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR),
                    new EmptyBorder(8, 10, 8, 10)
            ));

            lblId    = new JLabel(); lblId.setFont(FONT_BOLD); lblId.setForeground(ACCENT);
            lblName  = new JLabel(); lblName.setFont(FONT_SMALL); lblName.setForeground(TEXT_MUTED);
            lblTotal = new JLabel(); lblTotal.setFont(FONT_BOLD); lblTotal.setForeground(SUCCESS);

            JPanel left = new JPanel(new BorderLayout(0, 2));
            left.setOpaque(false);
            left.add(lblId, BorderLayout.NORTH);
            left.add(lblName, BorderLayout.CENTER);

            add(left, BorderLayout.CENTER);
            add(lblTotal, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends BillEntry> list,
                                                      BillEntry bill, int index, boolean isSelected, boolean cellHasFocus) {
            lblId.setText("Đơn #" + bill.id);
            String nameDisplay = bill.buyerName.isBlank() ? "Khách lẻ" : bill.buyerName;
            if (!bill.buyerPhone.isBlank()) nameDisplay += "  📞 " + bill.buyerPhone;
            lblName.setText(nameDisplay);
            lblTotal.setText(NF.format(bill.total) + " đ");
            setBackground(isSelected ? SELECTED_BILL : SIDEBAR_BG);
            setOpaque(true);
            return this;
        }
    }

    /** Renders table rows with number formatting */
    private class ItemsTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                                                       boolean sel, boolean foc, int row, int col) {
            // Format numbers
            if (val != null) {
                if (col == 1 || col == 4 || col == 5) {
                    try { val = NF.format(Long.parseLong(val.toString())); }
                    catch (NumberFormatException ignore) {}
                }
            }
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setFont(FONT_TABLE);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            if (sel) { setBackground(new Color(0xDBEAFE)); setForeground(TEXT_MAIN); }
            else {
                setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                setForeground(col == 5 ? new Color(0x15803D) : TEXT_MAIN);
            }
            setHorizontalAlignment(col >= 1 && col <= 5 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }

    private class DeleteBtnRenderer extends JButton implements TableCellRenderer {
        DeleteBtnRenderer() {
            setText("✕");
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
        }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
            return this;
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(DANGER);
            g2.fillRoundRect(4, 4, getWidth()-8, getHeight()-8, 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class DeleteBtnEditor extends DefaultCellEditor {
        private final JButton btn;
        private int editRow;
        DeleteBtnEditor() {
            super(new JCheckBox());
            btn = new JButton("✕") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(DANGER);
                    g2.fillRoundRect(4, 4, getWidth()-8, getHeight()-8, 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setForeground(Color.WHITE);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (editRow >= 0 && editRow < itemsModel.getRowCount()) {
                    itemsModel.removeRow(editRow);
                    if (itemsModel.getRowCount() == 0) addEmptyRow();
                    refreshGrandTotal();
                }
            });
        }
        @Override public Component getTableCellEditorComponent(
                JTable t, Object val, boolean sel, int row, int col) {
            editRow = row;
            return btn;
        }
        @Override public Object getCellEditorValue() { return "✕"; }
    }

    private class NumberCellEditor extends DefaultCellEditor {
        private final JTextField tf;
        private final boolean isDecimal;
        NumberCellEditor(boolean isDecimal) {
            super(new JTextField());
            this.isDecimal = isDecimal;
            tf = (JTextField) getComponent();
            tf.setFont(FONT_TABLE);
            tf.setHorizontalAlignment(SwingConstants.RIGHT);
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT, 2, true),
                    new EmptyBorder(2, 6, 2, 6)));
            tf.setBackground(new Color(0xEFF6FF));
            setClickCountToStart(1);
        }
        @Override public Component getTableCellEditorComponent(
                JTable t, Object val, boolean sel, int row, int col) {
            tf.setText(val != null ? val.toString().replace(",","").replace(".","") : "0");
            SwingUtilities.invokeLater(tf::selectAll);
            return tf;
        }
        @Override public Object getCellEditorValue() {
            String s = tf.getText().trim();
            if (isDecimal) {
                try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
            } else {
                try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
            }
        }
    }
}