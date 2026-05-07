package ui.tabs;

import db.DBConnection;
import ui.tabs.model.CardType;
import ui.tabs.dao.CardTypeDAO;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * F4 - Tra cứu tồn kho (ảnh 5 & 6)
 * Mở dialog chọn: Từ ngày, Đến ngày, Mã hàng (trống = tất cả)
 * Kết quả: Tồn đầu | Nhập | Xuất | Tồn cuối (SL + Đơn giá + Thành tiền)
 * Highlight đỏ dòng âm tồn.
 */
public class F4ClosingPanel extends JPanel {

    private static final Color C_BG      = new Color(0xECF0F1);
    private static final Color C_WHITE   = Color.WHITE;
    private static final Color C_ACCENT  = new Color(0x1565C0);
    private static final Color C_GREEN   = new Color(0x2E7D32);
    private static final Color C_RED     = new Color(0xC62828);
    private static final Color C_ORANGE  = new Color(0xE65100);
    private static final Color C_GRID    = new Color(0xB0BEC5);
    private static final Color C_TH_BG   = new Color(0x1565C0);
    private static final Color C_TH_FG   = Color.WHITE;
    private static final Color C_ROW1    = Color.WHITE;
    private static final Color C_ROW2    = new Color(0xE3F2FD);
    private static final Color C_BORDER  = new Color(0x90A4AE);
    private static final Color C_ALERT   = new Color(0xFF5252);  // highlight âm

    private static final Font F_LABEL  = new Font("Arial", Font.PLAIN, 15);
    private static final Font F_BOLD   = new Font("Arial", Font.BOLD, 15);
    private static final Font F_TABLE  = new Font("Arial", Font.PLAIN, 14);
    private static final Font F_TH     = new Font("Arial", Font.BOLD, 13);
    private static final Font F_BTN    = new Font("Arial", Font.BOLD, 14);
    private static final Font F_TOTAL  = new Font("Arial", Font.BOLD, 16);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat NF = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private List<CardType> allCards;
    private Map<String, CardType> codeMap = new LinkedHashMap<>();
    private LocalDate fromDate = LocalDate.now();
    private LocalDate toDate   = LocalDate.now();
    private String filterCode  = "";

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel lblTotalGia;
    private JLabel lblFromTo;

    public F4ClosingPanel() {
        allCards = new CardTypeDAO().getAll();
        for (CardType ct : allCards) {
            String code = (ct.code!=null&&!ct.code.isBlank()) ? ct.code : CardTypeDAO.deriveCode(ct.name);
            ct.code = code; codeMap.put(code.toUpperCase(), ct);
        }

        setLayout(new BorderLayout()); setBackground(C_BG);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        // Auto open dialog on first show
        SwingUtilities.invokeLater(this::showQueryDialog);
    }

    // ── TOOLBAR ───────────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setBackground(C_BG);

        lblFromTo = new JLabel(""); lblFromTo.setFont(F_BOLD); lblFromTo.setForeground(C_ACCENT);

        JButton btnQuery  = mkBtn("🔍 Tra cứu",     C_ACCENT,  Color.WHITE);
        JButton btnPrint  = mkBtn("🖨 In Báo cáo",  C_ORANGE,  Color.WHITE);
        JButton btnClose  = mkBtn("Đóng",            C_BORDER,  Color.WHITE);

        btnQuery.addActionListener(e -> showQueryDialog());
        btnPrint.addActionListener(e -> JOptionPane.showMessageDialog(this,"Chức năng in đang phát triển."));
        btnClose.addActionListener(e -> tableModel.setRowCount(0));

        // Search field (quick code filter)
        JTextField txtQuick = new JTextField(10); styleField(txtQuick);
        txtQuick.putClientProperty("JTextField.placeholderText","Tìm mã...");
        JButton btnGo = mkBtn("Lọc", C_ACCENT, Color.WHITE);
        btnGo.addActionListener(e -> { filterCode=txtQuick.getText().trim(); loadData(); });

