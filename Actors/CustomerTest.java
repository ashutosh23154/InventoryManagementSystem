package Actors;

import Helper.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import Helper.Category;
import java.util.HashMap;
import java.util.Map;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {
    private Customer customer;

    @BeforeEach
    void setCustomer(){
        customer = new Customer("dummy123@iiitd.ac.in", "Dummy Customer", "password123");
    }

    @Test
    void TestCanPlaceOrderWhenAllItemsAreAvailable() {
        Map<Item, Integer> itemsToCheck = new HashMap<>();
        itemsToCheck.put(new Item("Spring Rolls", 150.0f, Category.Starters, true), 2);
        itemsToCheck.put(new Item("Tomato Soup", 120.0f, Category.Soups, true), 1);
        itemsToCheck.put(new Item("Grilled Chicken", 300.0f, Category.MainCourse, true), 3);
        itemsToCheck.put(new Item("Chocolate Cake", 180.0f, Category.Desserts, true), 4);

        Item result = customer.canPlaceOrder(itemsToCheck);

        assertNull(result, "All items are available, should return null.");
        System.out.println("Order can be placed when all items are available");
    }

    @Test
    void TestCanPlaceOrderWhenSomeItemsAreAvailable(){
        Map<Item, Integer> itemsToCheck = new HashMap<>();
        itemsToCheck.put(new Item("Spring Rolls", 150.0f, Category.Starters, true), 2);
        itemsToCheck.put(new Item("Tomato Soup", 120.0f, Category.Soups, false), 1);
        itemsToCheck.put(new Item("Grilled Chicken", 300.0f, Category.MainCourse, true), 3);
        itemsToCheck.put(new Item("Chocolate Cake", 180.0f, Category.Desserts, true), 4);

        Item result = customer.canPlaceOrder(itemsToCheck);

        assertNotNull(result, "There is at least one unavailable item. Should return that item");
        assertEquals("Tomato Soup", result.getName(), "The unavailable item should be tomato soup");
        System.out.println("Order can't be placed when even one item is not available");
    }

    @Test
    void TestCanPlaceOrderWhenNoItemsAreAvailable(){
        Map<Item, Integer> itemsToCheck = new HashMap<>();
        itemsToCheck.put(new Item("Spring Rolls", 150.0f, Category.Starters, false), 2);
        itemsToCheck.put(new Item("Tomato Soup", 120.0f, Category.Soups, false), 1);
        itemsToCheck.put(new Item("Grilled Chicken", 300.0f, Category.MainCourse, false), 3);
        itemsToCheck.put(new Item("Chocolate Cake", 180.0f, Category.Desserts, false), 4);

        Item result = customer.canPlaceOrder(itemsToCheck);

        assertNotNull(result, "All items are unavailable, should return the first unavailable item.");
        assertEquals("Spring Rolls", result.getName(), "The first unavailable item should be Spring Rolls.");
        System.out.println("Order can't be placed when no item is available");
    }

    @Test
    void TestCanPlaceOrderWhenNoItemsToCheck(){
        Map<Item, Integer> itemsToCheck = new HashMap<>();

        Item result = customer.canPlaceOrder(itemsToCheck);

        assertNull(result, "No items to check, should return null");
        System.out.println("Order can be placed when there are no items to check");
    }
}