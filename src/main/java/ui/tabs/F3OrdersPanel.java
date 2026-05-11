package ui.tabs;

import db.DBConnection;
import ui.tabs.model.CardType;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * F3 - Bán Hàng
 * Layout giống F1/F2: SplitPane trái (danh sách đơn) / phải (form nhập)
 * - Bỏ cột ĐVT
 * - 1 hàng mặc định; chọn sản phẩm → tự sinh hàng mới
 * - Fix bug %CK tăng 10x mỗi lần edit
 * - Nút "In bill" → HTML preview (InvoiceBuilder)
 */
public class F3OrdersPanel extends BasePhieuPanel {

    private Integer editingOrderId = null;

    // ── LEFT (bill list) ──────────────────────────────────────────────────────
    private DefaultListModel<BillEntry> billListModel;
    private JList<BillEntry> billList;
    private JLabel lblDateLeft, lblTongCong;

    // ── RIGHT (bill form) ─────────────────────────────────────────────────────
    private JLabel lblSoPhieu;
    private JTextField txtNgay, txtBuyerName, txtBuyerPhone, txtBuyerAddress;
    private DefaultTableModel itemModel;
    private JTable itemTable;
    private JLabel lblTongThanhTien;

    // Columns: 0=Sản phẩm(CardType obj), 1=Mệnh giá, 2=SL, 3=%CK, 4=Giá CK, 5=Thành tiền, 6=Xóa
    private static final int C_PROD  = 0;
    private static final int C_DENOM = 1;
    private static final int C_QTY   = 2;
    private static final int C_CK    = 3;
    private static final int C_DISC  = 4;
    private static final int C_TOTAL = 5;
    private static final int C_DEL   = 6;

