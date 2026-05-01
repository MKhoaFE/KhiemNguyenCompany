package ui.tabs.dao;

import db.DBConnection;
import ui.tabs.model.CardType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CardTypeDAO {

    public List<CardType> getAll() {
        List<CardType> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM card_types ORDER BY id");

            while (rs.next()) {
                CardType c = new CardType();
                c.id = rs.getInt("id");
                c.name = rs.getString("name");
                c.denomination = rs.getInt("denomination");
                c.defaultPrice = rs.getInt("default_price");
                c.defaultDiscount = rs.getInt("default_discount");

                list.add(c);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}