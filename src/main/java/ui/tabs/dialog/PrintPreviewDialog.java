package ui.tabs;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.print.*;

/**
 * Print-preview dialog — renders HTML so the layout matches the printed invoice.
 * Usage:
 *   PrintPreviewDialog.show(parentComponent, "Tiêu đề cửa sổ", htmlContent);
 */
public class PrintPreviewDialog extends JDialog {

    private final String htmlContent;

    public PrintPreviewDialog(Frame owner, String windowTitle, String html) {
        super(owner, windowTitle, true);
        this.htmlContent = html;

        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(new EmptyBorder(12, 14, 12, 14));

        // ── Paper-white preview ─────────────────────────────────────────
        JEditorPane editor = new JEditorPane("text/html", html);
        editor.setEditable(false);
        editor.setBackground(Color.WHITE);
        editor.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel paper = new JPanel(new BorderLayout());
        paper.setBackground(Color.WHITE);
        paper.setBorder(BorderFactory.createLineBorder(new Color(0xBBBBBB), 1));
        paper.add(editor, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(paper);
        scroll.setPreferredSize(new Dimension(660, 700));
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(0xDDDDDD));
        add(scroll, BorderLayout.CENTER);

        // ── Buttons ─────────────────────────────────────────────────────
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton btnPrint = mkBtn("🖨  In ngay",  new Color(0x1565C0), Color.WHITE);
        JButton btnClose = mkBtn("✖  Đóng",      new Color(0x90A4AE), Color.WHITE);
        btnPrint.addActionListener(e -> { doPrint(); dispose(); });
        btnClose.addActionListener(e -> dispose());
        btns.add(btnClose);
        btns.add(btnPrint);
        add(btns, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(680, 540));
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    /** Convenience: open from any component. */
    public static void show(Component parent, String title, String html) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(parent);
        new PrintPreviewDialog(owner, title, html).setVisible(true);
    }

    // ── Printing via JEditorPane HTML renderer ───────────────────────────────
    private void doPrint() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf  = job.defaultPage();
        Paper paper = new Paper();
        double a4w = 8.27 * 72, a4h = 11.69 * 72;
        paper.setSize(a4w, a4h);
        paper.setImageableArea(36, 36, a4w - 72, a4h - 72);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);

        final JEditorPane ep = new JEditorPane("text/html", htmlContent);
        ep.setSize((int)(a4w - 72), Integer.MAX_VALUE);
        ep.setSize((int)(a4w - 72), ep.getPreferredSize().height);

        job.setPrintable((g, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            ep.print(g2);
            return Printable.PAGE_EXISTS;
        }, pf);

        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi in: " + ex.getMessage());
            }
        }
    }

    private JButton mkBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(new Font("Arial", Font.BOLD, 13)); b.setForeground(fg);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        return b;
    }
}