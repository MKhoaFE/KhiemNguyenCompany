package ui.tabs.model;
public class CardType {
    public int id;
    public String code;      // T20, M50, V100, VN20 etc.
    public String name;
    public int denomination;
    public int defaultPrice;
    public int defaultDiscount;
    @Override public String toString() { return name; }
}
