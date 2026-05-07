package ui.tabs.dao;

import db.DBConnection;
import ui.tabs.model.CardType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CardTypeDAO {

    public List<CardType> getAll() {
        List<CardType> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM card_types ORDER BY id")) {
            while (rs.next()) {
                CardType c = new CardType();
                c.id           = rs.getInt("id");
                c.name         = rs.getString("name");
                c.denomination = rs.getInt("denomination");
                c.defaultPrice = rs.getInt("default_price");
                c.defaultDiscount = rs.getInt("default_discount");
                try { String code = rs.getString("code"); c.code = (code!=null&&!code.isBlank()) ? code : deriveCode(c.name); }
                catch (Exception e) { c.code = deriveCode(c.name); }
                list.add(c);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Viettel 20.000  → T20
     * Mobifone 50.000 → M50
     * Vinaphone 100.000 → V100
     * Vietnammobile 20.000 → VN20
     */
    public static String deriveCode(String name) {
        if (name == null) return "";
        String n = name.toLowerCase().trim();
        String prefix;
        if (n.contains("vietnammobile") || n.contains("vietnam mobile")) prefix = "VN";
        else if (n.contains("viettel"))                                   prefix = "T";
        else if (n.contains("mobifone") || n.startsWith("mobi"))         prefix = "M";
        else if (n.contains("vinaphone") || n.startsWith("vina"))        prefix = "V";
        else prefix = name.substring(0, 1).toUpperCase();

        // extract digits → convert to thousands
        String digits = name.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return prefix;
        long val = 0;
        try { val = Long.parseLong(digits); } catch (NumberFormatException ignore) {}
        if (val >= 1000) val /= 1000;
        return prefix + val;
    }
}