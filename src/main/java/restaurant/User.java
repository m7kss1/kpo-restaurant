package restaurant;

import java.util.*;

public class User {
    public final String username;
    public final String password;

    public final transient Map<Integer, Order> orders = new HashMap<>();
    public final boolean admin;

    public User(String username, String password) {
        this(username, password, false);
    }

    public User(String username, String password, boolean admin) {
        this.username = username;
        this.password = password;
        this.admin = admin;
    }
}
