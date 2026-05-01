package ui.tabs;

import ui.tabs.dao.CardTypeDAO;
import ui.tabs.model.CardType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class F6PricePanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    public F6PricePanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{
                "ID", "Tên", "Mệnh giá", "Giá", "Giá CK"
        });

        table = new JTable(model);

        add(new JScrollPane(table), BorderLayout.CENTER);

        loadData();
    }

    private void loadData() {
        CardTypeDAO dao = new CardTypeDAO();
        List<CardType> list = dao.getAll();

        for (CardType c : list) {
            model.addRow(new Object[]{
                    c.id,
                    c.name,
                    c.denomination,
                    c.defaultPrice,
                    c.defaultDiscount
            });
        }
    }
}