        p.add(lblFromTo); p.add(btnQuery); p.add(btnPrint); p.add(btnClose);
        p.add(Box.createHorizontalStrut(20));
        p.add(new JLabel("Mã hàng:"){ {setFont(F_LABEL);} });
        p.add(txtQuick); p.add(btnGo);
        return p;
    }

    // ── TABLE ─────────────────────────────────────────────────────────────────

    private JPanel buildTablePanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(C_WHITE); card.setBorder(BorderFactory.createLineBorder(C_BORDER));

        // Header rows (2-level like ảnh 6)
        String[] cols = {"Mã hàng","Tên hàng","ĐVT","Tồn đầu (SL)","Nhập (SL)","Xuất (SL)","Tồn cuối (SL)","Đơn giá","Thành tiền"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(F_TABLE); table.setRowHeight(24);
        table.setShowGrid(true); table.setGridColor(C_GRID);
        table.setIntercellSpacing(new Dimension(1,1));
        table.setSelectionBackground(new Color(0xFFF9C4));
        table.setSelectionForeground(Color.BLACK);
        table.setDefaultRenderer(Object.class, new InventoryRenderer());

        JTableHeader th = table.getTableHeader();
        th.setFont(F_TH); th.setBackground(C_TH_BG); th.setForeground(C_TH_FG);
        th.setPreferredSize(new Dimension(0, 28)); th.setReorderingAllowed(false);

        int[] widths = {70,200,50,100,80,80,100,90,120};
        for (int i=0;i<widths.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── FOOTER ────────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(0xBBDEFB)); p.setBorder(BorderFactory.createMatteBorder(1,0,0,0,C_BORDER));
        JLabel lbl = new JLabel("  Tổng giá trị tồn kho"); lbl.setFont(F_TOTAL);
        lblTotalGia = new JLabel("0  ", SwingConstants.RIGHT); lblTotalGia.setFont(F_TOTAL); lblTotalGia.setForeground(C_RED);
        lblTotalGia.setBorder(new EmptyBorder(6,0,6,12));
        p.add(lbl, BorderLayout.WEST); p.add(lblTotalGia, BorderLayout.EAST);
        return p;
    }

    // ── QUERY DIALOG (ảnh 5) ─────────────────────────────────────────────────

    private void showQueryDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chọn thời gian tra cứu số liệu", true);
        dlg.setLayout(new BorderLayout(12,12));

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(16,24,8,24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6); gc.anchor = GridBagConstraints.WEST;

        JLabel q = new JLabel("Bạn muốn tra cứu thông tin tồn kho hàng hóa trong\nkhoảng thời gian và Mã hàng nào?");
        q.setFont(new Font("Arial", Font.BOLD, 14)); q.setForeground(C_RED);
        gc.gridx=0;gc.gridy=0;gc.gridwidth=3; content.add(q,gc);
        gc.gridwidth=1;

        JTextField txtFrom = new JTextField(fromDate.format(DATE_FMT), 10); txtFrom.setFont(F_LABEL); txtFrom.setForeground(C_RED); styleField(txtFrom);
        JTextField txtTo   = new JTextField(toDate.format(DATE_FMT),   10); txtTo.setFont(F_LABEL);   txtTo.setForeground(C_RED);   styleField(txtTo);
        JTextField txtCode = new JTextField(filterCode, 12); styleField(txtCode);
        txtCode.setFont(F_LABEL);

        gc.gridx=0;gc.gridy=1; content.add(fLbl("Từ ngày"),gc);
        gc.gridx=1; content.add(txtFrom,gc);
        gc.gridx=0;gc.gridy=2; content.add(fLbl("Đến ngày"),gc);
        gc.gridx=1; content.add(txtTo,gc);
        gc.gridx=0;gc.gridy=3; content.add(fLbl("Mã hàng"),gc);
        gc.gridx=1; content.add(txtCode,gc);
        gc.gridx=0;gc.gridy=4;gc.gridwidth=3;
        JLabel hint = new JLabel("(Không nhập Mã hàng có nghĩa là xem tất cả các mặt hàng)");
        hint.setFont(new Font("Arial",Font.ITALIC,12)); hint.setForeground(Color.GRAY);
        content.add(hint,gc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER,12,8));
        btns.setBackground(Color.WHITE);
        JButton ok  = mkBtn("✔ Đồng ý", C_GREEN, Color.WHITE);
        JButton cancel = mkBtn("✖ Hủy bỏ", C_RED, Color.WHITE);

        ok.addActionListener(e -> {
            try {
                fromDate  = LocalDate.parse(txtFrom.getText().trim(), DATE_FMT);
                toDate    = LocalDate.parse(txtTo.getText().trim(), DATE_FMT);
                filterCode = txtCode.getText().trim().toUpperCase();
                lblFromTo.setText("Từ " + fromDate.format(DATE_FMT) + "  →  " + toDate.format(DATE_FMT));
                dlg.dispose();
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg,"Ngày không hợp lệ! Định dạng: dd/MM/yyyy","Lỗi",JOptionPane.ERROR_MESSAGE);
            }
        });
        cancel.addActionListener(e -> dlg.dispose());

        btns.add(ok); btns.add(cancel);
        dlg.add(content, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.pack(); dlg.setMinimumSize(new Dimension(400,300));
        dlg.setLocationRelativeTo(this); dlg.setResizable(false);
        dlg.setVisible(true);
    }

    // ── DATA ─────────────────────────────────────────────────────────────────

    private void loadData() {
        tableModel.setRowCount(0);

        try (Connection conn = DBConnection.getConnection()) {
            String sql = """
                SELECT ct.id, ct.name, ct.default_discount,
                       COALESCE(SUM(CASE WHEN ie.type='opening'  THEN ie.quantity ELSE 0 END),0) AS opening,
                       COALESCE(SUM(CASE WHEN ie.type='received' THEN ie.quantity ELSE 0 END),0) AS received
                FROM card_types ct
                LEFT JOIN inventory_entries ie
                    ON ie.card_type_id=ct.id AND ie.date BETWEEN ? AND ?
                GROUP BY ct.id ORDER BY ct.id
            """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, fromDate.toString()); ps.setString(2, toDate.toString());
            ResultSet rs = ps.executeQuery();

            // build map id -> [opening, received, defaultDiscount]
            Map<Integer,int[]> inv = new LinkedHashMap<>();
            Map<Integer,String> names = new LinkedHashMap<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                inv.put(id, new int[]{rs.getInt("opening"), rs.getInt("received"), rs.getInt("default_discount")});
                names.put(id, rs.getString("name"));
            }

            // sold from orders
            PreparedStatement psSold = conn.prepareStatement("""
                SELECT oi.card_type_id, SUM(oi.quantity) AS sold
                FROM order_items oi JOIN orders o ON o.id=oi.order_id
                WHERE o.date BETWEEN ? AND ?
                GROUP BY oi.card_type_id
            """);
            psSold.setString(1, fromDate.toString()); psSold.setString(2, toDate.toString());
            ResultSet rsSold = psSold.executeQuery();
            Map<Integer,Integer> soldMap = new HashMap<>();
            while (rsSold.next()) soldMap.put(rsSold.getInt("card_type_id"), rsSold.getInt("sold"));

            long totalValue = 0;
            for (Map.Entry<Integer,int[]> e : inv.entrySet()) {
                int id = e.getKey();
                String name = names.get(id);

                // apply code filter
                CardType ctObj = findById(allCards, id);
                String code = ctObj!=null ? ctObj.code : "";
                if (!filterCode.isEmpty() && !code.equalsIgnoreCase(filterCode) && !name.toLowerCase().contains(filterCode.toLowerCase())) continue;

                int opening  = e.getValue()[0];
                int received = e.getValue()[1];
                int giaCK    = e.getValue()[2];
                int sold     = soldMap.getOrDefault(id, 0);
                int closing  = opening + received - sold;
                long value   = (long) closing * giaCK;
                totalValue  += value;

                tableModel.addRow(new Object[]{code, name, "Thẻ", opening, received, sold, closing, giaCK, value});
            }

            lblTotalGia.setText(NF.format(totalValue));

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private CardType findById(List<CardType> list, int id) { for(CardType ct:list) if(ct.id==id) return ct; return null; }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private JLabel fLbl(String t) {
        JLabel l=new JLabel(t); l.setFont(new Font("Arial",Font.BOLD,14)); return l;
    }

    private JButton mkBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()?bg.darker():bg);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(F_BTN); b.setForeground(fg);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6,14,6,14));
        return b;
    }

    private void styleField(JTextField f) {
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER),new EmptyBorder(3,6,3,6)));
    }

    // ── RENDERER ─────────────────────────────────────────────────────────────

    private class InventoryRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object val,
                                                                 boolean sel, boolean foc, int row, int col) {
            if (val!=null && col>=3) { try{val=NF.format(Long.parseLong(val.toString()));}catch(Exception ignore){} }
            super.getTableCellRendererComponent(t,val,sel,foc,row,col);
            setFont(F_TABLE); setBorder(new EmptyBorder(0,4,0,4));

            // Check if closing (col 6) is negative
            boolean isNeg = false;
            try { isNeg = Integer.parseInt(tableModel.getValueAt(row,6).toString()) < 0; } catch (Exception ignore) {}

            if (sel) {
                setBackground(new Color(0xFFF176)); setForeground(Color.BLACK);
            } else if (isNeg) {
                // Red background for negative rows (like ảnh 6)
                setBackground(new Color(0xFFCDD2)); setForeground(C_RED);
            } else {
                setBackground(row%2==0?C_ROW1:C_ROW2); setForeground(Color.BLACK);
            }

            // Color specific columns
            if (!sel && !isNeg) {
                if (col==3) setForeground(new Color(0xC62828)); // tồn đầu — dark red
                if (col==4) setForeground(new Color(0x1B5E20)); // nhập — green
                if (col==5) setForeground(new Color(0xE65100)); // xuất — orange
                if (col==6) { setFont(new Font("Arial",Font.BOLD,14)); setForeground(col==6&&isNeg?C_RED:new Color(0x1565C0)); } // tồn cuối blue bold
                if (col==8) { setFont(new Font("Arial",Font.BOLD,14)); setForeground(C_RED); }  // thành tiền
            }

            setHorizontalAlignment(col>=3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }
}