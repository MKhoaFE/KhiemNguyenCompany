package ui.tabs;

import db.DBConnection;
import ui.tabs.model.CardType;
import ui.tabs.dao.CardTypeDAO;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.SwingUtilities;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * F6 - Bảng Giá
 * Grouped: Viettel | Mobifone | Vinaphone | Vietnammobile | Cà phê | Khác
 * Chỉ %CK được nhập — Giá CK tự tính.
 * Arabica / Robusta: cột Mệnh giá cũng nhập được (giá theo mùa).
 */
public class F6PricePanel extends JPanel {

    private static final Color C_BG     = new Color(0xECF0F1);
    private static final Color C_WHITE  = Color.WHITE;
    private static final Color C_ACCENT = new Color(0x1565C0);
    private static final Color C_GREEN  = new Color(0x2E7D32);
    private static final Color C_RED    = new Color(0xC62828);
    private static final Color C_ORANGE = new Color(0xE65100);
    private static final Color C_GRID   = new Color(0xB0BEC5);
    private static final Color C_TH_BG  = new Color(0x37474F);
    private static final Color C_TH_FG  = Color.WHITE;
    private static final Color C_ROW1   = Color.WHITE;
    private static final Color C_ROW2   = new Color(0xE3F2FD);
    private static final Color C_EDIT   = new Color(0xFFFDE7);
    private static final Color C_CHANGED= new Color(0xFFE082);
    private static final Color C_BORDER = new Color(0x90A4AE);

    private static final Font F_LABEL  = new Font("Arial", Font.PLAIN, 15);
    private static final Font F_BOLD   = new Font("Arial", Font.BOLD, 15);
    private static final Font F_TABLE  = new Font("Arial", Font.PLAIN, 14);
    private static final Font F_TH     = new Font("Arial", Font.BOLD, 13);
    private static final Font F_BTN    = new Font("Arial", Font.BOLD, 14);
    private static final Font F_GROUP  = new Font("Arial", Font.BOLD, 14);
    private static final Font F_TOTAL  = new Font("Arial", Font.BOLD, 15);

    private static final NumberFormat NF = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // Brand definition: [groupName, ...keywords in lowercase]
    private static final String[][] BRANDS = {
            {"Viettel",        "viettel"},
            {"Mobifone",       "mobifone"},
            {"Vinaphone",      "vinaphone"},
            {"Vietnammobile",  "vietnammobile", "vietnam mobile"},
            {"Cà phê",         "arabica", "robusta", "cà phê", "cafe"},
    };
    private static final Color[] BRAND_COLORS = {
            new Color(0xEE0033),
            new Color(0xE30613),
            new Color(0x0066CC),
            new Color(0xFF6600),
            new Color(0x5D4037),
    };
    private static final Set<String> EDITABLE_DENOM = new HashSet<>(Arrays.asList("arabica","robusta"));

    private List<CardType> allCards;
    private final List<GroupBlock> groups = new ArrayList<>();
    private JPanel groupsPanel;
    private JScrollPane mainScroll;
    private JLabel lblChanged;
    private JTextField txtSearch;
    private int totalChanged = 0;

