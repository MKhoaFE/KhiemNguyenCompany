package ui.tabs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Màn hình chào mừng — hiển thị khi mở app.
 * Background gradient xanh đậm, đồng hồ số, tên phần mềm.
 */
public class WelcomePanel extends JPanel {

    private static final Color C_BG_TOP    = new Color(0x0D1B2A);
    private static final Color C_BG_BOT    = new Color(0x1B3A5C);
    private static final Color C_ACCENT    = new Color(0x4FC3F7);
    private static final Color C_GOLD      = new Color(0xFFD54F);
    private static final Color C_WHITE     = Color.WHITE;
    private static final Color C_SUBTITLE  = new Color(0xB0BEC5);
    private static final Color C_CARD_BG   = new Color(0x1E3A55);
    private static final Color C_CARD_BORD = new Color(0x2E6A9E);

    // Khai báo trước — PHẢI được gán trước khi timer chạy
    private final JLabel lblTime = new JLabel("--:--:--", SwingConstants.CENTER);
    private final JLabel lblDate = new JLabel("", SwingConstants.CENTER);

    public WelcomePanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        // 1. Thêm card vào panel (lblTime/lblDate đã được khởi tạo ở trên)
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;
        gc.anchor = GridBagConstraints.CENTER;
        add(buildCenterCard(), gc);

        // 2. Cập nhật lần đầu
        updateDateTime();

        // 3. Bắt đầu timer
        new Timer(1000, e -> updateDateTime()).start();
    }

    // ── Background gradient ───────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Gradient
        GradientPaint gp = new GradientPaint(0, 0, C_BG_TOP, 0, getHeight(), C_BG_BOT);
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Vòng tròn trang trí
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.07f));
        g2.setColor(C_ACCENT);
        g2.fillOval(-120, -120, 400, 400);
        g2.fillOval(getWidth() - 200, getHeight() - 200, 500, 500);
        g2.fillOval(getWidth() / 2 - 60, getHeight() - 100, 300, 300);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // Lưới mờ
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.04f));
        g2.setColor(C_WHITE);
        g2.setStroke(new BasicStroke(1f));
        for (int x = 0; x < getWidth(); x += 60)  g2.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += 60)  g2.drawLine(0, y, getWidth(), y);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    // ── Card trung tâm ────────────────────────────────────────────────────────
    private JPanel buildCenterCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
                g2.setColor(C_CARD_BORD);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 24, 24));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(50, 70, 50, 70));
        card.setPreferredSize(new Dimension(640, 520));

        // Icon
        JLabel icon = new JLabel("🏪", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 72));
        icon.setAlignmentX(CENTER_ALIGNMENT);

        // Tên đại lý
        JLabel lblCompany = new JLabel("ĐẠI LÝ SIM CARD 66 LÝ NAM ĐẾ", SwingConstants.CENTER);
        lblCompany.setFont(new Font("Arial", Font.BOLD, 18));
        lblCompany.setForeground(C_GOLD);
        lblCompany.setAlignmentX(CENTER_ALIGNMENT);

        // Tiêu đề chính
        JLabel lblTitle = new JLabel("PHẦN MỀM QUẢN LÝ BÁN HÀNG", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 28));
        lblTitle.setForeground(C_WHITE);
        lblTitle.setAlignmentX(CENTER_ALIGNMENT);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(C_CARD_BORD);
        sep.setMaximumSize(new Dimension(420, 2));
        sep.setAlignmentX(CENTER_ALIGNMENT);

        // Subtitle
        JLabel lblSub = new JLabel("Quản lý nhập · xuất · tồn kho · hóa đơn · báo cáo", SwingConstants.CENTER);
        lblSub.setFont(new Font("Arial", Font.ITALIC, 14));
        lblSub.setForeground(C_SUBTITLE);
        lblSub.setAlignmentX(CENTER_ALIGNMENT);

        // Đồng hồ — dùng field đã khai báo ở class level
        lblTime.setFont(new Font("Arial", Font.BOLD, 52));
        lblTime.setForeground(C_ACCENT);
        lblTime.setAlignmentX(CENTER_ALIGNMENT);

        lblDate.setFont(new Font("Arial", Font.PLAIN, 15));
        lblDate.setForeground(C_SUBTITLE);
        lblDate.setAlignmentX(CENTER_ALIGNMENT);

        // Version
        JLabel lblVer = new JLabel("v1.0  ·  CÔNG TY TNHH KHIÊM NGUYỄN", SwingConstants.CENTER);
        lblVer.setFont(new Font("Arial", Font.PLAIN, 12));
        lblVer.setForeground(new Color(0x546E7A));
        lblVer.setAlignmentX(CENTER_ALIGNMENT);

        card.add(icon);
        card.add(Box.createVerticalStrut(6));
        card.add(lblCompany);
        card.add(Box.createVerticalStrut(8));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(22));
        card.add(sep);
        card.add(Box.createVerticalStrut(22));
        card.add(lblSub);
        card.add(Box.createVerticalStrut(28));
        card.add(lblTime);
        card.add(Box.createVerticalStrut(6));
        card.add(lblDate);
        card.add(Box.createVerticalStrut(28));
        card.add(lblVer);

        return card;
    }

    // ── Cập nhật đồng hồ ─────────────────────────────────────────────────────
    private void updateDateTime() {
        java.time.LocalTime now = java.time.LocalTime.now();
        lblTime.setText(String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond()));

        LocalDate today = LocalDate.now();
        String dow = today.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("vi"));
        dow = Character.toUpperCase(dow.charAt(0)) + dow.substring(1);
        lblDate.setText(dow + ", ngày " + today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }
}
