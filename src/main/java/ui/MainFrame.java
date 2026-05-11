package ui;

import db.DatabaseInit;
import ui.tabs.*;

import javax.swing.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Quản Lý Card Đại Lý");

        // 🔥 FULLSCREEN
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(false); // true nếu muốn full không viền

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();

        tabs.add("🏠 Trang chủ", new WelcomePanel());
        tabs.add("F1 - Tồn đầu", new F1OpeningPanel());
        tabs.add("F2 - Nhập hàng", new F2ReceivedPanel());
        tabs.add("F3 - Bán hàng", new F3OrdersPanel());
        tabs.add("F4 - Tồn cuối", new F4ClosingPanel());
        tabs.add("F5 - Báo cáo", new F5ReportsPanel());
        tabs.add("F6 - Bảng giá", new F6PricePanel());

        tabs.setSelectedIndex(0); // Mở trang chủ mặc định

        add(tabs);
    }

    public static void main(String[] args) {
        System.out.println("Starting app...");

        try {
            DatabaseInit.init();
            System.out.println("Database initialized.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database init failed: " + e.getMessage());
            return;
        }

        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}