    public F3OrdersPanel() {
        initCards();
        setLayout(new BorderLayout());
        setBackground(C_BG);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeft(), buildRight());
        split.setDividerLocation(320);
        split.setDividerSize(5);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        loadBillList();
        newPhieu();
    }

    // ═══ LEFT — danh sách đơn hàng ════════════════════════════════════════════

    private JPanel buildLeft() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 4));

        // Toolbar date nav
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        toolbar.setBackground(C_BG);
        JButton prev = iconBtn("◀");
        JButton next = iconBtn("▶");
        lblDateLeft = new JLabel(currentDate.format(DATE_FMT));
        lblDateLeft.setFont(F_CODE_RED);
        lblDateLeft.setForeground(C_RED);
        lblDateLeft.setBorder(new EmptyBorder(0, 6, 0, 6));
        lblDateLeft.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblDateLeft.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { pickDate(); }
        });
        prev.addActionListener(e -> shiftDate(-1));
        next.addActionListener(e -> shiftDate(1));
        JButton btnFilter = mkBtn("🔍 Lọc dữ liệu", C_ACCENT, C_WHITE);
        btnFilter.addActionListener(e -> loadBillList());
        toolbar.add(prev); toolbar.add(lblDateLeft); toolbar.add(next); toolbar.add(btnFilter);

        // List header
        JPanel th = new JPanel(new GridLayout(1, 3));
        th.setBackground(C_TH_BG);
        th.setBorder(new EmptyBorder(4, 6, 4, 6));
        for (String s : new String[]{"Số đơn", "Ngày bán", "Tổng tiền"}) {
            JLabel l = new JLabel(s, SwingConstants.CENTER);
            l.setFont(F_TH); l.setForeground(C_TH_FG); th.add(l);
        }

        billListModel = new DefaultListModel<>();
        billList = new JList<>(billListModel);
        billList.setFont(F_LIST);
        billList.setSelectionBackground(C_SEL_BILL);
        billList.setFixedCellHeight(40);
        billList.setCellRenderer(new BillCellRenderer());
        billList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && billList.getSelectedValue() != null)
                loadBillIntoForm(billList.getSelectedValue());
        });

        JScrollPane scroll = new JScrollPane(billList);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));

        JPanel listArea = new JPanel(new BorderLayout());
        listArea.add(th, BorderLayout.NORTH);
        listArea.add(scroll, BorderLayout.CENTER);

        // Footer total
        JPanel foot = new JPanel(new GridLayout(1, 2));
        foot.setBackground(C_TOTAL_BG);
        foot.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
        JLabel lT = new JLabel("Tổng cộng", SwingConstants.LEFT);
        lT.setFont(F_TOTAL); lT.setBorder(new EmptyBorder(6, 8, 6, 0));
        lblTongCong = new JLabel("0", SwingConstants.RIGHT);
        lblTongCong.setFont(F_TOTAL); lblTongCong.setForeground(C_RED);
        lblTongCong.setBorder(new EmptyBorder(6, 0, 6, 8));
        foot.add(lT); foot.add(lblTongCong);

        p.add(toolbar, BorderLayout.NORTH);
        p.add(listArea, BorderLayout.CENTER);
        p.add(foot, BorderLayout.SOUTH);
        return p;
    }

    // ═══ RIGHT — form nhập đơn ════════════════════════════════════════════════

    private JPanel buildRight() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);
        p.setBorder(new EmptyBorder(8, 4, 8, 8));
        p.add(buildRightToolbar(), BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout());
        mid.setBackground(C_WHITE);
        mid.setBorder(BorderFactory.createLineBorder(C_BORDER));
        mid.add(buildFormHeader(), BorderLayout.NORTH);
        mid.add(buildItemTable(), BorderLayout.CENTER);
        p.add(mid, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRightToolbar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.setBackground(C_BG);
        JButton prev2 = iconBtn("◀"); JButton next2 = iconBtn("▶");
        JLabel lDate = new JLabel(currentDate.format(DATE_FMT));
        lDate.setFont(F_CODE_RED); lDate.setForeground(C_RED); lDate.setBorder(new EmptyBorder(0, 6, 0, 6));
        prev2.addActionListener(e -> { shiftDate(-1); lDate.setText(currentDate.format(DATE_FMT)); });
        next2.addActionListener(e -> { shiftDate(1);  lDate.setText(currentDate.format(DATE_FMT)); });
        JButton btnFilter2 = mkBtn("🔍 Lọc dữ liệu", C_ACCENT, C_WHITE);
        btnFilter2.addActionListener(e -> loadBillList());
        p.add(prev2); p.add(lDate); p.add(next2); p.add(btnFilter2);
        p.add(Box.createHorizontalStrut(10));

        JButton btnNew   = mkBtn("Thêm",     C_ACCENT,  C_WHITE);
        JButton btnDel   = mkBtn("Xóa",      C_RED,     C_WHITE);
        JButton btnPrint = mkBtn("In bill",  C_ORANGE,  C_WHITE);
        JButton btnClose = mkBtn("Đóng",     C_BORDER,  C_WHITE);
        btnNew.addActionListener(e -> newPhieu());
        btnDel.addActionListener(e -> deletePhieu());
        btnPrint.addActionListener(e -> showPrintPreview());
        btnClose.addActionListener(e -> newPhieu());
        p.add(btnNew); p.add(btnDel); p.add(btnPrint); p.add(btnClose);
        return p;
    }

    private JPanel buildFormHeader() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        p.setBackground(new Color(0xECEFF1));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        lblSoPhieu = new JLabel("HD00001");
        lblSoPhieu.setFont(F_CODE_RED); lblSoPhieu.setForeground(C_RED);

        txtNgay = new JTextField(currentDate.format(DATE_FMT), 10);
        styleField(txtNgay); txtNgay.setForeground(C_RED);

        txtBuyerName    = new JTextField("", 14); styleField(txtBuyerName);
        txtBuyerPhone   = new JTextField("", 12); styleField(txtBuyerPhone);
        txtBuyerAddress = new JTextField("", 18); styleField(txtBuyerAddress);

        p.add(lbl("Số đơn"));      p.add(lblSoPhieu);
        p.add(lbl("  Ngày bán"));  p.add(txtNgay);
        p.add(lbl("  Khách hàng")); p.add(txtBuyerName);
        p.add(lbl("  SĐT"));       p.add(txtBuyerPhone);
        p.add(lbl("  Địa chỉ"));   p.add(txtBuyerAddress);
        return p;
    }

    private JPanel buildItemTable() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_WHITE);

        // Cols: Sản phẩm | Mệnh giá | SL | %CK | Giá CK | Thành tiền | [X]
        String[] cols = {"Sản phẩm", "Mệnh giá", "SL", "%CK", "Giá CK", "Thành tiền", ""};
        itemModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == C_PROD || c == C_QTY || c == C_CK || c == C_DEL;
            }
        };
        itemModel.addTableModelListener(e -> {
            int row = e.getFirstRow(), col = e.getColumn();
            if (row < 0) return;
            if (col == C_PROD) { onProductSelected(row); maybeAddRow(row); }
            if (col == C_QTY || col == C_CK) recalcRow(row);
        });
        addEmptyRow();

        itemTable = new JTable(itemModel);
        styleTable(itemTable);
        itemTable.setRowHeight(28);

        // Renderers
        itemTable.setDefaultRenderer(Object.class, new F3Renderer());
        itemTable.getColumnModel().getColumn(C_DEL).setCellRenderer(new DelBtnRenderer());
        itemTable.getColumnModel().getColumn(C_DEL).setCellEditor(new DelBtnEditor());

        // Editors
        // Product: combobox
        JComboBox<CardType> cb = new JComboBox<>(allCards.toArray(new CardType[0]));
        cb.setFont(F_TABLE);
        itemTable.getColumnModel().getColumn(C_PROD).setCellEditor(new DefaultCellEditor(cb));

        // SL: integer editor (strip commas only, keep decimals — though int is fine)
        itemTable.getColumnModel().getColumn(C_QTY).setCellEditor(new IntEditor());

        // %CK: decimal editor — FIXED: only strip %, never strip "."
        itemTable.getColumnModel().getColumn(C_CK).setCellEditor(new CKEditor());

        // Read-only: mệnh giá, giá CK, thành tiền
        for (int c : new int[]{C_DENOM, C_DISC, C_TOTAL})
            itemTable.getColumnModel().getColumn(c).setCellEditor(new ReadOnlyEditor());

        int[] w = {190, 90, 55, 60, 100, 120, 36};
        for (int i = 0; i < w.length; i++) itemTable.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        itemTable.getColumnModel().getColumn(C_DEL).setMaxWidth(38);

        JScrollPane scroll = new JScrollPane(itemTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        p.add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel foot = new JPanel(new BorderLayout());
        foot.setBackground(C_WHITE);
        foot.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_GRID));
        JLabel lNguoi = new JLabel("  Người lập phiếu:  " + InvoiceBuilder.COMPANY_FULL);
        lNguoi.setFont(F_LABEL); lNguoi.setForeground(C_GREEN);
        lblTongThanhTien = new JLabel("0  ", SwingConstants.RIGHT);
        lblTongThanhTien.setFont(F_TOTAL); lblTongThanhTien.setForeground(C_TT_RED);
        JPanel rf = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4)); rf.setOpaque(false);
        rf.add(new JLabel("Tổng thành tiền   ") {{ setFont(F_BOLD); }});
        rf.add(lblTongThanhTien);
        JButton btnSave = mkBtn("💾 Lưu đơn", C_GREEN, C_WHITE);
        btnSave.addActionListener(e -> saveBill());
        rf.add(btnSave);
        foot.add(lNguoi, BorderLayout.WEST);
        foot.add(rf, BorderLayout.EAST);
        p.add(foot, BorderLayout.SOUTH);
        return p;
    }

    // ═══ AUTO-GROW ════════════════════════════════════════════════════════════

    private void maybeAddRow(int row) {
        Object prod = itemModel.getValueAt(row, C_PROD);
        boolean has = (prod instanceof CardType);
        if (has && row == itemModel.getRowCount() - 1) addEmptyRow();
    }

    // ═══ LOGIC ═══════════════════════════════════════════════════════════════

    private void addEmptyRow() {
        // Default to first card type so combo shows something sensible
        CardType first = allCards.isEmpty() ? null : allCards.get(0);
        int denom = 0; double ckPct = 0; int disc = 0;
        if (first != null) {
            denom = first.denomination;
            ckPct = denom > 0 ? Math.round((1.0 - (double) first.defaultDiscount / denom) * 1000.0) / 10.0 : 0;
            disc  = first.defaultDiscount;
        }
        itemModel.addRow(new Object[]{first, denom, 1, ckPct, disc, (long) disc, "✕"});
    }

    private void onProductSelected(int row) {
        Object val = itemModel.getValueAt(row, C_PROD);
        CardType ct = val instanceof CardType ? (CardType) val : null;
        if (ct == null) return;
        int denom   = ct.denomination;
        double ckPct = denom > 0
                ? Math.round((1.0 - (double) ct.defaultDiscount / denom) * 1000.0) / 10.0 : 0;
        int disc    = ct.defaultDiscount;
        int qty     = toInt(itemModel.getValueAt(row, C_QTY));
        if (qty == 0) qty = 1;
        final int fq = qty;
        suppress(itemModel, () -> {
            itemModel.setValueAt(denom,        row, C_DENOM);
            itemModel.setValueAt(ckPct,        row, C_CK);
            itemModel.setValueAt(disc,         row, C_DISC);
            itemModel.setValueAt(fq,           row, C_QTY);
            itemModel.setValueAt((long)fq*disc, row, C_TOTAL);
        });
        refreshTotals();
    }

    private void recalcRow(int row) {
        int denom = toInt(itemModel.getValueAt(row, C_DENOM));
        int qty   = toInt(itemModel.getValueAt(row, C_QTY));
        double ck = toDbl(itemModel.getValueAt(row, C_CK));
        int disc  = denom > 0 ? (int) Math.round(denom * (1.0 - ck / 100.0)) : 0;
        suppress(itemModel, () -> {
            itemModel.setValueAt(disc,         row, C_DISC);
            itemModel.setValueAt((long)qty*disc, row, C_TOTAL);
        });
        refreshTotals();
    }

    private void refreshTotals() {
        long t = 0;
        for (int i = 0; i < itemModel.getRowCount(); i++) t += toLong(itemModel.getValueAt(i, C_TOTAL));
        if (lblTongThanhTien != null) lblTongThanhTien.setText(NF.format(t));
    }

    private void newPhieu() {
        editingOrderId = null;
        if (lblSoPhieu   != null) lblSoPhieu.setText("HD" + String.format("%05d", System.currentTimeMillis() % 99999));
        if (txtNgay      != null) txtNgay.setText(currentDate.format(DATE_FMT));
        if (txtBuyerName != null) txtBuyerName.setText("");
        if (txtBuyerPhone!= null) txtBuyerPhone.setText("");
        if (txtBuyerAddress!=null)txtBuyerAddress.setText("");
        if (itemModel    != null) { itemModel.setRowCount(0); addEmptyRow(); }
        if (billList     != null) billList.clearSelection();
        refreshTotals();
    }

    private void loadBillList() {
        if (listModelNull()) return;
        billListModel.clear(); long total = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT o.id, o.buyer_name, o.buyer_phone,
                       COALESCE(SUM(oi.discount_price*oi.quantity),0) AS total
                FROM orders o
                LEFT JOIN order_items oi ON oi.order_id=o.id
                WHERE o.date=? GROUP BY o.id ORDER BY o.id DESC""")) {
            ps.setString(1, currentDate.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long t = rs.getLong("total"); total += t;
                billListModel.addElement(new BillEntry(
                        rs.getInt("id"),
                        rs.getString("buyer_name"),
                        rs.getString("buyer_phone"),
                        t));
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        if (lblTongCong != null) lblTongCong.setText(NF.format(total));
        if (lblDateLeft != null) lblDateLeft.setText(currentDate.format(DATE_FMT));
    }

    private boolean listModelNull() { return billListModel == null; }

    private void loadBillIntoForm(BillEntry entry) {
        editingOrderId = entry.id;
        lblSoPhieu.setText("HD" + String.format("%05d", entry.id));
        txtNgay.setText(currentDate.format(DATE_FMT));
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps1 = conn.prepareStatement(
                    "SELECT buyer_name,buyer_phone,buyer_address FROM orders WHERE id=?");
            ps1.setInt(1, entry.id); ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                txtBuyerName.setText(nne(rs1.getString("buyer_name")));
                txtBuyerPhone.setText(nne(rs1.getString("buyer_phone")));
                txtBuyerAddress.setText(nne(rs1.getString("buyer_address")));
            }
            itemModel.setRowCount(0);
            PreparedStatement ps2 = conn.prepareStatement("""
                SELECT oi.quantity,oi.discount_price,oi.ck_percent,ct.name,ct.denomination,ct.id as ct_id
                FROM order_items oi JOIN card_types ct ON ct.id=oi.card_type_id
                WHERE oi.order_id=? ORDER BY oi.id""");
            ps2.setInt(1, entry.id); ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                int qty   = rs2.getInt("quantity");
                int disc  = rs2.getInt("discount_price");
                double ck = rs2.getDouble("ck_percent");
                int denom = rs2.getInt("denomination");
                CardType ct2 = findById(rs2.getInt("ct_id"));
                itemModel.addRow(new Object[]{
                        ct2 != null ? ct2 : rs2.getString("name"),
                        denom, qty, ck, disc, (long) qty * disc, "✕"
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        addEmptyRow();
        refreshTotals();
    }

    private void saveBill() {
        if (itemTable.isEditing()) itemTable.getCellEditor().stopCellEditing();
        boolean hasItem = false;
        for (int i = 0; i < itemModel.getRowCount(); i++)
            if (toInt(itemModel.getValueAt(i, C_QTY)) > 0) { hasItem = true; break; }
        if (!hasItem) { showError("Đơn hàng chưa có sản phẩm nào."); return; }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orderId;
                if (editingOrderId != null) {
                    PreparedStatement upd = conn.prepareStatement(
                            "UPDATE orders SET buyer_name=?,buyer_phone=?,buyer_address=? WHERE id=?");
                    upd.setString(1, txtBuyerName.getText().trim());
                    upd.setString(2, txtBuyerPhone.getText().trim());
                    upd.setString(3, txtBuyerAddress.getText().trim());
                    upd.setInt(4, editingOrderId); upd.executeUpdate();
                    conn.prepareStatement("DELETE FROM order_items WHERE order_id=" + editingOrderId).executeUpdate();
                    orderId = editingOrderId;
                } else {
                    PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO orders(date,seller_name,buyer_name,buyer_phone,buyer_address) VALUES(?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ins.setString(1, currentDate.toString());
                    ins.setString(2, InvoiceBuilder.COMPANY_FULL);
                    ins.setString(3, txtBuyerName.getText().trim());
                    ins.setString(4, txtBuyerPhone.getText().trim());
                    ins.setString(5, txtBuyerAddress.getText().trim());
                    ins.executeUpdate();
                    ResultSet keys = ins.getGeneratedKeys(); keys.next(); orderId = keys.getInt(1);
                }
                PreparedStatement insItem = conn.prepareStatement(
                        "INSERT INTO order_items(order_id,card_type_id,quantity,price,discount_price,ck_percent) VALUES(?,?,?,?,?,?)");
                for (int i = 0; i < itemModel.getRowCount(); i++) {
                    int qty = toInt(itemModel.getValueAt(i, C_QTY)); if (qty <= 0) continue;
                    Object pv = itemModel.getValueAt(i, C_PROD);
                    CardType ct = pv instanceof CardType ? (CardType) pv : null;
                    if (ct == null) continue;
                    double ck  = toDbl(itemModel.getValueAt(i, C_CK));
                    int disc   = toInt(itemModel.getValueAt(i, C_DISC));
                    insItem.setInt(1, orderId); insItem.setInt(2, ct.id); insItem.setInt(3, qty);
                    insItem.setInt(4, ct.defaultPrice); insItem.setInt(5, disc); insItem.setDouble(6, ck);
                    insItem.addBatch();
                }
                insItem.executeBatch(); conn.commit();
                editingOrderId = orderId;
                lblSoPhieu.setText("HD" + String.format("%05d", orderId));
                loadBillList();
                // Re-select
                for (int i = 0; i < billListModel.size(); i++) {
                    if (billListModel.get(i).id == orderId) { billList.setSelectedIndex(i); break; }
                }
                JOptionPane.showMessageDialog(this, "✅ Đã lưu đơn hàng #" + orderId, "OK", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) { conn.rollback(); throw ex; }
        } catch (Exception ex) { ex.printStackTrace(); showError("Lỗi khi lưu: " + ex.getMessage()); }
    }

    private void deletePhieu() {
        BillEntry sel = billList.getSelectedValue();
        if (sel == null) { showError("Chọn đơn cần xóa!"); return; }
        if (JOptionPane.showConfirmDialog(this, "Xóa đơn hàng #" + sel.id + "?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection conn = DBConnection.getConnection()) {
            conn.prepareStatement("DELETE FROM orders WHERE id=" + sel.id).executeUpdate();
        } catch (Exception ex) { ex.printStackTrace(); }
        loadBillList(); newPhieu();
    }

    private void pickDate() {
        String in = JOptionPane.showInputDialog(this, "Nhập ngày (dd/MM/yyyy):", currentDate.format(DATE_FMT));
        if (in == null || in.isBlank()) return;
        try { currentDate = java.time.LocalDate.parse(in.trim(), DATE_FMT); loadBillList(); }
        catch (Exception ex) { showError("Ngày không hợp lệ!"); }
    }

    private void shiftDate(int days) {
        currentDate = currentDate.plusDays(days);
        if (lblDateLeft != null) lblDateLeft.setText(currentDate.format(DATE_FMT));
        if (txtNgay     != null) txtNgay.setText(currentDate.format(DATE_FMT));
        loadBillList();
    }

    // ═══ PRINT PREVIEW ════════════════════════════════════════════════════════

    private void showPrintPreview() {
        if (itemTable.isEditing()) itemTable.getCellEditor().stopCellEditing();
        String dateStr    = selectedDate().format(DATE_FMT);
        String buyerName  = txtBuyerName.getText().trim();
        String buyerPhone = txtBuyerPhone.getText().trim();
        String buyerAddr  = txtBuyerAddress.getText().trim();

        List<InvoiceBuilder.SaleItem> items = new ArrayList<>();
        for (int i = 0; i < itemModel.getRowCount(); i++) {
            int qty = toInt(itemModel.getValueAt(i, C_QTY)); if (qty <= 0) continue;
            Object pv = itemModel.getValueAt(i, C_PROD);
            if (!(pv instanceof CardType)) continue;
            CardType ct = (CardType) pv;
            InvoiceBuilder.SaleItem it = new InvoiceBuilder.SaleItem();
            it.name         = ct.name;
            it.denomination = toInt(itemModel.getValueAt(i, C_DENOM));
            it.quantity     = qty;
            it.discountPrice= toInt(itemModel.getValueAt(i, C_DISC));
            it.ckPercent    = toDbl(itemModel.getValueAt(i, C_CK));
            it.lineTotal    = toLong(itemModel.getValueAt(i, C_TOTAL));
            items.add(it);
        }
        String html = InvoiceBuilder.buildSaleInvoice(
                editingOrderId, dateStr, buyerName, buyerPhone, buyerAddr, items);
        PrintPreviewDialog.show(this,
                "Hóa đơn bán hàng" + (editingOrderId != null ? " #" + editingOrderId : ""), html);
    }

    private LocalDate selectedDate() { return currentDate; }

    // ═══ HELPERS ═════════════════════════════════════════════════════════════

    private String nne(String s) { return s == null ? "" : s; }

    // ═══ DATA CLASSES ════════════════════════════════════════════════════════

    private static class BillEntry {
        int id; String buyerName, buyerPhone; long total;
        BillEntry(int id, String bn, String bp, long t) {
            this.id = id;
            buyerName  = bn != null ? bn : "";
            buyerPhone = bp != null ? bp : "";
            total = t;
        }
    }

    // ═══ RENDERERS / EDITORS ═════════════════════════════════════════════════

    /** Main table renderer — formats numbers, colours total column */
    private class F3Renderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            // Format numbers for display
            if (val != null) {
                if (col == C_DENOM || col == C_DISC || col == C_TOTAL) {
                    try { val = NF.format(Long.parseLong(val.toString())); } catch (Exception ignore) {}
                }
                if (col == C_CK) {
                    try { val = toDbl(val) + "%"; } catch (Exception ignore) {}
                }
                if (col == C_PROD && val instanceof CardType) {
                    val = ((CardType) val).name;
                }
            }
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setFont(F_TABLE);
            setBorder(new EmptyBorder(0, 4, 0, 4));
            if (sel) {
                setBackground(C_YELLOW_H); setForeground(Color.BLACK);
            } else {
                setBackground(row % 2 == 0 ? C_ROW1 : C_ROW2);
                setForeground(col == C_TOTAL ? C_TT_RED : Color.BLACK);
            }
            setHorizontalAlignment(col >= C_DENOM && col <= C_TOTAL ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (col == C_CK) setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }

    /** Small red ✕ button renderer */
    private class DelBtnRenderer extends JButton implements TableCellRenderer {
        DelBtnRenderer() {
            setText("✕"); setFont(new Font("Arial", Font.BOLD, 11));
            setForeground(Color.WHITE); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
        }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            setBackground(C_RED); return this;
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xC62828));
            g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 6, 6);
            g2.dispose(); super.paintComponent(g);
        }
    }

    /** ✕ button editor — removes the row */
    private class DelBtnEditor extends DefaultCellEditor {
        private final JButton btn; private int editRow;
        DelBtnEditor() {
            super(new JCheckBox());
            btn = new JButton("✕") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xC62828));
                    g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 6, 6);
                    g2.dispose(); super.paintComponent(g);
                }
            };
            btn.setFont(new Font("Arial", Font.BOLD, 11));
            btn.setForeground(Color.WHITE);
            btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (editRow >= 0 && editRow < itemModel.getRowCount()) {
                    itemModel.removeRow(editRow);
                    if (itemModel.getRowCount() == 0) addEmptyRow();
                    refreshTotals();
                }
            });
        }
        @Override public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int row, int col) {
            editRow = row; return btn;
        }
        @Override public Object getCellEditorValue() { return "✕"; }
    }

    /** Bill list cell renderer */
    private class BillCellRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(
                JList<?> list, Object val, int idx, boolean sel, boolean foc) {
            BillEntry e = (BillEntry) val;
            JPanel p = new JPanel(new GridLayout(1, 3));
            p.setOpaque(true);
            p.setBackground(sel ? C_SEL_BILL : (idx % 2 == 0 ? C_ROW1 : C_ROW2));
            p.setBorder(new EmptyBorder(4, 4, 4, 4));

            String nameDisplay = e.buyerName.isBlank() ? "Khách lẻ" : e.buyerName;
            if (!e.buyerPhone.isBlank()) nameDisplay += " - " + e.buyerPhone;

            JLabel l1 = new JLabel("HD" + String.format("%05d", e.id)); l1.setFont(F_LIST); l1.setForeground(C_ACCENT);
            JLabel l2 = new JLabel(nameDisplay, SwingConstants.CENTER); l2.setFont(F_LIST);
            JLabel l3 = new JLabel(NF.format(e.total), SwingConstants.RIGHT);
            l3.setFont(new Font("Arial", Font.BOLD, 13)); l3.setForeground(C_TT_RED);
            p.add(l1); p.add(l2); p.add(l3);
            return p;
        }
    }
}