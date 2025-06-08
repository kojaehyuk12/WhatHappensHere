package io.github.mygame;

import java.util.HashSet;
import java.util.Set;

public class Inventory {
    private static Inventory instance = null;
    private Set<String> items;

    private Inventory() {
        items = new HashSet<>();
    }

    public static Inventory getInstance() {
        if (instance == null) {
            instance = new Inventory();
        }
        return instance;
    }

    public boolean hasItem(String itemName) {
        return items.contains(itemName);
    }

    public void addItem(String itemName) {
        items.add(itemName);
    }

    public void removeItem(String itemName) {
        items.remove(itemName);
    }

    public Set<String> getItems() {
        return items;
    }
}
