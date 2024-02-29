package restaurant;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import restaurant.CommandHandler.ResponseType;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CommandHandler handler = new CommandHandler();
    private static final Gson gson = new Gson();

    private static Map<String, User> users = new HashMap<>();
    private static Map<String, Dish> menu = new HashMap<>();

    private static RestaurantData data = new RestaurantData();

    private static User currentUser;
    private static Order currentOrder;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::save));
    }

    public static void main(String... args) {
        load();
        loadCommands();

        // Значения по умолчанию
        users.put("admin", new User("admin", "1234", true));
        menu.put("salad", new Dish("salad", 123, 456, 789));

        while (scanner.hasNext()) {
            var response = handler.handleMessage(scanner.nextLine());
            if (response.type() == ResponseType.valid) continue;

            switch (response.type()) {
                case fewArguments ->
                        System.out.println("Слишком мало аргументов! Правильно: " + response.runCommand() + " " + response.command().paramText);

                case manyArguments ->
                        System.out.println("Слишком много аргументов! Правильно: " + response.runCommand() + " " + response.command().paramText);

                case unknownCommand -> System.out.println("Неизвестная команда!");
            }
        }
    }

    private static void load() {
        var usersPath = Path.of("users.json");
        var menuPath = Path.of("menu.json");
        var dataPath = Path.of("revenue.json");

        try {
            if (Files.exists(usersPath))
                users = gson.fromJson(Files.readString(usersPath), new TypeToken<Map<String, User>>() {
                }.getType());

            if (Files.exists(menuPath))
                menu = gson.fromJson(Files.readString(menuPath), new TypeToken<Map<String, Dish>>() {
                }.getType());

            if (Files.exists(dataPath))
                data = gson.fromJson(Files.readString(dataPath), RestaurantData.class);

            System.out.println("Data loaded!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        var usersPath = Path.of("users.json");
        var menuPath = Path.of("menu.json");
        var dataPath = Path.of("revenue.json");

        try {
            Files.writeString(usersPath, gson.toJson(users, HashMap.class));
            Files.writeString(menuPath, gson.toJson(menu, HashMap.class));
            Files.writeString(dataPath, gson.toJson(data, RestaurantData.class));

            System.out.println("Data saved!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadCommands() {
        // ОБЫЧНЫЕ КОМАНДЫ
        
        
        handler.register("help", "Показать список всех команд", args -> {
            System.out.println("Список команд:");
            handler.getCommandList().forEach(command -> System.out.println(command.text + (command.paramText.isEmpty() ? "" : " " + command.paramText) + " - " + command.description));
        });

        handler.register("login", "<логин> <пароль>", "Войти в систему", args -> {
            if (!users.containsKey(args[0])) {
                System.out.println("Пользователя с таким именем не существует!");
                return;
            }

            if (!users.get(args[0]).password.equals(args[1])) {
                System.out.println("Неверный логин или пароль!");
                return;
            }

            currentUser = users.get(args[0]);
            System.out.println("Вход в систему успешен!");
        });

        handler.register("logout", "Выйти из системы", args -> {
            currentUser = null;
            System.out.println("Выход из системы успешен!");
        });

        handler.register("register", "<логин> <пароль>", "Зарегистрировать нового пользователя", args -> {
            if (users.containsKey(args[0])) {
                System.out.println("Пользователь с таким именем уже существует!");
                return;
            }

            users.put(args[0], new User(args[0], args[1]));
            System.out.println("Регистрация успешна!");
        });

        handler.register("menu", "Просмотреть меню", args -> {
            System.out.println("Меню ресторана:");

            if (menu.isEmpty())
                System.out.println("<тут ничего нет>");

            menu.forEach((name, dish) -> System.out.println("- " + name + " / Цена: " + dish.price + " / Количество: " + dish.amount + " / Время приготовления: " + dish.time + " сек."));
        });

        handler.register("create-order", "Начать создание заказа", args -> {
            if (currentUser == null) {
                System.out.println("Вы не вошли в систему!");
                return;
            }

            if (currentOrder != null) {
                System.out.println("Вы уже в процессе создания заказа!");
                return;
            }

            currentOrder = new Order();
            System.out.println("Заказ #" + currentOrder.number + " успешно создан!");
        });

        handler.register("select-dish", "<название_блюда> [количество]", "Добавить блюдо в текущий заказ", args -> {
            if (currentUser == null) {
                System.out.println("Вы не вошли в систему!");
                return;
            }

            if (currentOrder == null) {
                System.out.println("Нет никакого текущего заказа!");
                return;
            }

            var dish = menu.get(args[0]);
            if (dish == null) {
                System.out.println("Блюдо с таким названием не найдено!");
                return;
            }

            int amount = 1;
            if (args.length == 2) {
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Количество должно быть числом!");
                    return;
                }
            }

            if (dish.amount < amount) {
                System.out.println("Этого блюда осталось меньше требуемого количества!");
                return;
            }

            dish.amount -= amount;
            currentOrder.addDish(dish, amount);

            System.out.println("В заказ успешно добавлено " + amount + " " + dish.name + "!");
        });

        handler.register("remove-dish", "<название_блюда>", "Удалить блюдо из текущего заказа", args -> {
            if (currentUser == null) {
                System.out.println("Вы не вошли в систему!");
                return;
            }

            if (currentOrder == null) {
                System.out.println("Нет никакого текущего заказа!");
                return;
            }

            var dish = menu.get(args[0]);
            if (dish == null) {
                System.out.println("Блюдо с таким названием не найдено!");
                return;
            }

            var removedDish = currentOrder.dishes.remove(dish);
            if (removedDish == null) {
                System.out.println("Этого блюда не было в заказе!");
                return;
            }

            dish.amount += removedDish.amount;
            System.out.println("Из заказа успешно удалено " + removedDish.amount + " " + dish.name + "!");
        });

        handler.register("finish-order", "Завершить создание заказа", args -> {
            if (currentUser == null) {
                System.out.println("Вы не вошли в систему!");
                return;
            }

            if (currentOrder == null) {
                System.out.println("Нет никакого текущего заказа!");
                return;
            }

            if (currentOrder.dishes.isEmpty()) {
                System.out.println("Вы не выбрали ни одного блюда!");
                return;
            }

            currentUser.orders.put(currentOrder.number, currentOrder);
            currentOrder.start();

            System.out.println("Заказ #" + currentOrder.number + " успешно оформлен!");
            currentOrder = null;
        });

        handler.register("cancel-order", "<номер_заказа>", "Отменить заказ", args -> {
            if (currentUser == null) {
                System.out.println("Вы не вошли в систему!");
                return;
            }

            int number;
            try {
                number = Integer.parseInt(args[0]);
            } catch (Exception e) {
                System.out.println("Номер заказа должен быть числом!");
                return;
            }

            var order = currentUser.orders.get(number);
            if (order == null) {
                System.out.println("Заказ с таким номером не найден иди еще не оформлен!");
                return;
            }

            order.cancel();
            currentUser.orders.remove(order.number);

            System.out.println("Заказ #" + order.number + " успешно отменен!");
        });

        handler.register("add-order-dish", "<номер_заказа> <название_блюда> [количество]", "Добавить блюдо в уже созданный заказ, который находится в процессе приготовления", args -> {
            if (currentUser == null) {
                System.out.println("Вы не вошли в систему!");
                return;
            }

            int number;
            try {
                number = Integer.parseInt(args[0]);
            } catch (Exception e) {
                System.out.println("Номер заказа должен быть числом!");
                return;
            }

            var order = currentUser.orders.get(number);
            if (order == null) {
                System.out.println("Заказ с таким номером не найден иди еще не оформлен!");
                return;
            }

            var dish = menu.get(args[1]);
            if (dish == null) {
                System.out.println("Блюдо с таким названием не найдено!");
                return;
            }

            int amount = 1;
            if (args.length == 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (Exception e) {
                    System.out.println("Количество должно быть числом!");
                    return;
                }
            }

            if (dish.amount < amount) {
                System.out.println("Этого блюда осталось меньше требуемого количества!");
                return;
            }

            dish.amount -= amount;
            order.addDish(dish, amount);

            System.out.println("В заказ #" + order.number + " успешно добавлено " + amount + " " + dish.name + "!");
        });

        handler.register("my-orders", "Отобразить список заказов", args -> {
            if (currentUser == null) {
                System.out.println("Вы не вошли в систему!");
                return;
            }

            System.out.println("Список заказов:");
            currentUser.orders.forEach((number, order) -> System.out.println("#" + order.number + ": " + (order.paid ? "<оплачен>" : order.ready ? "<готов>" : "<готовится>")));

            if (currentOrder == null) {
                if (currentUser.orders.isEmpty())
                    System.out.println("<ничего нет>");
                return;
            }

            System.out.println("Текущий заказ:");
            System.out.println("#" + currentOrder.number + ": <не оформлен>");
        });

        handler.register("pay-order", "<номер_заказа>", "Оплатить готовый заказ", args -> {
            if (currentUser == null) {
                System.out.println("Вы не вошли в систему!");
                return;
            }

            int number;
            try {
                number = Integer.parseInt(args[0]);
            } catch (Exception e) {
                System.out.println("Номер заказа должен быть числом!");
                return;
            }

            var order = currentUser.orders.get(number);
            if (order == null) {
                System.out.println("Заказ с таким номером не найден!");
                return;
            }

            if (!order.ready) {
                System.out.println("Этот заказ еще не готов!");
                return;
            }

            if (order.paid) {
                System.out.println("Этот заказ уже оплачен!");
                return;
            }

            int totalPrice = order.dishes.entrySet()
                    .stream()
                    .mapToInt(entry -> entry.getKey().price * entry.getValue().amount)
                    .sum();

            data.totalRevenue += totalPrice;
            System.out.println("Заказ #" + order.number + " на сумму " + totalPrice + "руб. успешно оплачен!");
        });


        // АДМИНСКИЕ КОМАНДЫ


        handler.register("status", "Показать статус ресторана", args -> {
            if (currentUser == null || !currentUser.admin) {
                System.out.println("Вы не администратор!");
                return;
            }

            System.out.println("Статус ресторана:");
            System.out.println("- Общая выручка: " + data.totalRevenue);
            System.out.println("- Всего пользователей: " + users.size());
            System.out.println("- Всего блюд в меню: " + menu.size());
        });

        handler.register("add-menu-dish", "<название> <цена> <количество> <время_приготовления>", "Добавить новое блюдо в меню", args -> {
            if (currentUser == null || !currentUser.admin) {
                System.out.println("Вы не администратор!");
                return;
            }

            if (menu.containsKey(args[0])) {
                System.out.println("Блюдо с таким названием уже существует!");
                return;
            }

            int price;
            try {
                price = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.out.println("Цена блюда должна быть числом!");
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.out.println("Количество блюда должно быть числом!");
                return;
            }

            int time;
            try {
                time = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.out.println("Время приготовления блюда должно быть числом!");
                return;
            }

            menu.put(args[0], new Dish(args[0], price, amount, time));
            System.out.println("Блюдо успешно добавлено!");
        });

        handler.register("remove-menu-dish", "<название>", "Удалить блюдо из меню", args -> {
            if (currentUser == null || !currentUser.admin) {
                System.out.println("Вы не администратор!");
                return;
            }

            if (!menu.containsKey(args[0])) {
                System.out.println("Блюда с таким названием не существует!");
                return;
            }

            menu.remove(args[0]);
            System.out.println("Блюдо успешно удалено!");
        });

        handler.register("edit-menu-dish", "<название> <цена> <количество> <время_приготовления>", "Отредактировать блюдо из меню", args -> {
            if (currentUser == null || !currentUser.admin) {
                System.out.println("Вы не администратор!");
                return;
            }

            if (!menu.containsKey(args[0])) {
                System.out.println("Блюда с таким названием не существует!");
                return;
            }

            int price;
            try {
                price = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.out.println("Цена блюда должна быть числом!");
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.out.println("Количество блюда должно быть числом!");
                return;
            }

            int time;
            try {
                time = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.out.println("Время приготовления блюда должно быть числом!");
                return;
            }

            var dish = menu.get(args[0]);
            dish.price = price;
            dish.amount = amount;
            dish.time = time;

            System.out.println("Блюдо успешно отредактировано!");
        });
    }
}