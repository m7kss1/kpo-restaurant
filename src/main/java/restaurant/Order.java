package restaurant;

import java.util.*;

public class Order {
    private static final Random RANDOM = new Random();

    public final int number = RANDOM.nextInt(999999);
    public final Map<Dish, DishState> dishes = new HashMap<>();

    public boolean started, ready, paid, cancelled;

    public void addDish(Dish dish, int amount) {
        if (dishes.containsKey(dish)) {
            var state = dishes.get(dish);
            state.amount += amount;

            if (state.ready)
                state.startCooking();

            return;
        }

        dishes.put(dish, new DishState(dish, amount) {{
            if (started)
                startCooking();
        }});
    }

    public void start() {
        dishes.forEach((dish, state) -> state.startCooking());
        started = true;
    }

    public void cancel() {
        dishes.forEach((dish, state) -> state.cancelCooking());
        cancelled = true;
    }

    private void checkReady() {
        if (cancelled) return;

        for (var entry : dishes.entrySet()) {
            if (entry.getValue().ready)
                continue;

            return;
        }

        ready = true;
        System.out.println("Заказ #" + number + " готов к выдаче!");
    }

    public class DishState {
        public final Dish dish;

        public int amount, finished;
        public boolean ready;

        private transient Thread cookingThread;

        public DishState(Dish dish, int amount) {
            this.dish = dish;
            this.amount = amount;
        }

        public void startCooking() {
            ready = false;

            cookingThread = new Thread(() -> {
                while (finished < amount) {
                    try {
                        Thread.sleep(dish.time * 1000L);
                    } catch (InterruptedException ignored) {}

                    finished++;
                }

                ready = true;
                checkReady();
            });

            cookingThread.setDaemon(true);
            cookingThread.start();
        }

        public void cancelCooking() {
            cookingThread.interrupt();
        }
    }
}
