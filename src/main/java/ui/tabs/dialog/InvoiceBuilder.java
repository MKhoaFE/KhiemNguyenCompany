package ui.tabs;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Builds HTML invoice/receipt strings for print-preview.
 * All methods return a self-contained HTML string suitable for JEditorPane.
 *
 * Layout matches the sample image:
 *   - Company header (bold, centered)
 *   - Address / phone line
 *   - Horizontal rule
 *   - Title (HÓA ĐƠN BÁN HÀNG / PHIẾU NHẬP HÀNG / …)
 *   - Meta fields (Số HĐ, Ngày, Khách hàng …)
 *   - Bordered table with STT | columns | Thành tiền
 *   - TỔNG CỘNG row
 *   - Signature block
 */
public class InvoiceBuilder {

    private static final NumberFormat NF = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public static final String COMPANY_NAME    = "ĐẠI LÝ SIM CARD 66 LÝ NAM ĐẾ";
    public static final String COMPANY_FULL    = "CÔNG TY TNHH KHIÊM NGUYỄN";
    public static final String COMPANY_ADDRESS = "66 Lý Nam Đế";
    public static final String COMPANY_PHONE   = "";   // fill if needed

    // ─────────────────────────────────────────────────────────────────────────
    // F3 — Hóa đơn bán hàng
    // ─────────────────────────────────────────────────────────────────────────

    public static class SaleItem {
        public String name;
        public int    denomination;
        public int    quantity;
        public int    discountPrice;
        public double ckPercent;
        public long   lineTotal;
    }

