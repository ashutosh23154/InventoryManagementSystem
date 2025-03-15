package Actors;

import Helper.*;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class Customer extends User implements Serializable {

    private final Map<Item, Integer> Cart;
    private final List<Order> OrderHistory;
    private float AmountInWallet;
    private boolean isVIP;
    private final int cardNo;
    private static final String pathForOrderHistory = "IOHandling/OrderHistory.txt";
//    private JPanel panel;

    private static final Comparator<Item> priceComparator = (u1, u2) -> Float.compare(u1.getPrice(), u2.getPrice());

    public Customer(String Email, String Name, String Password){
        super(Email, Name, Password);
        Cart = new HashMap<>();
        OrderHistory = new ArrayList<>();
        AmountInWallet = 2000;
        isVIP = false;
        Random random = new Random();
        cardNo = 1000 + random.nextInt(9000);
    }

    @Override
    protected String encode(String Password) {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < Password.length(); i++) {
            char ch = Password.charAt(i);
            ch += 3;
            encoded.append(ch);
        }
        return encoded.toString();
    }

    @Override
    protected String decode(String encodedPassword) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < encodedPassword.length(); i++) {
            char ch = encodedPassword.charAt(i);
            ch -= 3;
            decoded.append(ch);
        }
        return decoded.toString();
    }

    public List<Order> getOrderHistory(){
        return this.OrderHistory;
    }

    public float getAmountInWallet(){
        return AmountInWallet;
    }

    public void Deposit(float Amount){
        AmountInWallet += Amount;
    }

    public void Withdraw(float Amount){
        AmountInWallet -= Amount;
    }

    private static String generateOrderID() {

        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();

        StringBuilder orderId = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(CHARACTERS.length());
            orderId.append(CHARACTERS.charAt(index));
        }

        return orderId.toString();
    }

    public void addOrder(Order CurrentOrder){
        OrderHistory.add(CurrentOrder);
    }

    private void Reorder(Scanner sc, List<Order> TotalOrdersRaw, Admin admin, long startTime, Map<String, Item> AllItemsByName, PriorityQueue<Item> itemQueue){
        if(TotalOrdersRaw==null || TotalOrdersRaw.isEmpty()){
            System.out.println("There are no orders available to re-order");
            return;
        }
        int i=1;
        List<Order> TotalOrders = new ArrayList<>(TotalOrdersRaw);
        List<Order> DeniedOrders = new ArrayList<>();
        for(Order CurrentOrder: TotalOrders){
            if(CurrentOrder.getStatus()==OrderStatus.Denied){
                DeniedOrders.add(CurrentOrder);
                continue;
            }
            System.out.print(i++);
            CurrentOrder.ShowInfo();
        }
        for(Order toDeny: DeniedOrders){
            TotalOrders.remove(toDeny);
        }
        System.out.println("Denied Orders are not shown here as you can't reorder them!");
        ArrayList<Integer> Choices = TakeMultipleInputs(sc, "Enter the serial numbers associated with orders to re-order them (comma-separated): ");
        for(int choice: Choices){
            try{
                Order CurrentOrder = TotalOrders.get(choice-1);
                Item UnavailableItem = canPlaceOrder(CurrentOrder.getItems());
                if(UnavailableItem!=null){
                    System.out.println(CurrentOrder.getOrderID() + " contains " + UnavailableItem.getName() + " which is unavailable at the moment. So, we can't place this order!");
                    continue;
                }

                HashMap<Item, Integer> map = new HashMap<>();

                float newPrice = 0;

                for(Map.Entry<Item, Integer> entry: CurrentOrder.getItems().entrySet()){
                    Item Target = AllItemsByName.get(entry.getKey().getName());
                    newPrice += (Target.getPrice()*entry.getValue());
                }

                if(newPrice>this.AmountInWallet){
                    System.out.println("Ooho! You don't have enough money to place the same-order as one with id " + CurrentOrder.getOrderID());
                    continue;
                }
                long elapsedMilliseconds = System.currentTimeMillis() - startTime;
                int DaysToSkip = (int) (elapsedMilliseconds/30_000);
                LocalDate OrderDate = LocalDate.now();
                OrderDate = OrderDate.plusDays(DaysToSkip);

                String specialRequest;
                System.out.print("Do you have any special request for this order (you may choose NA): ");
                specialRequest = sc.nextLine();

                System.out.print("Enter Delivery Details: " );
                String Delivery = sc.nextLine();

                if(AskForCardDetails(sc)==-1){
                    System.out.println(ANSI_RED + "Reorder cancelled due to wrong card details" + ANSI_RESET);
                    return;
                }

                for(Map.Entry<Item, Integer> entry: CurrentOrder.getItems().entrySet()){
                    Item Target = AllItemsByName.get(entry.getKey().getName());
                    Target.incrementNumberOfOrders(entry.getValue(), itemQueue);
                    map.put(new Item(Target), entry.getValue());
                }

                Order newOrder = new Order(generateOrderID(), map, OrderDate, newPrice, this, specialRequest, isVIP, Delivery);
                this.saveOrderHistory(newOrder);
                this.addOrder(newOrder);
                this.Withdraw(newOrder.getPrice());
                admin.addOrder(newOrder);
                admin.appendNotification(getName() + " has recently ordered their previous meal!");
                System.out.println(ANSI_GREEN + "You have successfully placed an order same as order with order ID: " + CurrentOrder.getOrderID() + ANSI_RESET);

            } catch (Exception e){
                System.out.println(choice + " is not associated with any order");
            }
        }
    }

    private void saveOrderHistory(Order order) {
        try (FileWriter fileWriter = new FileWriter(pathForOrderHistory, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            Customer customer = order.getAssociatedCustomer();

            bufferedWriter.write("Customer: " + customer.getEmail() + " (" + customer.getName() + ")\n");
            String orderDetails = String.format(
                    "  OrderID: %s, Date: %s, Price: %.2f, Status: %s, VIP: %s, Special Request: %s, Delivery Details: %s\n",
                    order.getOrderID(),
                    order.getDate(),
                    order.getPrice(),
                    order.getStatus(),
                    order.getVIPOrder() ? "Yes" : "No",
                    order.getSpecialRequest() != null ? order.getSpecialRequest() : "None",
                    order.getDeliveryDetails() != null ? order.getDeliveryDetails() : "None"
            );
            bufferedWriter.write(orderDetails);
            bufferedWriter.write("    Items:\n");
            for (Map.Entry<Item, Integer> itemEntry : order.getItems().entrySet()) {
                Item item = itemEntry.getKey();
                int quantity = itemEntry.getValue();
                bufferedWriter.write(String.format(
                        "      %s (Price: %.2f, Quantity: %d)\n",
                        item.getName(), item.getPrice(), quantity
                ));
            }
            bufferedWriter.write("\n");
        } catch (IOException e) {
            System.out.println("Error saving order history: " + e.getMessage());
        }
    }

    public void ViewAllOrders(Scanner sc, Admin admin, long startTime, Map<String, Item> AllItemsByName, PriorityQueue<Item> itemQueue) {
        while(true){
            if (OrderHistory.isEmpty()) {
                System.out.println("Ooho! You have not placed any order yet!");
                return;
            }
            List<Order> PlacedDeliveredOrders = new ArrayList<>();
            int i = 1;
            System.out.println("=======================================");
            System.out.println("           Order History               ");
            System.out.println("=======================================");

            for (Order currentOrder : OrderHistory) {
                System.out.printf("Order #%d\n", i++);
                System.out.println("--------------------------------------------------");
                System.out.printf("Order ID         : %s\n", currentOrder.getOrderID());

                System.out.println("Items in this Order:");
                float orderTotal = 0;
                for (Map.Entry<Item, Integer> entry : currentOrder.getItems().entrySet()) {
                    Item currentItem = entry.getKey();
                    int quantity = entry.getValue();
                    float itemTotal = currentItem.getPrice() * quantity;
                    orderTotal += itemTotal;

                    System.out.println("  -------------------------------------");
                    System.out.printf("  Product Name   : %s\n", currentItem.getName());
                    System.out.printf("  Product Price  : Rs. %.2f\n", currentItem.getPrice());
                    System.out.printf("  Quantity       : %d\n", quantity);
                    System.out.printf("  Total          : Rs. %.2f\n", itemTotal);
                    System.out.println("  -------------------------------------");
                }

                System.out.println("--------------------------------------------------");
                System.out.printf("Order Total      : Rs. %.2f\n", orderTotal);
                System.out.println("==================================================\n");
                if (currentOrder.getStatus() == OrderStatus.OrderPlaced || currentOrder.getStatus() == OrderStatus.Delivered) {
                    PlacedDeliveredOrders.add(currentOrder);
                }
            }

            System.out.println("Choose an option:\n1. Cancel an order\n2. Track Status of an order\n3. Re-order\n4. Go Back");
            int option = TakeSingleInput(sc, 1, 5, "Enter the serial number associated with an option to choose it: ");
            if (option == 1) {
                if (PlacedDeliveredOrders.isEmpty()) {
                    System.out.println("There are no items to cancel");
                    return;
                }
                int j = 1;
                for (Order CurrentOrder : PlacedDeliveredOrders) {
                    System.out.println(j++);
                    CurrentOrder.ShowInfo();
                }
                CancelOrder(PlacedDeliveredOrders, sc, admin, AllItemsByName, itemQueue);
            } else if (option == 2) {
                int choice = TakeSingleInput(sc, 1, OrderHistory.size() + 1, "Enter the serial number associated with an order to track their status: ");
                Order TargetOrder = OrderHistory.get(choice - 1);
                System.out.println("Status of order with id " + TargetOrder.getOrderID() + " is " + ANSI_GREEN + TargetOrder.getStatus() + ANSI_RESET);
            } else if (option == 3) {
                Reorder(sc, OrderHistory, admin, startTime, AllItemsByName, itemQueue);
            } else {
                return;
            }
        }
    }

    @SafeVarargs
    private void SortByPrice(Scanner sc, List<Item>... itemLists){
        List<Item> Items = new ArrayList<>();
        for(List<Item> itemList: itemLists){
            Items.addAll(itemList);
        }

        Items.sort(priceComparator);

        if(Items.isEmpty()){
            System.out.println("Ooho! There are no items to sort");
            return;
        }

        int i=1;
        for(Item Current: Items){
            System.out.println(i++);
            Current.ShowInfo();
        }

        while(true){
            System.out.println("Choose an option\n1. Add Item to cart\n2. Feedback Center\n3. Go Back");
            int option = TakeSingleInput(sc, 1, 4, "Enter the serial number associated with an option to choose them: ");
            if(option==1){
                AddItemsToCart(sc, Items);
            } else if(option==2){
                System.out.println("Please choose an item!");
                int ItemNum = TakeSingleInput(sc, 1, Items.size()+1, "Enter the serial number associated with an item to choose it: ");
                Item TargetItem = Items.get(ItemNum-1);
                ManageFeedback(sc, TargetItem);
            } else{
                break;
            }
        }
    }

    @SafeVarargs
    private void AddItemsToCart(Scanner sc, List<Item>... Available){
        List<Item> AllAvailableItems = new ArrayList<>();
        for(List<Item> Current: Available){
            AllAvailableItems.addAll(Current);
        }
        if(AllAvailableItems.isEmpty()){
            System.out.println("Ooho! No items available to add into the cart");
            return;
        }
        ArrayList<Integer> options  = TakeMultipleInputs(sc, "Enter the serial numbers associated with items to add into cart (comma-separated): ");
        for(Integer option: options){
            Item ToAdd;
            try{
                ToAdd = AllAvailableItems.get(option-1);
            } catch (Exception e){
                System.out.println(ANSI_RED + option + " is not associated with any item" + ANSI_RESET);
                continue;
            }
            if(!ToAdd.getIsAvailable()){
                System.out.println(ANSI_RED + "You cannot add " + ToAdd.getName() + " to your cart as it is unavailable at the moment!" + ANSI_RESET);
                continue;
            }
            System.out.println("How many " + ToAdd.getName() + " would you like to order: ");
            int numbers;
            while(true){
                String strInput;
                strInput = sc.nextLine();
                try{
                    numbers = Integer.parseInt(strInput);
                    if(numbers>=1){
                        break;
                    }
                    else{
                        System.out.println("Enter a number greater than 0");
                    }
                } catch(NumberFormatException e){
                    System.out.println(ANSI_RED + "Invalid Integer! Enter a valid integer!" + ANSI_RESET);
                }
            }
            if(this.Cart.containsKey(ToAdd)){
                int oldValue = this.Cart.get(ToAdd);
                this.Cart.put(ToAdd, oldValue+numbers);
            } else{
                this.Cart.put(ToAdd, numbers);
            }
            System.out.println(ANSI_GREEN + "Successfully added " + ToAdd.getName() + " to your cart\n" + ANSI_RESET);
        }
    }

    private void SearchByName(Scanner sc, Trie ItemNames, Map<String, Item> AllItemsByNames){
        System.out.print("Enter the prefix of the item name: ");
        String prefix = sc.nextLine();
        prefix = prefix.toLowerCase();
        List<String> FoundNames = ItemNames.startsWith(prefix);
        if(FoundNames.isEmpty()){
            System.out.println("There are no items beginning with " + prefix);
            return;
        }
        Map<Category, List<Item>> FoundItems = new HashMap<>();

        for(String CurrentName: FoundNames){
            CurrentName = CurrentName.toLowerCase();
            Item CurrentItem = AllItemsByNames.get(CurrentName);
            FoundItems.putIfAbsent(CurrentItem.getCategory(), new ArrayList<>());
            FoundItems.get(CurrentItem.getCategory()).add(CurrentItem);
        }

        System.out.println("Items found with given prefix: ");
        int i=1;
        List<Item> itemList = new ArrayList<>();
        for(Map.Entry<Category, List<Item>> entry: FoundItems.entrySet()){
            for(Item currentItem: entry.getValue()){
                System.out.println(i++);
                currentItem.ShowInfo();
                itemList.add(currentItem);
                System.out.println();
            }
        }

        while(true){
            System.out.println("Choose an option:\n1. Search with another prefix\n2. Sort by price (low to high)\n3. Filter by Categories\n4. Add Items to cart\n5. Feedback Center\n6. Go Back");
            int option = TakeSingleInput(sc, 1, 7, "Enter the integer associated with an option to choose it: ");
            if(option==1){
                SearchByName(sc, ItemNames, AllItemsByNames);
                return;
            } else if(option==2){
                SortByPrice(sc, FoundItems.values().toArray(new List[0]));
            } else if(option==3){
                FilterByCategories(sc, FoundItems);
            } else if(option==4){
                System.out.println("Items found with given prefix: ");
                int j=1;
                for(Map.Entry<Category, List<Item>> entry: FoundItems.entrySet()){
                    for(Item currentItem: entry.getValue()){
                        System.out.println(j++);
                        currentItem.ShowInfo();
                        System.out.println();
                    }
                }
                AddItemsToCart(sc, FoundItems.values().toArray(new List[0]));
            } else if (option==5){
                System.out.println("Please choose an item: ");
                int choice = TakeSingleInput(sc, 1, itemList.size()+1, "Enter the serial number associated with an item to choose it: ");
                Item TargetItem = itemList.get(choice-1);
                ManageFeedback(sc, TargetItem);
            } else {
                return;
            }
        }
    }

    private boolean isInOrder(Item TargetItem){
        for(Order currentOrder: OrderHistory){
            for(Map.Entry<Item, Integer> entry: currentOrder.getItems().entrySet()){
                if(entry.getKey().equals(TargetItem)){
                    return true;
                }
            }
        }
        return false;
    }

    private void ManageFeedback(Scanner sc, Item TargetItem){
        while(true){
            System.out.println("Choose an option:\n1. Give Feedback\n2. Edit your feedback\n3. Delete your feedback\n4. View Feedbacks\n5. Go Back");
            int option = TakeSingleInput(sc, 1, 6, "Enter the serial number associated with an option to choose it: ");
            if(option==1){
                if(!isInOrder(TargetItem)){
                    System.out.println("You have not ordered this item yet!. So, you cannot give feedback to to this product.");
                    continue;
                }
                TargetItem.GiveFeedback(sc, this);
            } else if(option==2){
                TargetItem.EditFeedback(sc, this);
            } else if(option==3){
                TargetItem.DeleteFeedback(this);
            } else if(option==4){
                TargetItem.ViewAllFeedbacks();
            } else{
                return;
            }
        }
    }

    private void FilterByCategories(Scanner sc, Map<Category, List<Item>> ItemsByCategory){
        Category selectedCategory = getCategory(sc);
        List<Item> itemsInCategory = ItemsByCategory.get(selectedCategory);
        if (itemsInCategory == null || itemsInCategory.isEmpty()) {
            System.out.println("No items found in the category: " + selectedCategory);
            return;
        } else {
            int i=1;
            System.out.println("Items in category: " + selectedCategory);
            for (Item item : itemsInCategory) {
                System.out.println(i++);
                item.ShowInfo();
                System.out.println();
            }
        }

        while(true){
            System.out.println("Choose an option\n1. Add Item to cart\n2. Feedback Center\n3. Go Back");
            int option = TakeSingleInput(sc, 1, 4, "Enter the serial number associated with an option to choose it: ");
            if(option==1){
                AddItemsToCart(sc, itemsInCategory);
            } else if(option==2){
                System.out.println("Please choose an item!");
                int ItemNum = TakeSingleInput(sc, 1, itemsInCategory.size()+1, "Enter the serial number associated with an item to choose it: ");
                Item TargetItem = itemsInCategory.get(ItemNum-1);
                ManageFeedback(sc, TargetItem);
            } else{
                break;
            }
        }
    }

    public void UpgradeToVIP(Scanner sc, int cost){
        System.out.println("VIP membership cost: Rs. " + cost );
        int option = TakeSingleInput(sc, 1, 3, "Enter 1 for yes, 2 for no: ");
        if(option==2){
            return;
        }
        if(isVIP){
            System.out.println("You are already a VIP Customer!");
            return;
        }
        if(this.AmountInWallet<cost){
            System.out.println("Ooho! You don't have enough money in your wallet!");
            return;
        }
        this.Withdraw(cost);
        this.isVIP = true;
        System.out.println(ANSI_GREEN + "Successfully upgraded to VIP Customer" + ANSI_RESET);
    }

    public void CanteenMenu(Scanner sc, Map<Category, List<Item>> AllItemsByCategory, Trie ItemNames, Map<String, Item> AllItemsByNames){
        while(true){
            System.out.println("Choose an option:\n1. View all items\n2. Search by Item name\n3. Sort by price (low to high)\n4. Filter by Category\n5. Go Back");
            int option = TakeSingleInput(sc, 1, 6, "Enter the integer associated with an option to choose it: ");
            if(option==1){
                ViewAllItems(AllItemsByCategory);
                while(true){
                    System.out.println("Choose an option\n1. Add Item to cart\n2. Feedback Center\n3. Go Back");
                    int choice = TakeSingleInput(sc, 1, 4, "Enter the serial number associated with an option to choose them: ");
                    if(choice==1){
                        AddItemsToCart(sc, AllItemsByCategory.values().toArray(new List[0]));
                    } else if(choice==2){
                        System.out.println("Please choose an item!");
                        Item TargetItem = getItemFromPrefix(sc, AllItemsByCategory, AllItemsByNames, ItemNames);
                        ManageFeedback(sc, TargetItem);
                    } else{
                        break;
                    }
                }
            } else if(option == 2){
                SearchByName(sc, ItemNames, AllItemsByNames);
            } else if(option == 3){
                SortByPrice(sc, AllItemsByCategory.values().toArray(new List[0]));
            } else if(option == 4){
                FilterByCategories(sc, AllItemsByCategory);
            } else{
                return;
            }
        }
    }

    private void CancelOrder(List<Order> Orders, Scanner sc, Admin admin, Map<String, Item> AllItemsByName, PriorityQueue<Item> itemQueue){
        if(Orders==null || Orders.isEmpty()){
            System.out.println("There are no orders to cancel");
            return;
        }
        ArrayList<Integer> choices = TakeMultipleInputs(sc, "Enter the serial numbers associated with orders to cancel them (comma-separated): ");
        for(Integer choice: choices){
            if(choice<=0 || choice>Orders.size()){
                System.out.println(choice + " is not associated with any order");
            } else {
                Order TargetOrder = Orders.get(choice-1);
                OrderStatus status = TargetOrder.getStatus();
                Orders.get(choice-1).setStatus(OrderStatus.Cancelled);
                admin.getAllOrders().get(status).remove(TargetOrder);
                admin.CancelOrder(TargetOrder);
                for(Map.Entry<Item, Integer> entry: TargetOrder.getItems().entrySet()){
                    Item TargetItem = AllItemsByName.get(entry.getKey().getName());
                    TargetItem.decrementNumberOfOrders(entry.getValue(), itemQueue);
                }
                admin.appendNotification(getName() + " has recently cancelled an order!");
                System.out.println(ANSI_GREEN + "You have successfully cancelled the order with id: " + TargetOrder.getOrderID() + ANSI_RESET);
            }
        }
    }

    public void OrderOperations(Scanner sc, Admin admin, long startTime, Map<String, Item> AllItemsByNames, PriorityQueue<Item> itemQueue){
        while(true){
            System.out.println("Choose an option:\n1. View all orders\n2. View order by status\n3. Go Back");
            int option = TakeSingleInput(sc, 1, 4, "Enter the serial number associated with an option to choose it: ");
            if(option==1){
                ViewAllOrders(sc, admin, startTime, AllItemsByNames, itemQueue);
            } else if(option==2){
                OrderStatus status = getStatusFromUser(sc);
                List<Order> OrdersByStatus = ViewOrdersByStatus(status, new java.util.LinkedList<>(this.OrderHistory));
                if(!OrdersByStatus.isEmpty() && (status==OrderStatus.OrderPlaced || status==OrderStatus.Delivered)){
                    System.out.println("Choose an option:\n1. Cancel order\n2. Re-order\n3. Go Back");
                    int choice = TakeSingleInput(sc, 1,4, "Enter the serial number associated with an option to choose it: ");
                    if(choice==1){
                        CancelOrder(OrdersByStatus, sc, admin, AllItemsByNames, itemQueue);
                    } else if(choice==2){
                        Reorder(sc, OrdersByStatus, admin, startTime, AllItemsByNames, itemQueue);
                    }
                }
            } else{
                return;
            }
        }
    }

    private void ViewCart() {
        int i = 1;
        float TotalPrice = 0;

        System.out.println("=================================");
        System.out.println("           Cart Details          ");
        System.out.println("=================================");

        for (Map.Entry<Item, Integer> entry : this.Cart.entrySet()) {
            System.out.printf("Item #%d\n", i++);
            System.out.println("------------------------------------------------");
            System.out.printf("Product Name   : %s%n", entry.getKey().getName());
            System.out.printf("Product Price  : Rs. %.2f%n", entry.getKey().getPrice());
            System.out.printf("Quantity       : %d%n", entry.getValue());
            System.out.println("------------------------------------------------");

            TotalPrice += (entry.getKey().getPrice() * entry.getValue());
        }

        System.out.println("=================================");
        System.out.printf("Total Price    : Rs. %.2f%n", TotalPrice);
        System.out.println("=================================");
    }

    private Item getCartItemFromPrefix(Scanner sc, String Prefix, Trie ItemNames, Map<String, Item> AllItemsByName){
        List<String> FoundItems = ItemNames.startsWith(Prefix);

        if(FoundItems.isEmpty()){
            return null;
        }
        if(FoundItems.size()==1){
            String name = FoundItems.getFirst();
            name = name.toLowerCase();
            return AllItemsByName.get(name);
        }
        List<Item> TargetItemsInCart = new ArrayList<>();
        int i=1;
        for(String itemName: FoundItems){
            itemName = itemName.toLowerCase();
            Item TargetItem = AllItemsByName.get(itemName);
            if(Cart.containsKey(TargetItem)){
                System.out.println(i++);
                TargetItem.ShowInfo();
                TargetItemsInCart.add(TargetItem);
            }
        }

        System.out.println("Choose one of these items: ");
        int option = TakeSingleInput(sc, 1, i, "Enter the serial number associated with an option to choose it: ");
        return TargetItemsInCart.get(option-1);
    }

    private void RemoveItemFromCart(Scanner sc, Trie ItemNames, Map<String, Item> AllItemsByName){
        while(true){
            ViewCart();
            System.out.print("Enter the prefix of the name of the item which you wish to remove from the cart (or type 'exit' to quit): ");
            String ItemName = sc.nextLine().trim();
            if(ItemName.equalsIgnoreCase("exit")){
                return;
            }

            Item foundItem = getCartItemFromPrefix(sc, ItemName, ItemNames, AllItemsByName);
            if (foundItem != null) {
                this.Cart.remove(foundItem);
                System.out.println(ANSI_GREEN + "Successfully removed " + foundItem.getName() + " from your cart." + ANSI_RESET);
                ViewCart();
                return;
            } else {
                System.out.println("Item not found in cart. Please try again.");
            }
        }
    }

    private void ModifyQuantities(Scanner sc, Trie ItemNames, Map<String, Item> AllItemsByName){
        while(true){
            ViewCart();
            System.out.print("Enter the name of the item whose order quantity you wish to change (or type 'exit' to quit): ");
            String ItemName = sc.nextLine().trim();
            if(ItemName.equalsIgnoreCase("exit")){
                return;
            }

            Item foundItem = getCartItemFromPrefix(sc, ItemName, ItemNames, AllItemsByName);
            if (foundItem != null) {
                System.out.println("Current quantity: " + this.Cart.get(foundItem));
                String strInput;
                int newQuantity;
                while(true){
                    System.out.print("Enter new quantity: ");
                    strInput = sc.nextLine();
                    try{
                        newQuantity = Integer.parseInt(strInput);
                        if(newQuantity>=1){
                            break;
                        }
                        else{
                            System.out.println("Enter a valid integer!");
                        }
                    }catch (NumberFormatException e){
                        System.out.println(ANSI_RED + "Invalid Integer! Enter a valid integer" + ANSI_RESET);
                    }
                }
                this.Cart.put(foundItem, newQuantity);
                System.out.println(ANSI_GREEN + "Successfully changed quantity of " + foundItem.getName() + " to " + newQuantity + ANSI_RESET);
                return;
            } else {
                System.out.println("Item not found in cart. Please try again.");
            }
        }
    }

    private int AskForCardDetails(Scanner sc){
        int GivenCardNo = TakeSingleInput(sc, 1, 10000, "Enter last 4 digits of your smart card: ");
        if(GivenCardNo==cardNo){
            return GivenCardNo;
        }
        System.out.println(ANSI_RED + GivenCardNo + " is not last 4 digits of your card no." + ANSI_RESET);
        System.out.println("Choose an option:\n1. Try Again\n2. Go Back (Order Cancelled)");
        int option = TakeSingleInput(sc, 1 ,3, "Enter the serial number associated with an option to choose them: ");
        if (option == 1) {
            return AskForCardDetails(sc);
        } else {
            return -1;
        }
    }

    public Item canPlaceOrder(Map<Item, Integer> ItemsToCheck){
        for(Map.Entry<Item, Integer> entry: ItemsToCheck.entrySet()){
            if(!entry.getKey().getIsAvailable()){
                return entry.getKey();
            }
        }
        return null;
    }

    private void Checkout(Scanner sc, Admin admin, long startTime, PriorityQueue<Item> itemQueue){

        Item UnavailableItem = canPlaceOrder(Cart);
        if(UnavailableItem!=null){
            System.out.println(UnavailableItem.getName() + " is not available at the moment. So, we can't place your order. Consider removing this item from cart!");
            return;
        }

        float TotalPrice = 0;
        for(Map.Entry<Item, Integer> entry: this.Cart.entrySet()){
            TotalPrice += (entry.getKey().getPrice()*entry.getValue());
        }
        if(TotalPrice>this.AmountInWallet){
            System.out.println("Ooho! You don't have enough money in your wallet to place an order!");
            return;
        }

        HashMap<Item, Integer> toAdd = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : this.Cart.entrySet()) {
            Item Current = new Item(entry.getKey());
            toAdd.put(Current, entry.getValue());
        }
        long elapsedMilliseconds = System.currentTimeMillis() - startTime;
        int DaysToSkip = (int) (elapsedMilliseconds/30_000);
        LocalDate OrderDate = LocalDate.now();
        OrderDate = OrderDate.plusDays(DaysToSkip);

        System.out.print("Enter any special request you want to make (you may choose 'NA'): " );
        String specialRequest = sc.nextLine();

        System.out.print("Enter Delivery Details: " );
        String Delivery = sc.nextLine();

        if(AskForCardDetails(sc)==-1){
            System.out.println(ANSI_RED + "Checkout cancelled due to wrong card details" + ANSI_RESET);
            return;
        }

        for(Map.Entry<Item, Integer> entry: Cart.entrySet()){
            entry.getKey().incrementNumberOfOrders(entry.getValue(), itemQueue);
        }

        Order newOrder = new Order(generateOrderID(), toAdd, OrderDate, TotalPrice, this, specialRequest, isVIP, Delivery);
        this.saveOrderHistory(newOrder);
        this.addOrder(newOrder);
        this.Cart.clear();
        this.Withdraw(TotalPrice);
        admin.addOrder(newOrder);
        System.out.println(ANSI_GREEN + "You have successfully placed an order" + ANSI_RESET);
        admin.appendNotification(getName() + " has placed an order recently!");
    }

    public void CartOperations(Scanner sc, Admin admin, Trie ItemNames, Map<String, Item> AllItemsByName, long startTime, PriorityQueue<Item> itemQueue){
        while(true){
            if(this.Cart.isEmpty()){
                System.out.println("Ooho! Your cart is empty");
                return;
            }
            System.out.println("Choose an option:\n1. View Items in cart\n2. Modify quantities\n3. Remove Items\n4. Checkout\n5. Go Back");
            int option = TakeSingleInput(sc, 1, 6, "Enter an integer associated with an option to choose it: ");
            if(option==1){
                ViewCart();
            } else if(option == 2){
                ModifyQuantities(sc, ItemNames, AllItemsByName);
            } else if(option == 3){
                RemoveItemFromCart(sc, ItemNames, AllItemsByName);
            } else if(option == 4){
                Checkout(sc, admin, startTime, itemQueue);
            } else{
                return;
            }
        }
    }

    public void CardOperations(Scanner sc){
        while(true){
            System.out.println("Choose an option:\n1. View Card Info.\n2. Deposit\n3. Go Back");
            int option = TakeSingleInput(sc, 1, 4, "Enter the integer associated with an option to choose it: ");
            if(option==1){
                System.out.println("You Smart Card Number is: XXXX XXXX XXXX " + cardNo);
                System.out.println("You have Rs. " + this.getAmountInWallet() + " in your wallet") ;
            } else if(option==2){
                int amount;
                while(true){
                    System.out.print("How much would you like to deposit: ");
                    String strInput = sc.nextLine();
                    try{
                        amount = Integer.parseInt(strInput);
                        if(amount<0){
                            System.out.println("Enter an amount greater than 0");
                        } else{
                            break;
                        }
                    } catch (NumberFormatException e){
                        System.out.println(ANSI_RED + "Invalid Integer! Enter a valid integer!" + ANSI_RESET);
                    }
                }
                this.Deposit(amount);
                System.out.println(ANSI_GREEN + "Successfully added Rs. " + amount + " to your wallet" + ANSI_RESET);
                System.out.println("You have Rs. " + this.getAmountInWallet() + " in your wallet") ;
            } else{
                return;
            }
        }
    }

    public boolean getIsVIP(){
        return this.isVIP;
    }

    private void GUIBasedCartView(JPanel previousScreen) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        if (Cart.isEmpty()) {
            JLabel emptyLabel = new JLabel("Ooho! Your cart is empty");
            emptyLabel.setFont(new Font("Arial", Font.BOLD, 35));
            emptyLabel.setForeground(Color.BLUE);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(emptyLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 30)));
        } else {
            JLabel heading = new JLabel("Your Cart Details:");
            heading.setFont(new Font("Arial", Font.BOLD, 25));
            heading.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(heading);
            panel.add(Box.createRigidArea(new Dimension(0, 20)));

            String[] columnNames = { "Name", "Price", "Available", "Quantity" };
            Object[][] data = new Object[Cart.size()][4];

            int row = 0;
            for (Map.Entry<Item, Integer> entry : Cart.entrySet()) {
                Item item = entry.getKey();
                int quantity = entry.getValue();

                data[row][0] = item.getName();
                data[row][1] = String.format("%.2f", item.getPrice());
                data[row][2] = item.getIsAvailable() ? "Yes" : "No";
                data[row][3] = quantity;
                row++;
            }

            JTable table = new JTable(data, columnNames);
            table.setFont(new Font("Monospaced", Font.PLAIN, 16));
            table.setRowHeight(30);
            table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
            table.getTableHeader().setReorderingAllowed(false); // Disable column reordering

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(scrollPane);
            panel.add(Box.createRigidArea(new Dimension(0, 20)));
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

    public void GUIBasedHomeScreen(Map<Category, List<Item>> AllItemsByCategory){
        frame = new JFrame("GUI Based Interface");
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        JPanel panel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                g.setFont(new Font("Arial", Font.BOLD, 80));
                g.setColor(Color.BLUE);
                g.drawString("Welcome to your account", 280, 300);
            }
        };

        panel.setLayout(null);

        JButton BackButton = new JButton("Back");
        JButton ViewMenuButton = new JButton("View Canteen Menu");
        JButton ViewCartButton = new JButton("View Cart");
        JButton ViewPendingOrders = new JButton("View My Orders");

        ViewMenuButton.setBounds(225, 500, 250, 50);
        ViewCartButton.setBounds(525, 500, 250, 50);
        ViewPendingOrders.setBounds(825, 500, 250, 50);
        BackButton.setBounds(1125, 500, 250, 50);

        BackButton.addActionListener(e -> {
            frame.setVisible(false);
            frame.dispose();
        });

        ViewMenuButton.addActionListener(e -> {
            GUIBasedCanteenMenu(AllItemsByCategory, panel);
        });

        ViewCartButton.addActionListener(e -> {
            GUIBasedCartView(panel);
        });

        ViewPendingOrders.addActionListener(e -> {
            GUIBasedOrderView(panel, OrderHistory);
        });

        panel.add(BackButton);
        panel.add(ViewMenuButton);
        panel.add(ViewCartButton);
        panel.add(ViewPendingOrders);
        frame.setContentPane(panel);
        frame.repaint();
    }
}