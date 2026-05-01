package ui.tabs;

import javax.swing.*;
import java.awt.*;

public class F2ReceivedPanel extends JPanel {

    public F2ReceivedPanel() {
        setLayout(new BorderLayout());

        JLabel label = new JLabel("F1 - Tồn đầu", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}