    /**
     * @param orderId   null → "---"
     * @param date      e.g. "09/05/2026"
     * @param buyerName may be blank
     * @param items     list of SaleItem
     */
    public static String buildSaleInvoice(Integer orderId, String date,
                                          String buyerName, String buyerPhone,
                                          String buyerAddress,
                                          List<SaleItem> items) {
        String soHd = orderId != null ? "#" + orderId : "---";
        long grand  = items.stream().mapToLong(i -> i.lineTotal).sum();

        StringBuilder rows = new StringBuilder();
        int stt = 1;
        for (SaleItem it : items) {
            if (it.quantity <= 0) continue;
            rows.append("<tr>")
                    .append(td(String.valueOf(stt++)))
                    .append(td(esc(it.name)))
                    .append(tdR(fmt(it.denomination) + "đ"))
                    .append(tdR(String.valueOf(it.quantity)))
                    .append(tdR(fmt(it.discountPrice) + "đ"))
                    .append(tdR(pct(it.ckPercent)))
                    .append(tdR("<b>" + fmt(it.lineTotal) + "đ</b>"))
                    .append("</tr>\n");
        }

        // buyer meta
        StringBuilder meta = new StringBuilder();
        meta.append("<p style='margin:4px 0;font-size:13px;'>");
        meta.append("Số HĐ: <b>").append(soHd).append("</b>&nbsp;&nbsp;&nbsp;");
        meta.append("Ngày: <b>").append(esc(date)).append("</b>");
        meta.append("</p>\n");
        if (buyerName != null && !buyerName.isBlank())
            meta.append("<p style='margin:4px 0;font-size:13px;'>Khách hàng: <b>").append(esc(buyerName)).append("</b></p>\n");
        if (buyerPhone != null && !buyerPhone.isBlank())
            meta.append("<p style='margin:4px 0;font-size:13px;'>Điện thoại: ").append(esc(buyerPhone)).append("</p>\n");
        if (buyerAddress != null && !buyerAddress.isBlank())
            meta.append("<p style='margin:4px 0;font-size:13px;'>Địa chỉ: ").append(esc(buyerAddress)).append("</p>\n");

        String tableHeader =
                "<tr style='background:#f0f0f0;'>"
                        + th("STT") + th("Loại card") + thR("Mệnh giá") + thR("SL")
                        + thR("Giá CK") + thR("% CK") + thR("Thành tiền")
                        + "</tr>";

        String totalRow =
                "<tr><td colspan='6' align='right' style='border:1px solid #999;padding:5px 8px;'>"
                        + "<b>TỔNG CỘNG</b></td>"
                        + "<td align='right' style='border:1px solid #999;padding:5px 8px;'>"
                        + "<b>" + fmt(grand) + "đ</b></td></tr>";

        return html(
                companyHeader(COMPANY_NAME, COMPANY_ADDRESS, COMPANY_PHONE)
                        + "<hr style='border:1px solid #333;margin:8px 0;'/>\n"
                        + "<h2 style='text-align:center;font-size:16px;margin:10px 0;'>HÓA ĐƠN BÁN HÀNG</h2>\n"
                        + meta
                        + "<br/>\n"
                        + "<table width='100%' cellspacing='0' cellpadding='0' style='border-collapse:collapse;font-size:13px;'>\n"
                        + tableHeader + "\n"
                        + rows
                        + totalRow
                        + "</table>\n"
                        + signatureBlock("Người mua hàng", "Người bán hàng",
                        buyerName != null ? buyerName : "", COMPANY_FULL)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // F2 — Phiếu nhập hàng
    // ─────────────────────────────────────────────────────────────────────────

    public static class ReceiptItem {
        public String name;
        public int    quantity;
        public int    unitPrice;
        public double ckPercent;
        public long   lineTotal;
    }

    public static String buildReceiptInvoice(String soPhieu, String date,
                                             String supplier, String note,
                                             List<ReceiptItem> items) {
        long grand = items.stream().mapToLong(i -> i.lineTotal).sum();

        StringBuilder meta = new StringBuilder();
        meta.append("<p style='margin:4px 0;font-size:13px;'>")
                .append("Số phiếu: <b>").append(esc(soPhieu)).append("</b>&nbsp;&nbsp;&nbsp;")
                .append("Ngày nhập: <b>").append(esc(date)).append("</b></p>\n");
        if (supplier != null && !supplier.isBlank())
            meta.append("<p style='margin:4px 0;font-size:13px;'>Nhà cung cấp: <b>")
                    .append(esc(supplier)).append("</b></p>\n");
        if (note != null && !note.isBlank())
            meta.append("<p style='margin:4px 0;font-size:13px;'>Ghi chú: ")
                    .append(esc(note)).append("</p>\n");

        StringBuilder rows = new StringBuilder();
        int stt = 1;
        for (ReceiptItem it : items) {
            if (it.quantity <= 0) continue;
            rows.append("<tr>")
                    .append(td(String.valueOf(stt++)))
                    .append(td(esc(it.name)))
                    .append(tdR(String.valueOf(it.quantity)))
                    .append(tdR(fmt(it.unitPrice) + "đ"))
                    .append(tdR(pct(it.ckPercent)))
                    .append(tdR("<b>" + fmt(it.lineTotal) + "đ</b>"))
                    .append("</tr>\n");
        }

        String tableHeader =
                "<tr style='background:#f0f0f0;'>"
                        + th("STT") + th("Tên hàng") + thR("Số lượng")
                        + thR("Đơn giá") + thR("% CK") + thR("Thành tiền")
                        + "</tr>";

        String totalRow =
                "<tr><td colspan='5' align='right' style='border:1px solid #999;padding:5px 8px;'>"
                        + "<b>TỔNG CỘNG</b></td>"
                        + "<td align='right' style='border:1px solid #999;padding:5px 8px;'>"
                        + "<b>" + fmt(grand) + "đ</b></td></tr>";

        return html(
                companyHeader(COMPANY_NAME, COMPANY_ADDRESS, COMPANY_PHONE)
                        + "<hr style='border:1px solid #333;margin:8px 0;'/>\n"
                        + "<h2 style='text-align:center;font-size:16px;margin:10px 0;'>PHIẾU NHẬP HÀNG</h2>\n"
                        + meta
                        + "<br/>\n"
                        + "<table width='100%' cellspacing='0' cellpadding='0' style='border-collapse:collapse;font-size:13px;'>\n"
                        + tableHeader + "\n"
                        + rows
                        + totalRow
                        + "</table>\n"
                        + signatureBlock("Người lập phiếu", "Người giao hàng",
                        "", supplier != null ? supplier : "")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // F1 — Phiếu tồn đầu kỳ
    // ─────────────────────────────────────────────────────────────────────────

    public static class OpeningItem {
        public String name;
        public int    quantity;
        public int    unitPrice;
        public double ckPercent;
        public long   lineTotal;
    }

    public static String buildOpeningInvoice(String soPhieu, String date,
                                             String note, List<OpeningItem> items) {
        long grand = items.stream().mapToLong(i -> i.lineTotal).sum();

        StringBuilder meta = new StringBuilder();
        meta.append("<p style='margin:4px 0;font-size:13px;'>")
                .append("Số phiếu: <b>").append(esc(soPhieu)).append("</b>&nbsp;&nbsp;&nbsp;")
                .append("Ngày: <b>").append(esc(date)).append("</b></p>\n");
        if (note != null && !note.isBlank())
            meta.append("<p style='margin:4px 0;font-size:13px;'>Ghi chú: ").append(esc(note)).append("</p>\n");

        StringBuilder rows = new StringBuilder();
        int stt = 1;
        for (OpeningItem it : items) {
            if (it.quantity <= 0) continue;
            rows.append("<tr>")
                    .append(td(String.valueOf(stt++)))
                    .append(td(esc(it.name)))
                    .append(tdR(String.valueOf(it.quantity)))
                    .append(tdR(fmt(it.unitPrice) + "đ"))
                    .append(tdR(pct(it.ckPercent)))
                    .append(tdR("<b>" + fmt(it.lineTotal) + "đ</b>"))
                    .append("</tr>\n");
        }

        String tableHeader =
                "<tr style='background:#f0f0f0;'>"
                        + th("STT") + th("Tên hàng") + thR("Số lượng")
                        + thR("Đơn giá") + thR("% CK") + thR("Thành tiền")
                        + "</tr>";

        String totalRow =
                "<tr><td colspan='5' align='right' style='border:1px solid #999;padding:5px 8px;'>"
                        + "<b>TỔNG CỘNG</b></td>"
                        + "<td align='right' style='border:1px solid #999;padding:5px 8px;'>"
                        + "<b>" + fmt(grand) + "đ</b></td></tr>";

        return html(
                companyHeader(COMPANY_NAME, COMPANY_ADDRESS, COMPANY_PHONE)
                        + "<hr style='border:1px solid #333;margin:8px 0;'/>\n"
                        + "<h2 style='text-align:center;font-size:16px;margin:10px 0;'>PHIẾU TỒN ĐẦU KỲ</h2>\n"
                        + meta
                        + "<br/>\n"
                        + "<table width='100%' cellspacing='0' cellpadding='0' style='border-collapse:collapse;font-size:13px;'>\n"
                        + tableHeader + "\n"
                        + rows
                        + totalRow
                        + "</table>\n"
                        + signatureBlock("Người lập phiếu", "Phụ trách kho", "", "")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared HTML fragments
    // ─────────────────────────────────────────────────────────────────────────

    private static String companyHeader(String name, String address, String phone) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1 style='text-align:center;font-size:18px;margin:0 0 4px 0;'>")
                .append(esc(name)).append("</h1>\n");
        StringBuilder sub = new StringBuilder();
        if (address != null && !address.isBlank()) sub.append("Địa chỉ: ").append(esc(address));
        if (phone   != null && !phone.isBlank())   { if (sub.length()>0) sub.append("&nbsp;&nbsp;|&nbsp;&nbsp;"); sub.append("ĐT: ").append(esc(phone)); }
        if (sub.length() > 0)
            sb.append("<p style='text-align:center;font-size:13px;margin:0;'>").append(sub).append("</p>\n");
        return sb.toString();
    }

    private static String signatureBlock(String leftTitle, String rightTitle,
                                         String leftName,  String rightName) {
        return "<br/><br/>\n"
                + "<table width='100%' style='font-size:13px;'><tr>\n"
                + "<td width='50%' align='center'><b>" + esc(leftTitle) + "</b><br/>"
                + "<i style='font-size:11px;'>(Ký, ghi rõ họ tên)</i><br/><br/><br/>"
                + "<u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u><br/>"
                + (leftName.isBlank()  ? "" : esc(leftName))
                + "</td>\n"
                + "<td width='50%' align='center'><b>" + esc(rightTitle) + "</b><br/>"
                + "<i style='font-size:11px;'>(Ký, ghi rõ họ tên)</i><br/><br/><br/>"
                + "<u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u><br/>"
                + (rightName.isBlank() ? "" : esc(rightName))
                + "</td>\n</tr></table>\n"
                + "<p style='text-align:center;font-size:12px;margin-top:16px;'>"
                + "<i>Cảm ơn quý khách đã mua hàng!</i></p>\n";
    }

    // Table cell helpers
    private static final String CELL_STYLE  = "border:1px solid #999;padding:5px 8px;";
    private static final String HEADER_STYLE = "border:1px solid #999;padding:5px 8px;background:#f0f0f0;";

    private static String td(String v)  { return "<td style='" + CELL_STYLE + "'>"  + v + "</td>"; }
    private static String tdR(String v) { return "<td align='right' style='" + CELL_STYLE + "'>" + v + "</td>"; }
    private static String th(String v)  { return "<th style='" + HEADER_STYLE + "'>" + v + "</th>"; }
    private static String thR(String v) { return "<th align='right' style='" + HEADER_STYLE + "'>" + v + "</th>"; }

    // Wrap in minimal HTML document
    private static String html(String body) {
        return "<html><head>"
                + "<meta charset='UTF-8'/>"
                + "<style>body{font-family:Arial,sans-serif;font-size:13px;margin:0;padding:0;}"
                + "table{width:100%;}"
                + "</style></head>"
                + "<body>" + body + "</body></html>";
    }

    // Formatting helpers
    static String fmt(long v)    { return NF.format(v); }
    static String fmt(int v)     { return NF.format(v); }
    static String pct(double v)  { return (v == (long)v ? String.valueOf((long)v) : String.valueOf(v)) + "%"; }
    static String esc(String s)  {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}