package Actors;

import Helper.*;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;

public abstract class User implements Serializable {
    private final String Email, Name;
    private String Password;
    private StringBuilder Notification;
    protected JFrame frame;

    public final static String ANSI_RESET = "\u001B[0m";
    public final static String ANSI_RED = "\u001B[31m";
    public final static String ANSI_GREEN = "\u001B[32m";

    public User(String Email, String Name, String Password){
        this.Email = Email;
        this.Name = Name;
        this.Password = this.encode(Password);
        this.Notification = new StringBuilder();
    }

    protected void GUIBasedCanteenMenu(Map<Category, List<Item>> AllItemsByCategory, JPanel previousScreen) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Canteen Menu");
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
//        heading.setForeground(Color.BLUE);
        panel.add(heading);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        String[] columnNames = { "Category", "Name", "Price", "Available", "Number of Orders" };
        int totalItems = AllItemsByCategory.values().stream().mapToInt(List::size).sum();
        Object[][] data = new Object[totalItems][5];

        int row = 0;
        for (Map.Entry<Category, List<Item>> entry : AllItemsByCategory.entrySet()) {
            Category category = entry.getKey();
            List<Item> items = entry.getValue();

            for (Item item : items) {
                data[row][0] = category.name();
                data[row][1] = item.getName();
                data[row][2] = String.format("%.2f", item.getPrice());
                data[row][3] = item.getIsAvailable() ? "Yes" : "No";
                data[row][4] = item.getNumberOfOrders();
                row++;
            }
        }

