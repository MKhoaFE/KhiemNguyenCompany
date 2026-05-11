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
 * F1 - Tồn Đầu / Phiếu trả lại hàng
 * - Bỏ cột ĐVT
 * - 1 hàng mặc định; gõ mã hàng → tự sinh hàng mới
 * - Nút "In phiếu" → HTML preview (InvoiceBuilder)
 */
public class F1OpeningPanel extends BasePhieuPanel {

    private Integer editingId = null;

    private DefaultListModel<PhieuEntry> listModel;
    private JList<PhieuEntry> phieuList;
    private JLabel lblDateLeft, lblTongCong;

    private JLabel lblSoPhieu;
    private JTextField txtNgay, txtGhiChu;
    private DefaultTableModel itemModel;
    private JTable itemTable;
    private JLabel lblTongThanhTien;

    private static final int COL_CODE  = 0;
    private static final int COL_NAME  = 1;
    private static final int COL_QTY   = 2;
    private static final int COL_PRICE = 3;
    private static final int COL_CK    = 4;
    private static final int COL_TOTAL = 5;

    public F1OpeningPanel() {
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
        JButton prev = iconBtn("◀"); JButton next = iconBtn("▶");
        lblDateLeft = new JLabel(currentDate.format(DATE_FMT));
        lblDateLeft.setFont(F_CODE_RED); lblDateLeft.setForeground(C_RED); lblDateLeft.setBorder(new EmptyBorder(0,6,0,6));
        lblDateLeft.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblDateLeft.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){pickDate();}});
        prev.addActionListener(e->shiftDate(-1)); next.addActionListener(e->shiftDate(1));
        JButton btnFilter = mkBtn("🔍 Lọc dữ liệu",C_ACCENT,C_WHITE); btnFilter.addActionListener(e->loadPhieuList());
        toolbar.add(prev);toolbar.add(lblDateLeft);toolbar.add(next);toolbar.add(btnFilter);

        JPanel th = new JPanel(new GridLayout(1,3)); th.setBackground(C_TH_BG); th.setBorder(new EmptyBorder(4,6,4,6));
        for(String s:new String[]{"Số phiếu","Ngày nhập","Số tiền"}){JLabel l=new JLabel(s,SwingConstants.CENTER);l.setFont(F_TH);l.setForeground(C_TH_FG);th.add(l);}
        listModel=new DefaultListModel<>();
        phieuList=new JList<>(listModel);phieuList.setFont(F_LIST);phieuList.setSelectionBackground(C_SEL_BILL);phieuList.setFixedCellHeight(26);
        phieuList.setCellRenderer(new PhieuCellRenderer());
        phieuList.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&phieuList.getSelectedValue()!=null)loadPhieuIntoForm(phieuList.getSelectedValue());});
        JScrollPane scroll=new JScrollPane(phieuList); scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        JPanel listArea=new JPanel(new BorderLayout()); listArea.add(th,BorderLayout.NORTH); listArea.add(scroll,BorderLayout.CENTER);
        JPanel foot=new JPanel(new GridLayout(1,2)); foot.setBackground(C_TOTAL_BG); foot.setBorder(BorderFactory.createMatteBorder(1,0,0,0,C_BORDER));
        JLabel lT=new JLabel("Tổng cộng",SwingConstants.LEFT);lT.setFont(F_TOTAL);lT.setBorder(new EmptyBorder(6,8,6,0));
        lblTongCong=new JLabel("0",SwingConstants.RIGHT);lblTongCong.setFont(F_TOTAL);lblTongCong.setForeground(C_RED);lblTongCong.setBorder(new EmptyBorder(6,0,6,8));
        foot.add(lT);foot.add(lblTongCong);
        p.add(toolbar,BorderLayout.NORTH);p.add(listArea,BorderLayout.CENTER);p.add(foot,BorderLayout.SOUTH);
        return p;
    }

    // ═══ RIGHT ═══════════════════════════════════════════════════════════════

    private JPanel buildRight() {
        JPanel p=new JPanel(new BorderLayout());p.setBackground(C_BG);p.setBorder(new EmptyBorder(8,4,8,8));
        p.add(buildRightToolbar(),BorderLayout.NORTH);
        JPanel mid=new JPanel(new BorderLayout());mid.setBackground(C_WHITE);mid.setBorder(BorderFactory.createLineBorder(C_BORDER));
        mid.add(buildFormHeader(),BorderLayout.NORTH);mid.add(buildItemTable(),BorderLayout.CENTER);
        p.add(mid,BorderLayout.CENTER); return p;
    }

    private JPanel buildRightToolbar() {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));p.setBackground(C_BG);
        JButton prev2=iconBtn("◀");JButton next2=iconBtn("▶");
        JLabel lDate=new JLabel(currentDate.format(DATE_FMT));lDate.setFont(F_CODE_RED);lDate.setForeground(C_RED);lDate.setBorder(new EmptyBorder(0,6,0,6));
        prev2.addActionListener(e->{shiftDate(-1);lDate.setText(currentDate.format(DATE_FMT));});
        next2.addActionListener(e->{shiftDate(1);lDate.setText(currentDate.format(DATE_FMT));});
        JButton bf=mkBtn("🔍 Lọc dữ liệu",C_ACCENT,C_WHITE);bf.addActionListener(e->loadPhieuList());
        p.add(prev2);p.add(lDate);p.add(next2);p.add(bf);p.add(Box.createHorizontalStrut(10));
        JButton btnNew=mkBtn("Thêm",C_ACCENT,C_WHITE);JButton btnDel=mkBtn("Xóa",C_RED,C_WHITE);
        JButton btnPrint=mkBtn("In phiếu",C_ORANGE,C_WHITE);JButton btnClose=mkBtn("Đóng",C_BORDER,C_WHITE);
        btnNew.addActionListener(e->newPhieu());btnDel.addActionListener(e->deletePhieu());
        btnPrint.addActionListener(e->showPrintPreview());btnClose.addActionListener(e->newPhieu());
        p.add(btnNew);p.add(btnDel);p.add(btnPrint);p.add(btnClose); return p;
    }

    private JPanel buildFormHeader() {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,12,6));
        p.setBackground(new Color(0xECEFF1));p.setBorder(BorderFactory.createMatteBorder(0,0,1,0,C_BORDER));
        lblSoPhieu=new JLabel("PN00001");lblSoPhieu.setFont(F_CODE_RED);lblSoPhieu.setForeground(C_RED);
        txtNgay=new JTextField(currentDate.format(DATE_FMT),10);styleField(txtNgay);txtNgay.setForeground(C_RED);
        txtGhiChu=new JTextField("",22);styleField(txtGhiChu);
        JCheckBox chkTT=new JCheckBox("Thanh toán ngay");chkTT.setFont(F_LABEL);chkTT.setOpaque(false);chkTT.setSelected(true);
        p.add(lbl("Số phiếu"));p.add(lblSoPhieu);p.add(lbl("  Ngày nhập"));p.add(txtNgay);p.add(chkTT);p.add(lbl("  Ghi chú"));p.add(txtGhiChu);
        return p;
    }

    private JPanel buildItemTable() {
        JPanel p=new JPanel(new BorderLayout());p.setBackground(C_WHITE);
        String[] cols={"Mã hàng","Tên hàng","Số lượng","Đơn giá","CK (%)","Thành tiền"};
        itemModel=new DefaultTableModel(cols,0){@Override public boolean isCellEditable(int r,int c){return c==COL_CODE||c==COL_QTY||c==COL_CK;}};
        itemModel.addTableModelListener(e->{
            int row=e.getFirstRow(),col=e.getColumn();if(row<0)return;
            if(col==COL_CODE){resolveCode(row);maybeAddRow(row);}
            if(col==COL_QTY||col==COL_CK)recalcRow(row);
        });
        addEmptyRow();
        itemTable=new JTable(itemModel);styleTable(itemTable);
        itemTable.setDefaultRenderer(Object.class,new F1Renderer());
        itemTable.getColumnModel().getColumn(COL_CK).setCellEditor(new CKEditor());
        itemTable.getColumnModel().getColumn(COL_CODE).setCellEditor(new CodeEditor());
        itemTable.getColumnModel().getColumn(COL_QTY).setCellEditor(new IntEditor());
        for(int c:new int[]{COL_NAME,COL_PRICE,COL_TOTAL})itemTable.getColumnModel().getColumn(c).setCellEditor(new ReadOnlyEditor());
        int[] w={80,220,85,110,70,130};
        for(int i=0;i<w.length;i++)itemTable.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        JScrollPane scroll=new JScrollPane(itemTable);scroll.setBorder(BorderFactory.createEmptyBorder());p.add(scroll,BorderLayout.CENTER);
        JPanel foot=new JPanel(new BorderLayout());foot.setBackground(C_WHITE);foot.setBorder(BorderFactory.createMatteBorder(1,0,0,0,C_GRID));
        JLabel lNguoi=new JLabel("  Người lập phiếu:  SIM CARD 66 Lý Nam Đế");lNguoi.setFont(F_LABEL);lNguoi.setForeground(C_GREEN);
        lblTongThanhTien=new JLabel("0  ",SwingConstants.RIGHT);lblTongThanhTien.setFont(F_TOTAL);lblTongThanhTien.setForeground(C_TT_RED);
        JPanel rf=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,4));rf.setOpaque(false);
        rf.add(new JLabel("Tổng thành tiền   "){{setFont(F_BOLD);}});rf.add(lblTongThanhTien);
        JButton btnSave=mkBtn("💾 Lưu phiếu",C_GREEN,C_WHITE);btnSave.addActionListener(e->savePhieu());rf.add(btnSave);
        foot.add(lNguoi,BorderLayout.WEST);foot.add(rf,BorderLayout.EAST);p.add(foot,BorderLayout.SOUTH);
        return p;
    }

    // ═══ AUTO-GROW ════════════════════════════════════════════════════════════

    private void maybeAddRow(int row){
        Object code=itemModel.getValueAt(row,COL_CODE);
        if(code!=null&&!code.toString().trim().isEmpty()&&row==itemModel.getRowCount()-1)addEmptyRow();
    }

    // ═══ LOGIC ═══════════════════════════════════════════════════════════════

    private void addEmptyRow(){itemModel.addRow(new Object[]{"","",0,0,0.0,0});}

    private void resolveCode(int row){
        Object o=itemModel.getValueAt(row,COL_CODE);if(o==null)return;
        String code=o.toString().trim().toUpperCase();if(code.isEmpty()){clearRow(row);return;}
        CardType ct=codeMap.get(code);if(ct==null){clearRow(row);return;}
        double ckPct=ct.denomination>0?Math.round((1.0-(double)ct.defaultDiscount/ct.denomination)*1000.0)/10.0:0.0;
        int qty=toInt(itemModel.getValueAt(row,COL_QTY));if(qty==0)qty=1;
        int giaCK=(int)Math.round(ct.denomination*(1.0-ckPct/100.0));
        final int fq=qty,fg=giaCK;
        suppress(itemModel,()->{
            itemModel.setValueAt(ct.name,row,COL_NAME);itemModel.setValueAt(ct.denomination,row,COL_PRICE);
            itemModel.setValueAt(ckPct,row,COL_CK);itemModel.setValueAt(fq,row,COL_QTY);itemModel.setValueAt((long)fq*fg,row,COL_TOTAL);
        });
        refreshTotals();
    }

    private void clearRow(int row){
        suppress(itemModel,()->{itemModel.setValueAt("",row,COL_NAME);itemModel.setValueAt(0,row,COL_QTY);itemModel.setValueAt(0,row,COL_PRICE);itemModel.setValueAt(0.0,row,COL_CK);itemModel.setValueAt(0,row,COL_TOTAL);});
        refreshTotals();
    }

    private void recalcRow(int row){
        int denom=toInt(itemModel.getValueAt(row,COL_PRICE));if(denom==0)return;
        int qty=toInt(itemModel.getValueAt(row,COL_QTY));double ck=toDbl(itemModel.getValueAt(row,COL_CK));
        int giaCK=(int)Math.round(denom*(1.0-ck/100.0));
        suppress(itemModel,()->itemModel.setValueAt((long)qty*giaCK,row,COL_TOTAL));refreshTotals();
    }

    private void refreshTotals(){
        long t=0;for(int i=0;i<itemModel.getRowCount();i++)t+=toLong(itemModel.getValueAt(i,COL_TOTAL));
        if(lblTongThanhTien!=null)lblTongThanhTien.setText(NF.format(t));
    }

    private void newPhieu(){
        editingId=null;
        if(lblSoPhieu!=null)lblSoPhieu.setText("PN"+String.format("%05d",System.currentTimeMillis()%99999));
        if(txtNgay!=null)txtNgay.setText(currentDate.format(DATE_FMT));
        if(txtGhiChu!=null)txtGhiChu.setText("");
        if(itemModel!=null){itemModel.setRowCount(0);addEmptyRow();}
        if(phieuList!=null)phieuList.clearSelection();refreshTotals();
    }

    private void loadPhieuList(){
        if(listModel==null)return;listModel.clear();long total=0;
        try(Connection conn=DBConnection.getConnection();PreparedStatement ps=conn.prepareStatement(
                "SELECT id,COALESCE(SUM(quantity*discount_price),0) AS total FROM inventory_entries WHERE date=? AND type='opening' GROUP BY id ORDER BY id")){
            ps.setString(1,currentDate.toString());ResultSet rs=ps.executeQuery();
            while(rs.next()){long t=rs.getLong("total");total+=t;listModel.addElement(new PhieuEntry(rs.getInt("id"),currentDate,"PN"+String.format("%05d",rs.getInt("id")),t));}
        }catch(Exception ex){ex.printStackTrace();}
        if(lblTongCong!=null)lblTongCong.setText(NF.format(total));
        if(lblDateLeft!=null)lblDateLeft.setText(currentDate.format(DATE_FMT));
    }

    private void loadPhieuIntoForm(PhieuEntry entry){
        editingId=entry.id;lblSoPhieu.setText(entry.soPhieu);txtNgay.setText(currentDate.format(DATE_FMT));
        itemModel.setRowCount(0);
        try(Connection conn=DBConnection.getConnection();PreparedStatement ps=conn.prepareStatement(
                "SELECT ie.quantity,ie.discount_price,ct.name,ct.denomination,ct.id as ct_id FROM inventory_entries ie JOIN card_types ct ON ct.id=ie.card_type_id WHERE ie.id=? ORDER BY ct.id")){
            ps.setInt(1,entry.id);ResultSet rs=ps.executeQuery();
            while(rs.next()){
                int qty=rs.getInt("quantity"),denom=rs.getInt("denomination"),ckPri=rs.getInt("discount_price");
                double ck=denom>0?Math.round((1.0-(double)ckPri/denom)*1000.0)/10.0:0;
                CardType ct2=findById(rs.getInt("ct_id"));
                itemModel.addRow(new Object[]{ct2!=null?ct2.code:"",rs.getString("name"),qty,denom,ck,(long)qty*ckPri});
            }
        }catch(Exception ex){ex.printStackTrace();}
        addEmptyRow();refreshTotals();
    }

    private void savePhieu(){
        if(itemTable.isEditing())itemTable.getCellEditor().stopCellEditing();
        List<int[]> rows=new ArrayList<>();
        for(int i=0;i<itemModel.getRowCount();i++){
            String code=itemModel.getValueAt(i,COL_CODE).toString().trim().toUpperCase();if(code.isEmpty())continue;
            int qty=toInt(itemModel.getValueAt(i,COL_QTY));if(qty<=0)continue;
            CardType ct=codeMap.get(code);if(ct==null)continue;
            double ck=toDbl(itemModel.getValueAt(i,COL_CK));
            int ckPrice=(int)Math.round(ct.denomination*(1.0-ck/100.0));
            rows.add(new int[]{ct.id,qty,ct.denomination,ckPrice});
        }
        if(rows.isEmpty()){showError("Chưa nhập dòng nào hợp lệ!");return;}
        try(Connection conn=DBConnection.getConnection()){
            conn.setAutoCommit(false);
            try{
                if(editingId!=null)conn.prepareStatement("DELETE FROM inventory_entries WHERE id="+editingId).executeUpdate();
                PreparedStatement ps=conn.prepareStatement("INSERT INTO inventory_entries(date,type,card_type_id,quantity,price,discount_price,supplier_name,supplier_phone,supplier_address) VALUES(?,'opening',?,?,?,?,'','','')");
                for(int[] r:rows){ps.setString(1,currentDate.toString());ps.setInt(2,r[0]);ps.setInt(3,r[1]);ps.setInt(4,r[2]);ps.setInt(5,r[3]);ps.addBatch();}
                ps.executeBatch();conn.commit();
                JOptionPane.showMessageDialog(this,"✅ Lưu phiếu thành công!","OK",JOptionPane.INFORMATION_MESSAGE);
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

    private void pickDate(){
        String in=JOptionPane.showInputDialog(this,"Nhập ngày (dd/MM/yyyy):",currentDate.format(DATE_FMT));
        if(in==null||in.isBlank())return;
        try{currentDate=LocalDate.parse(in.trim(),DATE_FMT);loadPhieuList();}catch(Exception ex){showError("Ngày không hợp lệ!");}
    }

    private void shiftDate(int days){
        currentDate=currentDate.plusDays(days);
        if(lblDateLeft!=null)lblDateLeft.setText(currentDate.format(DATE_FMT));
        if(txtNgay!=null)txtNgay.setText(currentDate.format(DATE_FMT));
        loadPhieuList();
    }

    // ═══ PRINT PREVIEW ════════════════════════════════════════════════════════

    private void showPrintPreview(){
        if(itemTable.isEditing())itemTable.getCellEditor().stopCellEditing();
        String soPhieu=lblSoPhieu!=null?lblSoPhieu.getText():"";
        String ngay=txtNgay!=null?txtNgay.getText():currentDate.format(DATE_FMT);
        String ghiChu=txtGhiChu!=null?txtGhiChu.getText():"";

        List<InvoiceBuilder.OpeningItem> items=new ArrayList<>();
        for(int i=0;i<itemModel.getRowCount();i++){
            String code=itemModel.getValueAt(i,COL_CODE).toString().trim();if(code.isEmpty())continue;
            int qty=toInt(itemModel.getValueAt(i,COL_QTY));if(qty<=0)continue;
            InvoiceBuilder.OpeningItem it=new InvoiceBuilder.OpeningItem();
            it.name=itemModel.getValueAt(i,COL_NAME).toString();
            it.quantity=qty;
            it.unitPrice=toInt(itemModel.getValueAt(i,COL_PRICE));
            it.ckPercent=toDbl(itemModel.getValueAt(i,COL_CK));
            it.lineTotal=toLong(itemModel.getValueAt(i,COL_TOTAL));
            items.add(it);
        }
        String html=InvoiceBuilder.buildOpeningInvoice(soPhieu,ngay,ghiChu,items);
        PrintPreviewDialog.show(this,"Phiếu tồn đầu "+soPhieu,html);
    }

    // ═══ DATA CLASS + RENDERER ════════════════════════════════════════════════

    private static class PhieuEntry{
        int id;LocalDate date;String soPhieu;long total;
        PhieuEntry(int id,LocalDate d,String sp,long t){this.id=id;date=d;soPhieu=sp;total=t;}
    }

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

    private class F1Renderer extends DefaultTableCellRenderer{
        @Override public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int row,int col){
            if(val!=null){
                if(col==COL_PRICE||col==COL_TOTAL){try{val=NF.format(Long.parseLong(val.toString()));}catch(Exception ignore){}}
                if(col==COL_CK){try{val=toDbl(val)+"%";}catch(Exception ignore){}}
            }
            super.getTableCellRendererComponent(t,val,sel,foc,row,col);
            setFont(F_TABLE);setBorder(new EmptyBorder(0,4,0,4));
            if(sel){setBackground(C_YELLOW_H);setForeground(Color.BLACK);}
            else{setBackground(row%2==0?C_ROW1:C_ROW2);setForeground(col==COL_TOTAL?C_TT_RED:Color.BLACK);}
            setHorizontalAlignment(col>=COL_QTY?SwingConstants.RIGHT:SwingConstants.LEFT);
            if(col==COL_CK)setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }
}