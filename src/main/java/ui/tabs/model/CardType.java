package ui.tabs.model;

public class CardType {
    public int id;
    public String name;
    public int denomination;
    public int defaultPrice;
    public int defaultDiscount;
    @Override
    public String toString() {
        return name;
    }
}
