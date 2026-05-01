package ui.tabs;

import javax.swing.*;
import java.awt.*;

public class F1OpeningPanel extends JPanel {

    public F1OpeningPanel() {
        setLayout(new BorderLayout());

        JLabel label = new JLabel("F1 - Tồn đầu", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}