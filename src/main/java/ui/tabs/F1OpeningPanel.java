package ui.tabs;

import ui.tabs.dao.CardTypeDAO;
import db.DBConnection;
import ui.tabs.model.CardType;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

public class F1OpeningPanel extends JPanel {

    private JComboBox<CardType> cbCard;
    private JTextField txtQty;

    public F1OpeningPanel() {
        setLayout(new FlowLayout());

        cbCard = new JComboBox<>();
        txtQty = new JTextField(10);
        JButton btnSave = new JButton("Lưu");

        add(new JLabel("Sản phẩm:"));
        add(cbCard);
        add(new JLabel("Số lượng:"));
        add(txtQty);
        add(btnSave);

        loadCardTypes();

        btnSave.addActionListener(e -> save());
    }

    private void loadCardTypes() {
        List<CardType> list = new CardTypeDAO().getAll();
        for (CardType c : list) {
            cbCard.addItem(c);
        }
    }

    private void save() {
        try {
            CardType c = (CardType) cbCard.getSelectedItem();
            int qty = Integer.parseInt(txtQty.getText());

            Connection conn = DBConnection.getConnection();

            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO inventory_entries(date, type, card_type_id, quantity, price, discount_price)
                VALUES (?, 'opening', ?, ?, ?, ?)
            """);

            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, c.id);
            ps.setInt(3, qty);
            ps.setInt(4, c.defaultPrice);
            ps.setInt(5, c.defaultDiscount);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Đã lưu!");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}