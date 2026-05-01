package ui;

import db.DatabaseInit;
import ui.tabs.*;

import javax.swing.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Quản Lý Card Đại Lý");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        tabs.add("F1 - Tồn đầu", new F1OpeningPanel());
        tabs.add("F2 - Nhập", new F2ReceivedPanel());
        tabs.add("F3 - Bán", new F3OrdersPanel());
        tabs.add("F4 - Tồn cuối", new F4ClosingPanel());
        tabs.add("F5 - Báo cáo", new F5ReportsPanel());
        tabs.add("F6 - Bảng giá", new F6PricePanel());

        add(tabs);
    }

    public static void main(String[] args) {
        DatabaseInit.init();

        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}