package restaurant;

public class Dish {
    public final String name;
    public int price, amount, time;

    public Dish(String name, int price, int amount, int time) {
        this.name = name;
        this.price = price;
        this.amount = amount;
        this.time = time;
    }
}