    public F6PricePanel() {
        allCards = new CardTypeDAO().getAll();
        setLayout(new BorderLayout(0,0));
        setBackground(C_BG);
        setBorder(new EmptyBorder(10,14,10,14));

        add(buildHeader(), BorderLayout.NORTH);

        groupsPanel = new JPanel();
        groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
        groupsPanel.setBackground(C_BG);

        mainScroll = new JScrollPane(groupsPanel);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        mainScroll.getViewport().setBackground(C_BG);
        add(mainScroll, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
        buildAllGroups(allCards);
    }

    // ── HEADER ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(8,0));
        p.setBackground(C_BG); p.setBorder(new EmptyBorder(0,0,10,0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); left.setOpaque(false);
        JLabel title = new JLabel("💰  Bảng Giá"); title.setFont(new Font("Arial",Font.BOLD,18));
        JLabel sub   = new JLabel("— chỉnh sửa %CK, Giá CK tự tính"); sub.setFont(new Font("Arial",Font.ITALIC,13)); sub.setForeground(Color.GRAY);
        left.add(title); left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0)); right.setOpaque(false);
        txtSearch = new JTextField(16); txtSearch.setFont(F_TABLE);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER),new EmptyBorder(4,8,4,8)));
        txtSearch.setForeground(Color.GRAY); txtSearch.setText("Tìm theo tên...");
        txtSearch.addFocusListener(new FocusAdapter(){
            @Override public void focusGained(FocusEvent e){if(txtSearch.getText().equals("Tìm theo tên...")){txtSearch.setText("");txtSearch.setForeground(Color.BLACK);}}
            @Override public void focusLost(FocusEvent e){if(txtSearch.getText().isBlank()){txtSearch.setForeground(Color.GRAY);txtSearch.setText("Tìm theo tên...");}}
        });
        txtSearch.getDocument().addDocumentListener(new DocumentListener(){
            public void changedUpdate(DocumentEvent e){applySearch();}
            public void removeUpdate(DocumentEvent e){applySearch();}
            public void insertUpdate(DocumentEvent e){applySearch();}
        });
        right.add(new JLabel("🔍"){{setFont(new Font("Segoe UI Emoji",Font.PLAIN,16));}}); right.add(txtSearch);

        p.add(left, BorderLayout.WEST); p.add(right, BorderLayout.EAST);
        return p;
    }

    // ── FOOTER ────────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setBorder(new EmptyBorder(8,0,0,0));
        lblChanged = new JLabel("Chưa có thay đổi"); lblChanged.setFont(F_LABEL); lblChanged.setForeground(Color.GRAY);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actions.setOpaque(false);
        JButton btnReset = mkBtn("↺ Hoàn tác", C_ORANGE, C_WHITE);
        JButton btnSave  = mkBtn("💾 Lưu tất cả", C_GREEN, C_WHITE);
        btnReset.addActionListener(e -> resetAll());
        btnSave.addActionListener(e  -> saveAll());
        actions.add(btnReset); actions.add(btnSave);
        p.add(lblChanged, BorderLayout.WEST); p.add(actions, BorderLayout.EAST);
        return p;
    }

    // ── GROUPS ────────────────────────────────────────────────────────────────

    private void buildAllGroups(List<CardType> source) {
        groupsPanel.removeAll(); groups.clear();
        List<List<CardType>> buckets = new ArrayList<>();
        for (int i=0;i<BRANDS.length;i++) buckets.add(new ArrayList<>());
        List<CardType> others = new ArrayList<>();

        outer: for (CardType ct : source) {
            String nl = ct.name.toLowerCase();
            for (int g=0;g<BRANDS.length;g++) {
                for (int k=1;k<BRANDS[g].length;k++) {
                    if (nl.contains(BRANDS[g][k])) { buckets.get(g).add(ct); continue outer; }
                }
            }
            others.add(ct);
        }
        for (int g=0;g<BRANDS.length;g++) {
            if (!buckets.get(g).isEmpty()) {
                GroupBlock b = new GroupBlock(BRANDS[g][0], buckets.get(g), BRAND_COLORS[g%BRAND_COLORS.length]);
                groups.add(b); groupsPanel.add(b); groupsPanel.add(Box.createVerticalStrut(8));
            }
        }
        if (!others.isEmpty()) {
            GroupBlock b = new GroupBlock("Khác", others, new Color(0x546E7A));
            groups.add(b); groupsPanel.add(b); groupsPanel.add(Box.createVerticalStrut(8));
        }
        groupsPanel.revalidate(); groupsPanel.repaint();
    }

    private void applySearch() {
        String q = txtSearch.getText().trim().toLowerCase();
        if (q.equals("tìm theo tên...")) q = "";
        final String fq = q;
        List<CardType> filtered = new ArrayList<>();
        for (CardType ct : allCards) { if (fq.isEmpty()||ct.name.toLowerCase().contains(fq)) filtered.add(ct); }
        buildAllGroups(filtered);
        recountChanged();
    }

    private void recountChanged() {
        totalChanged=0; for(GroupBlock g:groups) for(boolean b:g.changed) if(b) totalChanged++;
        updateChangedLabel();
    }

    private void updateChangedLabel() {
        if (lblChanged==null) return;
        if (totalChanged==0) { lblChanged.setText("Chưa có thay đổi"); lblChanged.setForeground(Color.GRAY); }
        else { lblChanged.setText("⚠  "+totalChanged+" mặt hàng đã thay đổi (chưa lưu)"); lblChanged.setForeground(C_ORANGE); }
    }

    private void saveAll() {
        for (GroupBlock g:groups) if (g.table.isEditing()) g.table.getCellEditor().stopCellEditing();
        recountChanged(); if (totalChanged==0) { JOptionPane.showMessageDialog(this,"Không có thay đổi."); return; }
        try(Connection conn=DBConnection.getConnection()){
            PreparedStatement ps=conn.prepareStatement("UPDATE card_types SET denomination=?,default_price=?,default_discount=? WHERE id=?");
            int saved=0;
            for (GroupBlock g:groups) {
                for (int i=0;i<g.types.size();i++) {
                    if (!g.changed[i]) continue;
                    CardType ct=g.types.get(i);
                    int denom=toInt(g.model.getValueAt(i,1));
                    double ck=toDbl(g.model.getValueAt(i,2));
                    int giaCK=(int)Math.round(denom*(1.0-ck/100.0));
                    ps.setInt(1,denom);ps.setInt(2,denom);ps.setInt(3,giaCK);ps.setInt(4,ct.id);ps.addBatch();
                    ct.denomination=denom;ct.defaultPrice=denom;ct.defaultDiscount=giaCK;saved++;
                }
                Arrays.fill(g.changed,false);
            }
            ps.executeBatch(); totalChanged=0; updateChangedLabel();
            for (GroupBlock g:groups) g.table.repaint();
            JOptionPane.showMessageDialog(this,"✅ Đã cập nhật "+saved+" mặt hàng!","OK",JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){ex.printStackTrace();JOptionPane.showMessageDialog(this,"Lỗi lưu: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);}
    }

    private void resetAll() {
        if (JOptionPane.showConfirmDialog(this,"Hoàn tác tất cả thay đổi?","Xác nhận",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        allCards=new CardTypeDAO().getAll(); buildAllGroups(allCards); totalChanged=0; updateChangedLabel();
    }

    private int toInt(Object v){if(v==null)return 0;try{return Integer.parseInt(v.toString().replace(",","").replace(".","").trim());}catch(NumberFormatException e){return 0;}}
    private double toDbl(Object v){if(v==null)return 0;try{return Double.parseDouble(v.toString().replace("%","").trim());}catch(NumberFormatException e){return 0;}}

    private JButton mkBtn(String t, Color bg, Color fg) {
        JButton b=new JButton(t){@Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(getModel().isRollover()?bg.darker():bg);g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);g2.dispose();super.paintComponent(g);}};
        b.setFont(F_BTN);b.setForeground(fg);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setBorder(new EmptyBorder(6,14,6,14));return b;
    }

    // ═══ GroupBlock ═══════════════════════════════════════════════════════════

    private class GroupBlock extends JPanel {
        final String brandName; final List<CardType> types; final Color brandColor;
        DefaultTableModel model; JTable table; boolean[] changed; boolean collapsed=false;
        JPanel tablePanel; JButton btnToggle;

        GroupBlock(String name, List<CardType> types, Color color) {
            this.brandName=name; this.types=new ArrayList<>(types); this.brandColor=color; this.changed=new boolean[types.size()];
            setLayout(new BorderLayout()); setBackground(C_WHITE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER,1,true),BorderFactory.createEmptyBorder(0,0,0,0)));
            setAlignmentX(Component.LEFT_ALIGNMENT); setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
            add(buildGroupHeader(), BorderLayout.NORTH);
            tablePanel=buildTablePanel(); add(tablePanel,BorderLayout.CENTER);
        }

        private JPanel buildGroupHeader() {
            Color bgH = new Color(Math.min(255,brandColor.getRed()+180),Math.min(255,brandColor.getGreen()+180),Math.min(255,brandColor.getBlue()+180));
            JPanel h=new JPanel(new BorderLayout(4,0)); h.setBackground(bgH); h.setBorder(new EmptyBorder(8,10,8,10));
            JPanel colorBar=new JPanel(); colorBar.setPreferredSize(new Dimension(6,0)); colorBar.setBackground(brandColor); colorBar.setOpaque(true);
            JLabel lName=new JLabel("  "+brandName+" ("+types.size()+" mặt hàng)"); lName.setFont(F_GROUP); lName.setForeground(brandColor.darker());
            btnToggle=new JButton("▲"){@Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(brandColor);g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);g2.dispose();super.paintComponent(g);}};
            btnToggle.setFont(new Font("Arial",Font.BOLD,11)); btnToggle.setForeground(Color.WHITE); btnToggle.setPreferredSize(new Dimension(30,26));
            btnToggle.setContentAreaFilled(false);btnToggle.setBorderPainted(false);btnToggle.setFocusPainted(false);
            btnToggle.addActionListener(e->{collapsed=!collapsed;tablePanel.setVisible(!collapsed);btnToggle.setText(collapsed?"▼":"▲");revalidate();});
            h.add(colorBar,BorderLayout.WEST); h.add(lName,BorderLayout.CENTER); h.add(btnToggle,BorderLayout.EAST);
            return h;
        }

        private JPanel buildTablePanel() {
            JPanel p=new JPanel(new BorderLayout()); p.setBackground(C_WHITE);
            // Cols: Tên sản phẩm | Mệnh giá (đ) | %CK | Giá CK (đ)
            String[] cols={"Tên sản phẩm","Mệnh giá (đ)","%CK","Giá CK (đ)"};
            model=new DefaultTableModel(cols,0){
                @Override public boolean isCellEditable(int r,int c){
                    if (c==2) return true; // %CK
                    if (c==1) { // denom editable for coffee
                        CardType ct=types.get(r); String nl=ct.name.toLowerCase();
                        for(String kw:EDITABLE_DENOM) if(nl.contains(kw)) return true;
                    }
                    return false;
                }
            };
            for (CardType ct:types) {
                double ck=ct.denomination>0?Math.round((1.0-(double)ct.defaultDiscount/ct.denomination)*1000.0)/10.0:0.0;
                model.addRow(new Object[]{ct.name,ct.denomination,ck,ct.defaultDiscount});
            }
            model.addTableModelListener(e->{
                int row=e.getFirstRow(),col=e.getColumn(); if(row<0||row>=types.size()) return;
                if(col==1||col==2){ recalcGiaCK(row); changed[row]=true; recountChanged(); }
            });

            table=new JTable(model); table.setFont(F_TABLE); table.setRowHeight(26);
            table.setShowGrid(true); table.setGridColor(C_GRID); table.setIntercellSpacing(new Dimension(1,1));
            table.setBackground(C_WHITE); table.setSelectionBackground(new Color(0xFFF9C4)); table.setSelectionForeground(Color.BLACK);

            JTableHeader th=table.getTableHeader(); th.setFont(F_TH); th.setBackground(C_TH_BG); th.setForeground(C_TH_FG);
            th.setPreferredSize(new Dimension(0,26)); th.setReorderingAllowed(false);

            int[] w={220,120,80,120}; for(int i=0;i<w.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

            table.setDefaultRenderer(Object.class,new GRenderer());
            // Editors
            table.getColumnModel().getColumn(1).setCellEditor(new DenomEditor());
            table.getColumnModel().getColumn(2).setCellEditor(new CKEdit());
            for(int c:new int[]{0,3}) table.getColumnModel().getColumn(c).setCellEditor(new ROEditor());

            int rowH=26*types.size()+30; JScrollPane sc=new JScrollPane(table); sc.setBorder(BorderFactory.createEmptyBorder()); sc.setPreferredSize(new Dimension(0,rowH+2));
            p.add(sc,BorderLayout.CENTER); return p;
        }

        private void recalcGiaCK(int row) {
            TableModelListener[] ls=model.getTableModelListeners();
            for(TableModelListener l:ls) model.removeTableModelListener(l);
            try{
                int denom=toInt(model.getValueAt(row,1)); double ck=toDbl(model.getValueAt(row,2));
                model.setValueAt((int)Math.round(denom*(1.0-ck/100.0)),row,3);
            }finally{ for(TableModelListener l:ls) model.addTableModelListener(l); }
        }

        private class GRenderer extends DefaultTableCellRenderer {
            @Override public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int row,int col){
                if(val!=null&&(col==1||col==3)){try{val=NF.format(Long.parseLong(val.toString()));}catch(Exception ignore){}}
                if(val!=null&&col==2){try{val=toDbl(val)+"%";}catch(Exception ignore){}}
                super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                setFont(F_TABLE); setBorder(new EmptyBorder(0,6,0,6));
                if(sel){setBackground(new Color(0xFFF9C4));setForeground(Color.BLACK);}
                else{
                    boolean ch=row<changed.length&&changed[row];
                    boolean isDenomEdit=model.isCellEditable(row,1);
                    if(ch){setBackground(C_CHANGED);setForeground(new Color(0x5D4037));}
                    else if(col==2){setBackground(C_EDIT);setForeground(C_RED);}
                    else if(col==1&&isDenomEdit){setBackground(new Color(0xF1F8E9));setForeground(C_GREEN);}
                    else{setBackground(row%2==0?C_ROW1:C_ROW2);setForeground(Color.BLACK);}
                }
                setHorizontalAlignment(col>=1?SwingConstants.RIGHT:SwingConstants.LEFT);
                if(col==2)setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        }

        private class CKEdit extends DefaultCellEditor {
            private final JTextField tf;
            CKEdit(){super(new JTextField());tf=(JTextField)getComponent();tf.setFont(F_TABLE);tf.setHorizontalAlignment(SwingConstants.CENTER);tf.setBorder(BorderFactory.createLineBorder(C_RED,2));tf.setBackground(new Color(0xFFF8F8));setClickCountToStart(1);}
            @Override public Component getTableCellEditorComponent(JTable t,Object val,boolean sel,int row,int col){
                double d=toDbl(val); tf.setText(d==(long)d?String.valueOf((long)d):String.valueOf(d));
                SwingUtilities.invokeLater(tf::selectAll); return tf;
            }
            @Override public Object getCellEditorValue(){try{return Double.parseDouble(tf.getText().trim());}catch(NumberFormatException e){return 0.0;}}
        }
        private class DenomEditor extends DefaultCellEditor {
            private final JTextField tf;
            DenomEditor(){super(new JTextField());tf=(JTextField)getComponent();tf.setFont(F_TABLE);tf.setHorizontalAlignment(SwingConstants.RIGHT);tf.setBorder(BorderFactory.createLineBorder(C_GREEN,2));setClickCountToStart(1);}
            @Override public Component getTableCellEditorComponent(JTable t,Object val,boolean sel,int row,int col){tf.setText(val!=null?val.toString().replace(",","").replace(".",""):"0");SwingUtilities.invokeLater(tf::selectAll);return tf;}
            @Override public Object getCellEditorValue(){try{return Integer.parseInt(tf.getText().trim().replace(",",""));}catch(NumberFormatException e){return 0;}}
        }
        private class ROEditor extends DefaultCellEditor{ROEditor(){super(new JTextField());setClickCountToStart(999);}}
    }
}