        JTable table = new JTable(data, columnNames);
        table.setFont(new Font("Monospaced", Font.PLAIN, 16));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
        table.getTableHeader().setReorderingAllowed(false); // Disable column reordering
        table.setEnabled(false); // Disable cell editing

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(scrollPane);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 18));
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> {
            frame.setContentPane(previousScreen);
            frame.revalidate();
            frame.repaint();
        });

        panel.add(backButton);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }

    public boolean isFrameVisible(){
        return frame.isVisible();
    }

    public JFrame getFrame(){
        return frame;
    }

    public String getEmail() {
        return Email;
    }

    public String getPassword() {
        return this.decode(this.Password);
    }

    public String getName(){
        return Name;
    }

    protected abstract String encode(String Password);

    protected abstract String decode(String Password);

    protected void appendNotification(String toAppend){
        Notification.append(toAppend).append("\n");
    }

    public void ViewAccount() {
        System.out.println("--------------------------");
        System.out.println("       Account Details    ");
        System.out.println("--------------------------");
        System.out.printf("%-10s: %s%n", "Email", Email);
        System.out.printf("%-10s: %s%n", "Name", Name);
        System.out.println("--------------------------");
    }

    public String getNotification(){
        return this.Notification.toString();
    }

    public void clearNotification(){
        this.Notification = new StringBuilder();
    }

    public void ChangePassword(String newPassword){
        this.Password = this.encode(newPassword);
    }

    public static int TakeSingleInput(Scanner sc, int i, int j, String str){
        int input;
        String strInput;
        while(true){
            System.out.print(str);
            strInput = sc.nextLine();
            try{
                input = Integer.parseInt(strInput);
                if(input>=i && input<j){
                    return input;
                }
                else{
                    System.out.println("Please choose a number within the specified range");
                }
            }catch (NumberFormatException e){
                System.out.println(ANSI_RED + "Invalid Integer! Enter a valid integer" + ANSI_RESET);
            }
        }
    }

    protected static ArrayList<Integer> TakeMultipleInputs(Scanner sc, String str){
        String strCourses;
        ArrayList<Integer> courseNumbers;
        System.out.print(str);
        strCourses = sc.nextLine();
        String[] courseSelections = strCourses.split(",");
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_RESET = "\u001B[0m";
        courseNumbers = new ArrayList<>();
        for (String course : courseSelections) {
            try {
                int course_num = Integer.parseInt(course.trim());
                courseNumbers.add(course_num);
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Invalid input: " + course.trim() + " is not a valid integer." + ANSI_RESET);
            }
        }
        return courseNumbers;
    }

    public static Category getCategory(Scanner sc){
        System.out.println("Available categories: ");
        Category[] categories = Category.values();
        int i=1;
        for(Category category: Category.values()){
            System.out.println(i++ + ". " + category);
        }

        System.out.println("Please choose a category from the list above: ");
        int option = TakeSingleInput(sc, 1, i, "Enter the integer associated with an option to choose it: ");
        return categories[option-1];
    }

    private static int PrintList(List<Item> TargetLists, int i){
        for (Item Current : TargetLists) {
            System.out.println(i++);
            Current.ShowInfo();
        }
        return i;
    }

    protected static List<Order> ViewOrdersByStatus(OrderStatus status, java.util.LinkedList<Order> Orders){
        List<Order> toReturn = new ArrayList<>();
        int i=1;
        System.out.println("=======================================");
        System.out.printf("      Order History - %s           \n", status.toString());
        System.out.println("=======================================");
        if(Orders==null){
            System.out.println("No orders found with this status");
            System.out.println("==================================================");
            return toReturn;
        }
        for(Order currentOrder: Orders){
            if(currentOrder.getStatus().equals(status)){
                toReturn.add(currentOrder);
                System.out.printf("Order #%d\n", i++);
                System.out.println("--------------------------------------------------");
                System.out.printf("Order ID         : %s\n", currentOrder.getOrderID());
                System.out.printf("Date             : %s\n", currentOrder.getDate());
                System.out.printf("Special Request  : %s\n", currentOrder.getSpecialRequest());

                System.out.println("Items in this Order:");
                for (Map.Entry<Item, Integer> entry : currentOrder.getItems().entrySet()) {
                    Item currentItem = entry.getKey();
                    int quantity = entry.getValue();
                    float itemTotal = currentItem.getPrice() * quantity;

                    System.out.println("  -------------------------------------");
                    System.out.printf("  Product Name   : %s\n", currentItem.getName());
                    System.out.printf("  Product Price  : Rs. %.2f\n", currentItem.getPrice());
                    System.out.printf("  Quantity       : %d\n", quantity);
                    System.out.printf("  Total          : Rs. %.2f\n", itemTotal);
                    System.out.println("  -------------------------------------");
                }
                System.out.println("--------------------------------------------------");
                System.out.printf("Order Total      : Rs. %.2f\n", currentOrder.getPrice());
                System.out.println("==================================================\n");
            }
        }
        if(toReturn.isEmpty()){
            System.out.println("No orders found with this status");
            System.out.println("==================================================");
        }
        return toReturn;
    }

    protected static int ViewAllItems(Map<Category, List<Item>> AllItemsByCategory){
        int i=1;
        for(Map.Entry<Category, List<Item>> entry: AllItemsByCategory.entrySet()){
            i = PrintList(entry.getValue(), i);
        }
        if(i==1){
            System.out.println("Ooho! There are no items in the canteen! Consider adding some items");
        }
        return i;
    }

    protected static Item getItemFromPrefix(Scanner sc, Map<Category, List<Item>> AllItemsByCategory, Map<String, Item> AllItemsByNames, Trie ItemNames){
        int i = ViewAllItems(AllItemsByCategory);
        if(i==1){
            System.out.println("There are no items to remove from the canteen");
            return null;
        }
        System.out.println("Enter the prefix of the name of the item to choose it from the menu: ");
        String prefix = sc.nextLine();
        List<String> ItemsList = ItemNames.startsWith(prefix);

        if(ItemsList.isEmpty()){
            System.out.println("Ooho! There are no item with prefix " + prefix);
            System.out.println("No item available with this prefix");
            System.out.println("Choose an option:\n1. Try Again with another prefix\n2. Go Back");
            int option = TakeSingleInput(sc, 1, 3, "Enter the serial number associated with an option to choose it: ");
            if(option==1){
                return getItemFromPrefix(sc, AllItemsByCategory, AllItemsByNames, ItemNames);
            } else {
                return null;
            }
        }

        String itemName;

        if(ItemsList.size()>1){
            i=1;
            for(String Current: ItemsList){
                System.out.println(i++ + ". " + Current);
            }

            int index = TakeSingleInput(sc, 1, ItemsList.size()+1, "Enter the serial number associated with an item to remove it from the menu: ");
            itemName = ItemsList.get(index-1);
        } else {
            itemName = ItemsList.getFirst();
        }

        Item foundItem;
        foundItem = AllItemsByNames.get(itemName);
        return foundItem;
    }

    protected static OrderStatus getStatusFromUser(Scanner sc){
        System.out.println("Choose a status among these: ");
        OrderStatus[] statuses = OrderStatus.values();
        int i=1;
        for(OrderStatus status: OrderStatus.values()){
            System.out.println(i++ + ". " + status);
        }

        System.out.println("Please choose a category from the list above: ");
        int option = TakeSingleInput(sc, 1, i, "Enter the integer associated with an option to choose it: ");
        return statuses[option-1];
    }

    protected void GUIBasedOrderView(JPanel previousScreen, List<Order> OrderHistory){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        if(OrderHistory.isEmpty()){
            JLabel emptyLabel = new JLabel("Ooho! There are no orders to show!");
            emptyLabel.setFont(new Font("Arial", Font.BOLD, 35));
            emptyLabel.setForeground(Color.BLUE);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(emptyLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 30)));
        } else {
            JLabel heading = new JLabel("Your Order History:");
            heading.setFont(new Font("Arial", Font.BOLD, 25));
            heading.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(heading);
            panel.add(Box.createRigidArea(new Dimension(0, 20))); // Space below heading

            StringBuilder ordersDetails = new StringBuilder();

            for (Order order : OrderHistory) {
                if(!(order.getStatus()==OrderStatus.OrderPlaced || order.getStatus()==OrderStatus.OutForDelivery)){
                    continue;
                }
                ordersDetails.append("Order ID: ").append(order.getOrderID()).append("\n")
                        .append("Date: ").append(order.getDate()).append("\n")
                        .append("Status: ").append(order.getStatus()).append("\n")
                        .append("VIP Order: ").append(order.getVIPOrder() ? "Yes" : "No").append("\n")
                        .append("Special Request: ").append(order.getSpecialRequest().isEmpty() ? "None" : order.getSpecialRequest()).append("\n")
                        .append("Total Price: ").append(String.format("%.2f", order.getPrice())).append("\n")
                        .append("Items:\n");

                for (Map.Entry<Item, Integer> entry : order.getItems().entrySet()) {
                    Item item = entry.getKey();
                    int quantity = entry.getValue();

                    ordersDetails.append("  - Name: ").append(item.getName())
                            .append(" | Price: ").append(String.format("%.2f", item.getPrice()))
                            .append(" | Quantity: ").append(quantity)
                            .append(" | Available: ").append(item.getIsAvailable() ? "Yes" : "No").append("\n");
                }
                ordersDetails.append("\n-----------------------\n"); // Separator between orders
            }

            JTextArea orderTextArea = new JTextArea(ordersDetails.toString());
            orderTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14)); // Monospaced font for alignment
            orderTextArea.setEditable(false); // Make it read-only
            orderTextArea.setLineWrap(true); // Wrap long lines
            orderTextArea.setWrapStyleWord(true); // Wrap at word boundaries
            orderTextArea.setAlignmentX(Component.CENTER_ALIGNMENT);

            JScrollPane scrollPane = new JScrollPane(orderTextArea);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setPreferredSize(new Dimension(600, 400)); // Adjust as needed
            panel.add(scrollPane);
        }

        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 20));
        backButton.setPreferredSize(new Dimension(200, 50)); // Set button size
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        backButton.addActionListener(e -> {
            frame.setContentPane(previousScreen);
            frame.revalidate();
            frame.repaint();
        });

        panel.add(backButton);
        panel.add(Box.createRigidArea(new Dimension(0, 50)));

        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }
}
