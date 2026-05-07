package ui.tabs;

import db.DBConnection;
import ui.tabs.model.CardType;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * F2 - Nhập Hàng — Phiếu nhập hàng (theo ảnh mẫu 2)
 */
public class F2ReceivedPanel extends BasePhieuPanel {

    private Integer editingId = null;

    // LEFT
    private DefaultListModel<PhieuEntry> listModel;
    private JList<PhieuEntry> phieuList;
    private JLabel lblDateLeft, lblTongCong;

    // RIGHT
    private JLabel lblSoPhieu;
    private JTextField txtNgay, txtNCC, txtGhiChu;
    private DefaultTableModel itemModel;
    private JTable itemTable;
    private JLabel lblTongThanhTien;

    public F2ReceivedPanel() {
        initCards();
        setLayout(new BorderLayout());
        setBackground(C_BG);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeft(), buildRight());
        split.setDividerLocation(320); split.setDividerSize(5); split.setBorder(null);
        add(split, BorderLayout.CENTER);

        loadPhieuList(); newPhieu();
    }

    // ═══ LEFT ════════════════════════════════════════════════════════════════

    private JPanel buildLeft() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(C_BG); p.setBorder(new EmptyBorder(8,8,8,4));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT,4,4)); toolbar.setBackground(C_BG);
        JButton prev=iconBtn("◀"); JButton next=iconBtn("▶");
        lblDateLeft=new JLabel(currentDate.format(DATE_FMT)); lblDateLeft.setFont(F_CODE_RED); lblDateLeft.setForeground(C_RED); lblDateLeft.setBorder(new EmptyBorder(0,6,0,6));
        lblDateLeft.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblDateLeft.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){pickDate();}});
        prev.addActionListener(e->shiftDate(-1)); next.addActionListener(e->shiftDate(1));
        JButton btnFilter=mkBtn("🔍 Lọc dữ liệu",C_ACCENT,C_WHITE); btnFilter.addActionListener(e->loadPhieuList());
        toolbar.add(prev);toolbar.add(lblDateLeft);toolbar.add(next);toolbar.add(btnFilter);

        JPanel th=new JPanel(new GridLayout(1,3)); th.setBackground(C_TH_BG); th.setBorder(new EmptyBorder(4,6,4,6));
        for(String s:new String[]{"Số phiếu","Ngày nhập","Số tiền"}){JLabel l=new JLabel(s,SwingConstants.CENTER);l.setFont(F_TH);l.setForeground(C_TH_FG);th.add(l);}

        listModel=new DefaultListModel<>();
        phieuList=new JList<>(listModel); phieuList.setFont(F_LIST); phieuList.setSelectionBackground(C_SEL_BILL); phieuList.setFixedCellHeight(26);
        phieuList.setCellRenderer(new PhieuCellRenderer());
        phieuList.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&phieuList.getSelectedValue()!=null)loadPhieuIntoForm(phieuList.getSelectedValue());});

        JScrollPane scroll=new JScrollPane(phieuList); scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        JPanel listArea=new JPanel(new BorderLayout()); listArea.add(th,BorderLayout.NORTH); listArea.add(scroll,BorderLayout.CENTER);

        JPanel foot=new JPanel(new GridLayout(1,2)); foot.setBackground(C_TOTAL_BG); foot.setBorder(BorderFactory.createMatteBorder(1,0,0,0,C_BORDER));
        JLabel lT=new JLabel("Tổng cộng",SwingConstants.LEFT); lT.setFont(F_TOTAL); lT.setBorder(new EmptyBorder(6,8,6,0));
        lblTongCong=new JLabel("0",SwingConstants.RIGHT); lblTongCong.setFont(F_TOTAL); lblTongCong.setForeground(C_RED); lblTongCong.setBorder(new EmptyBorder(6,0,6,8));
        foot.add(lT);foot.add(lblTongCong);

        p.add(toolbar,BorderLayout.NORTH);p.add(listArea,BorderLayout.CENTER);p.add(foot,BorderLayout.SOUTH);
        return p;
    }

    // ═══ RIGHT ═══════════════════════════════════════════════════════════════

    private JPanel buildRight() {
        JPanel p=new JPanel(new BorderLayout()); p.setBackground(C_BG); p.setBorder(new EmptyBorder(8,4,8,8));
        p.add(buildRightToolbar(),BorderLayout.NORTH);
        JPanel mid=new JPanel(new BorderLayout()); mid.setBackground(C_WHITE); mid.setBorder(BorderFactory.createLineBorder(C_BORDER));
        mid.add(buildFormHeader(),BorderLayout.NORTH);
        mid.add(buildItemTable(),BorderLayout.CENTER);
        p.add(mid,BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRightToolbar() {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,6,4)); p.setBackground(C_BG);
        JButton prev2=iconBtn("◀");JButton next2=iconBtn("▶");
        JLabel lDate=new JLabel(currentDate.format(DATE_FMT)); lDate.setFont(F_CODE_RED); lDate.setForeground(C_RED); lDate.setBorder(new EmptyBorder(0,6,0,6));
        prev2.addActionListener(e->{shiftDate(-1);lDate.setText(currentDate.format(DATE_FMT));});
        next2.addActionListener(e->{shiftDate(1);lDate.setText(currentDate.format(DATE_FMT));});
        JButton bf=mkBtn("🔍 Lọc dữ liệu",C_ACCENT,C_WHITE); bf.addActionListener(e->loadPhieuList());
        p.add(prev2);p.add(lDate);p.add(next2);p.add(bf); p.add(Box.createHorizontalStrut(10));
        JButton btnNew=mkBtn("Thêm",C_ACCENT,C_WHITE);
        JButton btnDel=mkBtn("Xóa",C_RED,C_WHITE);
        JButton btnPrint=mkBtn("In phiếu",C_ORANGE,C_WHITE);
        JButton btnClose=mkBtn("Đóng",C_BORDER,C_WHITE);
        btnNew.addActionListener(e->newPhieu());
        btnDel.addActionListener(e->deletePhieu());
        btnPrint.addActionListener(e->JOptionPane.showMessageDialog(this,"Chức năng in đang phát triển."));
        btnClose.addActionListener(e->newPhieu());
        p.add(btnNew);p.add(btnDel);p.add(btnPrint);p.add(btnClose);
        return p;
    }

    private JPanel buildFormHeader() {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,10,6));
        p.setBackground(new Color(0xECEFF1)); p.setBorder(BorderFactory.createMatteBorder(0,0,1,0,C_BORDER));

        lblSoPhieu=new JLabel("PN00001"); lblSoPhieu.setFont(F_CODE_RED); lblSoPhieu.setForeground(C_RED);
        txtNgay=new JTextField(currentDate.format(DATE_FMT),10); styleField(txtNgay); txtNgay.setForeground(C_RED);
        txtNCC=new JTextField("",18); styleField(txtNCC);
        txtGhiChu=new JTextField("",20); styleField(txtGhiChu);

        JCheckBox chkTT=new JCheckBox("Thanh toán ngay"); chkTT.setFont(F_LABEL); chkTT.setOpaque(false); chkTT.setSelected(true);
        ButtonGroup bg=new ButtonGroup();
        JRadioButton rCK=new JRadioButton("CK → Giá",true); rCK.setFont(F_LABEL); rCK.setOpaque(false);
        JRadioButton rGia=new JRadioButton("Giá → CK"); rGia.setFont(F_LABEL); rGia.setOpaque(false);
        bg.add(rCK);bg.add(rGia);

        p.add(lbl("Số phiếu"));p.add(lblSoPhieu);
        p.add(lbl("  Ngày nhập"));p.add(txtNgay);
        p.add(chkTT);p.add(rCK);p.add(rGia);
        p.add(lbl("  NCC"));p.add(txtNCC);
        p.add(lbl("  Ghi chú"));p.add(txtGhiChu);
        return p;
    }

    private JPanel buildItemTable() {
        JPanel p=new JPanel(new BorderLayout()); p.setBackground(C_WHITE);

        String[] cols={"Mã hàng","Tên hàng","ĐVT","Số lượng","Đơn giá","CK (%)","Thành tiền"};
        itemModel=new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){return c==0||c==3||c==5;}
        };
        itemModel.addTableModelListener(e->{
            int row=e.getFirstRow(),col=e.getColumn(); if(row<0)return;
            if(col==0) resolveCode(row);
            if(col==3||col==5) recalcRow(row);
        });
        for(int i=0;i<12;i++) addEmptyRow();

        itemTable=new JTable(itemModel); styleTable(itemTable);
        itemTable.setDefaultRenderer(Object.class,new ItemRenderer());
        itemTable.getColumnModel().getColumn(5).setCellEditor(new CKEditor());
        itemTable.getColumnModel().getColumn(0).setCellEditor(new CodeEditor());
        itemTable.getColumnModel().getColumn(3).setCellEditor(new IntEditor());
        for(int c:new int[]{1,2,4,6}) itemTable.getColumnModel().getColumn(c).setCellEditor(new ReadOnlyEditor());

        int[] w={70,200,50,80,100,65,120};
        for(int i=0;i<w.length;i++) itemTable.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        JScrollPane scroll=new JScrollPane(itemTable); scroll.setBorder(BorderFactory.createEmptyBorder());
        p.add(scroll,BorderLayout.CENTER);

        JPanel foot=new JPanel(new BorderLayout()); foot.setBackground(C_WHITE); foot.setBorder(BorderFactory.createMatteBorder(1,0,0,0,C_GRID));
        JLabel lNguoi=new JLabel("  Người lập phiếu:  SIM CARD 66 Lý Nam Đế"); lNguoi.setFont(F_LABEL); lNguoi.setForeground(C_GREEN);
        lblTongThanhTien=new JLabel("0  ",SwingConstants.RIGHT); lblTongThanhTien.setFont(F_TOTAL); lblTongThanhTien.setForeground(C_TT_RED);
        JPanel rf=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,4)); rf.setOpaque(false);
        JLabel lTTT=new JLabel("Tổng thành tiền   "); lTTT.setFont(F_BOLD);
        JButton btnSave=mkBtn("💾 Lưu phiếu",C_GREEN,C_WHITE); btnSave.addActionListener(e->savePhieu());
        rf.add(lTTT);rf.add(lblTongThanhTien);rf.add(btnSave);
        foot.add(lNguoi,BorderLayout.WEST);foot.add(rf,BorderLayout.EAST);
        p.add(foot,BorderLayout.SOUTH);
        return p;
    }

    // ═══ LOGIC ═══════════════════════════════════════════════════════════════

    private void addEmptyRow(){itemModel.addRow(new Object[]{"","","Thẻ",0,0,0.0,0});}

    private void resolveCode(int row){
        Object o=itemModel.getValueAt(row,0); if(o==null)return;
        String code=o.toString().trim().toUpperCase(); if(code.isEmpty()){clearRow(row);return;}
        CardType ct=codeMap.get(code); if(ct==null){clearRow(row);return;}
        double ckPct=ct.denomination>0?Math.round((1.0-(double)ct.defaultDiscount/ct.denomination)*1000.0)/10.0:0.0;
        int qty=toInt(itemModel.getValueAt(row,3)); if(qty==0)qty=1;
        int giaCK=(int)Math.round(ct.denomination*(1.0-ckPct/100.0));
        final int fq=qty,fg=giaCK;
        suppress(itemModel,()->{itemModel.setValueAt(ct.name,row,1);itemModel.setValueAt("Thẻ",row,2);itemModel.setValueAt(ct.denomination,row,4);itemModel.setValueAt(ckPct,row,5);itemModel.setValueAt(fq,row,3);itemModel.setValueAt((long)fq*fg,row,6);});
        refreshTotals();
    }

    private void clearRow(int row){suppress(itemModel,()->{itemModel.setValueAt("",row,1);itemModel.setValueAt("Thẻ",row,2);itemModel.setValueAt(0,row,3);itemModel.setValueAt(0,row,4);itemModel.setValueAt(0.0,row,5);itemModel.setValueAt(0,row,6);});refreshTotals();}

    private void recalcRow(int row){
        int denom=toInt(itemModel.getValueAt(row,4));if(denom==0)return;
        int qty=toInt(itemModel.getValueAt(row,3));double ck=toDbl(itemModel.getValueAt(row,5));
        int giaCK=(int)Math.round(denom*(1.0-ck/100.0));
        suppress(itemModel,()->itemModel.setValueAt((long)qty*giaCK,row,6));refreshTotals();
    }

    private void refreshTotals(){long t=0;for(int i=0;i<itemModel.getRowCount();i++)t+=toLong(itemModel.getValueAt(i,6));if(lblTongThanhTien!=null)lblTongThanhTien.setText(NF.format(t));}

    private void newPhieu(){
        editingId=null;
        if(lblSoPhieu!=null)lblSoPhieu.setText("PN"+String.format("%05d",System.currentTimeMillis()%99999));
        if(txtNgay!=null)txtNgay.setText(currentDate.format(DATE_FMT));
        if(txtNCC!=null)txtNCC.setText(""); if(txtGhiChu!=null)txtGhiChu.setText("");
        if(itemModel!=null){itemModel.setRowCount(0);for(int i=0;i<12;i++)addEmptyRow();}
        if(phieuList!=null)phieuList.clearSelection(); refreshTotals();
    }

    private void loadPhieuList(){
        if(listModel==null)return; listModel.clear(); long total=0;
        try(Connection conn=DBConnection.getConnection();PreparedStatement ps=conn.prepareStatement(
                "SELECT id,supplier_name,COALESCE(SUM(quantity*discount_price),0) AS total FROM inventory_entries WHERE date=? AND type='received' GROUP BY id ORDER BY id")){
            ps.setString(1,currentDate.toString());ResultSet rs=ps.executeQuery();
            while(rs.next()){long t=rs.getLong("total");total+=t;listModel.addElement(new PhieuEntry(rs.getInt("id"),currentDate,"PN"+String.format("%05d",rs.getInt("id")),rs.getString("supplier_name"),t));}
        }catch(Exception ex){ex.printStackTrace();}
        if(lblTongCong!=null)lblTongCong.setText(NF.format(total));
        if(lblDateLeft!=null)lblDateLeft.setText(currentDate.format(DATE_FMT));
    }

    private void loadPhieuIntoForm(PhieuEntry entry){
        editingId=entry.id;lblSoPhieu.setText(entry.soPhieu);txtNgay.setText(currentDate.format(DATE_FMT));
        if(entry.ncc!=null)txtNCC.setText(entry.ncc); itemModel.setRowCount(0);
        try(Connection conn=DBConnection.getConnection();PreparedStatement ps=conn.prepareStatement(
                "SELECT ie.quantity,ie.discount_price,ct.name,ct.denomination,ct.id as ct_id FROM inventory_entries ie JOIN card_types ct ON ct.id=ie.card_type_id WHERE ie.id=? ORDER BY ct.id")){
            ps.setInt(1,entry.id);ResultSet rs=ps.executeQuery();
            while(rs.next()){int qty=rs.getInt("quantity"),denom=rs.getInt("denomination"),ckPri=rs.getInt("discount_price");double ck=denom>0?Math.round((1.0-(double)ckPri/denom)*1000.0)/10.0:0;CardType ct2=findById(rs.getInt("ct_id"));itemModel.addRow(new Object[]{ct2!=null?ct2.code:"",rs.getString("name"),"Thẻ",qty,denom,ck,(long)qty*ckPri});}
        }catch(Exception ex){ex.printStackTrace();}
        while(itemModel.getRowCount()<12)addEmptyRow(); refreshTotals();
    }

    private void savePhieu(){
        if(itemTable.isEditing())itemTable.getCellEditor().stopCellEditing();
        List<int[]> rows=new ArrayList<>();
        for(int i=0;i<itemModel.getRowCount();i++){
            String code=itemModel.getValueAt(i,0).toString().trim().toUpperCase();if(code.isEmpty())continue;
            int qty=toInt(itemModel.getValueAt(i,3));if(qty<=0)continue;
            CardType ct=codeMap.get(code);if(ct==null)continue;
            double ck=toDbl(itemModel.getValueAt(i,5));
            int ckPrice=(int)Math.round(ct.denomination*(1.0-ck/100.0));
            rows.add(new int[]{ct.id,qty,ct.denomination,ckPrice});
        }
        if(rows.isEmpty()){showError("Chưa nhập dòng nào hợp lệ!");return;}
        String ncc=txtNCC.getText().trim();
        try(Connection conn=DBConnection.getConnection()){
            conn.setAutoCommit(false);
            try{
                if(editingId!=null)conn.prepareStatement("DELETE FROM inventory_entries WHERE id="+editingId).executeUpdate();
                PreparedStatement ps=conn.prepareStatement("INSERT INTO inventory_entries(date,type,card_type_id,quantity,price,discount_price,supplier_name,supplier_phone,supplier_address) VALUES(?,'received',?,?,?,?,?,'','')");
                for(int[] r:rows){ps.setString(1,currentDate.toString());ps.setInt(2,r[0]);ps.setInt(3,r[1]);ps.setInt(4,r[2]);ps.setInt(5,r[3]);ps.setString(6,ncc);ps.addBatch();}
                ps.executeBatch();conn.commit();
                JOptionPane.showMessageDialog(this,"✅ Lưu phiếu nhập thành công!","OK",JOptionPane.INFORMATION_MESSAGE);
                loadPhieuList();newPhieu();
            }catch(Exception ex){conn.rollback();throw ex;}
        }catch(Exception ex){ex.printStackTrace();showError("Lỗi lưu: "+ex.getMessage());}
    }

    private void deletePhieu(){
        PhieuEntry sel=phieuList.getSelectedValue();if(sel==null){showError("Chọn phiếu cần xóa!");return;}
        if(JOptionPane.showConfirmDialog(this,"Xóa phiếu "+sel.soPhieu+"?","Xác nhận",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try(Connection conn=DBConnection.getConnection()){conn.prepareStatement("DELETE FROM inventory_entries WHERE id="+sel.id).executeUpdate();}catch(Exception ex){ex.printStackTrace();}
        loadPhieuList();newPhieu();
    }

    private void pickDate(){String in=JOptionPane.showInputDialog(this,"Nhập ngày (dd/MM/yyyy):",currentDate.format(DATE_FMT));if(in==null||in.isBlank())return;try{currentDate=LocalDate.parse(in.trim(),DATE_FMT);loadPhieuList();}catch(Exception ex){showError("Ngày không hợp lệ!");}}

    private void shiftDate(int d){currentDate=currentDate.plusDays(d);if(lblDateLeft!=null)lblDateLeft.setText(currentDate.format(DATE_FMT));if(txtNgay!=null)txtNgay.setText(currentDate.format(DATE_FMT));loadPhieuList();}

    private static class PhieuEntry{int id;LocalDate date;String soPhieu,ncc;long total;PhieuEntry(int id,LocalDate d,String sp,String ncc,long t){this.id=id;date=d;soPhieu=sp;this.ncc=ncc;total=t;}}

    private class PhieuCellRenderer extends DefaultListCellRenderer{
        @Override public Component getListCellRendererComponent(JList<?> list,Object val,int idx,boolean sel,boolean foc){
            PhieuEntry e=(PhieuEntry)val;JPanel p=new JPanel(new GridLayout(1,3));p.setOpaque(true);
            p.setBackground(sel?C_SEL_BILL:(idx%2==0?C_ROW1:C_ROW2));p.setBorder(new EmptyBorder(2,4,2,4));
            JLabel l1=new JLabel(e.soPhieu);l1.setFont(F_LIST);
            JLabel l2=new JLabel(e.date.format(DATE_FMT),SwingConstants.CENTER);l2.setFont(F_LIST);
            JLabel l3=new JLabel(NF.format(e.total),SwingConstants.RIGHT);l3.setFont(new Font("Arial",Font.BOLD,13));l3.setForeground(C_TT_RED);
            p.add(l1);p.add(l2);p.add(l3);return p;
        }
    }
}