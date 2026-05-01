package ui.tabs;

import javax.swing.*;
import java.awt.*;

public class F4ClosingPanel extends JPanel {

    public F4ClosingPanel() {
        setLayout(new BorderLayout());

        JLabel label = new JLabel("F1 - Tồn đầu", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}