package ui.tabs;

import javax.swing.*;
import java.awt.*;

public class F3OrdersPanel extends JPanel {

    public F3OrdersPanel() {
        setLayout(new BorderLayout());

        JLabel label = new JLabel("F1 - Tồn đầu", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}