package ui;

import ui.tabs.*;

import javax.swing.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Quản Lý Card Đại Lý");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("F1 - Tồn đầu", new F1OpeningPanel());
        tabbedPane.addTab("F2 - Nhập hàng", new F2ReceivedPanel());
        tabbedPane.addTab("F3 - Bán hàng", new F3OrdersPanel());
        tabbedPane.addTab("F4 - Tồn cuối", new F4ClosingPanel());
        tabbedPane.addTab("F5 - Báo cáo", new F5ReportsPanel());
        tabbedPane.addTab("F6 - Bảng giá", new F6PricePanel());

        add(tabbedPane);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}