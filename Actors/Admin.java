package Actors;

import Helper.*;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Admin extends User implements Serializable {

    private final Map<OrderStatus, java.util.LinkedList<Order>> AllOrders;
    private int PendingVIPOrders, CancelledVIPOrders;

    public Admin(String Email, String Name, String Password){
        super(Email, Name, Password);
        AllOrders = new HashMap<>();
        PendingVIPOrders = 0;
        CancelledVIPOrders = 0;
        for(OrderStatus status: OrderStatus.values()){
            AllOrders.put(status, new java.util.LinkedList<>());
        }
    }

    @Override
    protected String encode(String Password) {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < Password.length(); i++) {
            char ch = Password.charAt(i);
            ch -= 5;
            encoded.append(ch);
        }
        return encoded.toString();
    }

    @Override
    protected String decode(String encodedPassword) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < encodedPassword.length(); i++) {
            char ch = encodedPassword.charAt(i);
            ch += 5;
            decoded.append(ch);
        }
        return decoded.toString();
    }

    void CancelOrder(Order order){
        if(order.getAssociatedCustomer().getIsVIP()){
            AllOrders.get(OrderStatus.Cancelled).add(CancelledVIPOrders, order);
            CancelledVIPOrders++;
        } else {
            AllOrders.get(OrderStatus.Cancelled).add(order);
        }
    }

    public Map<OrderStatus, java.util.LinkedList<Order>> getAllOrders(){
        return AllOrders;
    }

    public void addOrder(Order currentOrder){
        if(currentOrder.getVIPOrder()){
            AllOrders.get(currentOrder.getStatus()).add(PendingVIPOrders, currentOrder);
            PendingVIPOrders++;
        } else{
            AllOrders.get(currentOrder.getStatus()).addLast(currentOrder);
        }
    }

    public void DailySalesReport(Scanner sc, PriorityQueue<Item> itemQueue) {
        if (AllOrders.isEmpty()) {
            System.out.println("Ooho! There are no orders to show!");
            return;
        }

        Map<LocalDate, List<Order>> sortedOrders = new TreeMap<>();
        for(OrderStatus orderStatus : OrderStatus.values()){
            for(Order currentOrder: AllOrders.get(orderStatus)){
                sortedOrders.putIfAbsent(currentOrder.getDate(), new LinkedList<>());
                sortedOrders.get(currentOrder.getDate()).add(currentOrder);
            }
        }

        System.out.println("\n========== Orders ==========");

        int i=1;
        for(Map.Entry<LocalDate, List<Order>> entry: sortedOrders.entrySet()){
            for(Order order: entry.getValue()){
                System.out.println(i++);
                order.ShowInfo();
            }
        }

        System.out.println("======= End of Report =======\n");

        PriorityQueue<Item> items = new PriorityQueue<>(itemQueue);

        System.out.println("\n===== Most Popular Items =====");
        int serial = 1;
        while (!items.isEmpty()) {
            Item item = items.poll();
            if(item.getNumberOfOrders()!=0){
                System.out.printf("%d. %s               : (Ordered %d times)%n", serial++, item.getName(), item.getNumberOfOrders());
            }
        }
        System.out.println("=============================\n");

        System.out.println(ANSI_GREEN + "Total number of orders: " + (i-1) + ANSI_RESET);

        while(true){
            System.out.println("Choose an option\n1. View Date-wise Orders\n2. Go Back");
            int option = TakeSingleInput(sc, 1, 3, "Enter the serial number associated with an option to choose it: ");
            if(option==1){
                int j=1;
                System.out.println("Choose one of these dates: ");
                List<LocalDate> dateList = new ArrayList<>(sortedOrders.keySet());
                for(LocalDate date: dateList){
                    System.out.printf("%d. %s%n", j++, date);
                }
                int choice = TakeSingleInput(sc, 1, j, "Enter the serial number associated with a date to choose it: ");
                LocalDate chosenDate = dateList.get(choice-1);
                List<Order> chosenOrders = sortedOrders.get(chosenDate);
                j=1;
                for(Order currentOrders: chosenOrders){
                    System.out.println(j++);
                    currentOrders.ShowInfo();
                }

                System.out.println(ANSI_GREEN + "Total number of orders: " + (j-1) + ANSI_RESET);
            } else {
                break;
            }
        }
    }

    private void AddItem(Item newItem, Map<Category, List<Item>> AllItemsByCategory, Map<String, Item> AllItemsByNames, Trie ItemNames, PriorityQueue<Item> itemQueue){
        AllItemsByNames.put(newItem.getName(), newItem);
        AllItemsByCategory.putIfAbsent(newItem.getCategory(), new ArrayList<>());
        AllItemsByCategory.get(newItem.getCategory()).add(newItem);
        ItemNames.insert(newItem.getName());
        itemQueue.add(newItem);
    }

    private void AddNewItem(Scanner sc, Map<Category, List<Item>> AllItemsByCategory, Map<String, Item> AllItemsByNames, Trie ItemNames, PriorityQueue<Item> itemQueue){
        float Price;
        System.out.print("What should be the name of the new item: ");
        String Name = sc.nextLine();
        while(true){
            System.out.print("What should be the price of this item: ");
            String strInput = sc.nextLine();
            try{
                Price = Float.parseFloat(strInput);
                if(Price<=0){
                    System.out.println("Price should be greater than 0");
                } else {
                    break;
                }
            } catch (Exception e){
                System.out.println(ANSI_RED + "Invalid Number! Enter a valid Number!" + ANSI_RESET);
            }
        }
        Category category = User.getCategory(sc);
        System.out.println("Is this item currently available?");
        int option = TakeSingleInput(sc, 1, 3, "Choose 1 for yes, 2 for no: ");
        Name = Name.toLowerCase();
        AddItem(new Item(Name, Price, category, option==1), AllItemsByCategory, AllItemsByNames, ItemNames, itemQueue);
        System.out.println(ANSI_GREEN + "Successfully added " + Name + " to the canteen!" + ANSI_RESET);
    }

    private void InsertAtIndex(java.util.LinkedList<Order> GivenList, Order ToAdd){
        if(GivenList==null){
            return;
        }
        int index = 0;
        for(Order CurrentOrder: GivenList){
            if(CurrentOrder.getDate().isAfter(ToAdd.getDate())){
                break;
            }
            index++;
        }
        GivenList.add(index, ToAdd);
    }

    private void DenyExistingOrders(Item foundItem) {
        AllOrders.putIfAbsent(OrderStatus.Denied, new java.util.LinkedList<>());
        java.util.LinkedList<Order> PendingOrders = AllOrders.get(OrderStatus.OrderPlaced);
        List<Order> ordersToDeny = new ArrayList<>();
        if (PendingOrders != null && !PendingOrders.isEmpty()) {
            for (Order TargetOrder : PendingOrders) {
                for (Map.Entry<Item, Integer> entry : TargetOrder.getItems().entrySet()) {
                    Item TargetItem = entry.getKey();
                    if (Objects.equals(TargetItem.getName(), foundItem.getName())) {
                        TargetOrder.setStatus(OrderStatus.Denied);
                        ordersToDeny.add(TargetOrder);
//                        AllOrders.get(OrderStatus.Denied).add(TargetOrder);
                        InsertAtIndex(AllOrders.get(OrderStatus.Denied), TargetOrder);
                        TargetOrder.getAssociatedCustomer().Deposit(TargetOrder.getPrice());
                        TargetOrder.getAssociatedCustomer().appendNotification("An admin has denied one of your orders!");
                        break;
                    }
                }
            }
            for(Order TargetOrder: ordersToDeny){
                AllOrders.get(OrderStatus.OrderPlaced).remove(TargetOrder);
            }
        }
    }

    private void RemoveAnItem(Scanner sc, Map<Category, List<Item>> AllItemsByCategory, Map<String, Item> AllItemsByNames, Trie ItemNames, PriorityQueue<Item> itemQueue){
        Item foundItem = getItemFromPrefix(sc, AllItemsByCategory, AllItemsByNames, ItemNames);
        if(foundItem!=null){
            String itemName = foundItem.getName();
            ItemNames.delete(itemName);
            AllItemsByNames.remove(itemName);
            AllItemsByCategory.get(foundItem.getCategory()).remove(foundItem);
            itemQueue.remove(foundItem);
            DenyExistingOrders(foundItem);
            System.out.println(ANSI_GREEN + "Successfully removed " + itemName + " from menu!" + ANSI_RESET);
        }
    }

    private void ModifyExistingItems(Scanner sc, Map<Category, List<Item>> AllItemsByCategory, Map<String, Item> AllItemsByNames, Trie ItemNames){
        Item foundItem = getItemFromPrefix(sc, AllItemsByCategory, AllItemsByNames, ItemNames);
        if(foundItem==null){
            return;
        }
        while(true){
            System.out.println("Choose an option:\n1. Change Name\n2. Change Price\n3. Change category\n4. Change Availability\n5. Go Back");
            int option = TakeSingleInput(sc, 1, 6, "Enter the serial number associated with an option to choose it: ");
            if(option==1){
                System.out.println("What should be the name of the item: ");
                String oldName = foundItem.getName();
                String newName = sc.nextLine();
                foundItem.ChangeName(newName);
                ItemNames.delete(oldName);
                ItemNames.insert(newName);
                System.out.println(ANSI_GREEN + "Successfully changed name to " + newName + ANSI_RESET);
            } else if(option==2){
                float Price;
                while(true){
                    System.out.print("What should be the new price of the item: ");
                    String strInput = sc.nextLine();
                    try{
                        Price = Float.parseFloat(strInput);
                        if(Price<=0){
                            System.out.println("Enter a number greater than 0");
                        } else {
                            break;
                        }
                    } catch (Exception e){
                        System.out.println(ANSI_RED + "Invalid Number! Enter a valid number" + ANSI_RESET);
                    }
                }
                foundItem.setPrice(Price);
                System.out.println(ANSI_GREEN + "Successfully changed price to: " + Price + ANSI_RESET);
            } else if(option==3){
                Category category = getCategory(sc);
                Category oldCategory = foundItem.getCategory();
                foundItem.setCategory(category);
                AllItemsByCategory.get(oldCategory).remove(foundItem);
                AllItemsByCategory.putIfAbsent(category, new ArrayList<>());
                AllItemsByCategory.get(category).add(foundItem);
                System.out.println(ANSI_GREEN + "Successfully changed category to " + category + ANSI_RESET);
            } else if(option==4){
                System.out.println("Is the item available: ");
                int choice = TakeSingleInput(sc, 1, 3, "Choose 1 for yes and 2 for no: ");
                foundItem.setAvailability(choice == 1);
                System.out.println(ANSI_GREEN + "Successfully updated availability of " + foundItem.getName() + ANSI_RESET);
            } else {
                return;
            }
        }
    }

    public void MenuManagement(Scanner sc, Map<Category, List<Item>> AllItemsByCategory, Map<String, Item> AllItemsByNames, Trie ItemNames, PriorityQueue<Item> itemQueue){
        while(true){
            System.out.println("Choose an option:\n1. View all Items\n2. Add new Item\n3. Remove an Item\n4. Modify existing items\n5. Go Back");
            int option = TakeSingleInput(sc,1,6, "Enter the serial number associated with an option to choose it: ");
            if(option==1){
                ViewAllItems(AllItemsByCategory);
            } else if(option==2){
                AddNewItem(sc, AllItemsByCategory, AllItemsByNames, ItemNames, itemQueue);
            } else if(option==3){
                RemoveAnItem(sc, AllItemsByCategory, AllItemsByNames, ItemNames, itemQueue);
            } else if(option ==4){
                ModifyExistingItems(sc, AllItemsByCategory, AllItemsByNames, ItemNames);
            } else{
                return;
            }
        }
    }

    private boolean IfInCorrectOrder(ArrayList<Integer> arr){
        for(int i=0; i<arr.size(); i++){
            if(arr.get(i)!=i+1){
                return false;
            }
        }
        return true;
    }

    private void AutomaticallyUpdateToDelivered(Order targetOrder, String str1){
        ScheduledExecutorService obj = Executors.newSingleThreadScheduledExecutor();;
        obj .schedule(() -> {
            targetOrder.setStatus(OrderStatus.Delivered);
            if(targetOrder.getVIPOrder()){
                PendingVIPOrders--;
            }
            targetOrder.getAssociatedCustomer().appendNotification(str1);
            this.appendNotification(str1);
            InsertAtIndex(AllOrders.get(OrderStatus.Delivered), targetOrder);
            obj.shutdown();
        }, 10, TimeUnit.SECONDS);
    }

    private int UpdateStatus(Scanner sc, OrderStatus oldStatus, OrderStatus newStatus){
        java.util.LinkedList<Order> PendingOrders = new java.util.LinkedList<>();

        int i=0;
        while(i<AllOrders.get(oldStatus).size()){
            Order VIPOrder = AllOrders.get(oldStatus).get(i);
            if(!VIPOrder.getVIPOrder()){
                break;
            }
            PendingOrders.addLast(VIPOrder);
            i++;
        }
        int j=i;
        if(PendingOrders.isEmpty()){
            while(i<AllOrders.get(oldStatus).size()){
                Order NormalOrder = AllOrders.get(oldStatus).get(i);
                PendingOrders.addLast(NormalOrder);
                i++;
            }
        }

        if(PendingOrders.isEmpty()){
            return -1;
        }
        int k=1;
        for(Order CurrentOrder: PendingOrders){
            System.out.println("Order #" + k++);
            CurrentOrder.ShowInfo();
        }
        if(j==i && !PendingOrders.isEmpty()){
            System.out.println("\n\nNOTE: You may have normal orders too! Process VIP ones first to process them");
        }
        ArrayList<Integer> options = TakeMultipleInputs(sc, "Enter the serial number associated with orders to update their status (comma-separated): ");
        if(!IfInCorrectOrder(options)){
            System.out.println(ANSI_RED + "You need to process orders fairly (on FIFO basis)" + ANSI_RESET);
            return 0;
        }
        for(Integer choice: options){
            if(choice<=0 || choice>PendingOrders.size()){
                System.out.println(choice + " is not associated with any order");
            } else {
                Order Target = PendingOrders.get(choice-1);
                Target.setStatus(newStatus);
                this.AllOrders.get(oldStatus).remove(Target);
                InsertAtIndex(AllOrders.get(newStatus), Target);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if(newStatus==OrderStatus.Refunded){
                    if(Target.getVIPOrder()){
                        CancelledVIPOrders--;
                    }
                    Target.getAssociatedCustomer().Deposit(Target.getPrice());
                    Target.getAssociatedCustomer().appendNotification("An admin has refunded your amount for the order with id: " + Target.getOrderID());
                    System.out.println(ANSI_GREEN + "Successfully refunded the customer for the order with id: " + Target.getOrderID() + ANSI_RESET);
                }
                else if(newStatus==OrderStatus.OutForDelivery){
                    Target.getAssociatedCustomer().appendNotification("Your order with id " + Target.getOrderID() + " is out for delivery!");
                    System.out.println(ANSI_GREEN + "Successfully updated the status of order with id " + Target.getOrderID() + " to out for delivery!" + ANSI_RESET);
                    AutomaticallyUpdateToDelivered(Target, "Order with id " + Target.getOrderID() + " is delivered");
                }
            }
        }
        return 0;
    }

    public void OrderManagement(Scanner sc){
        while(true){
            System.out.println("Choose an option\n1. View all orders\n2. View Orders by Status\n3. Update Status of Pending Orders\n4. Process Refunds\n5. View Refunded orders\n6. Go Back");
            int option = TakeSingleInput(sc,1,7, "Enter the serial number associated with an option to choose it: ");
            if(option==1){
                int i=1;
                for(Map.Entry<OrderStatus, java.util.LinkedList<Order>> entry:  AllOrders.entrySet()){
                    for(Order Target: entry.getValue()){
                        System.out.print(i++);
                        Target.ShowInfo();
                    }
                }
            } else if(option==2){
                OrderStatus status = getStatusFromUser(sc);
                ViewOrdersByStatus(status, this.AllOrders.get(status));
            } else if(option==3){
                int i = UpdateStatus(sc, OrderStatus.OrderPlaced, OrderStatus.OutForDelivery);
                if(i==-1){
                    System.out.println("There are no orders to process");
                }
            } else if(option==4){
                int i = UpdateStatus(sc, OrderStatus.Cancelled, OrderStatus.Refunded);
                if(i==-1){
                    System.out.println("There are no cancelled orders to refund");
                }
            } else if(option==5){
                ViewOrdersByStatus(OrderStatus.Refunded, AllOrders.get(OrderStatus.Refunded));
            } else {
                return;
            }
        }
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
        JButton ViewPendingOrders = new JButton("View All Orders");

        ViewMenuButton.setBounds(225, 500, 250, 50);
        ViewPendingOrders.setBounds(660, 500, 250, 50);
        BackButton.setBounds(1125, 500, 250, 50);

        BackButton.addActionListener(e -> {
            frame.setVisible(false);
            frame.dispose();
        });

        ViewMenuButton.addActionListener(e -> {
            GUIBasedCanteenMenu(AllItemsByCategory, panel);
        });

        ViewPendingOrders.addActionListener(e -> {
            List<Order> OrderHistory = new LinkedList<>();
            OrderHistory.addAll(AllOrders.get(OrderStatus.OrderPlaced));
            OrderHistory.addAll(AllOrders.get(OrderStatus.OutForDelivery));

//            for(Map.Entry<OrderStatus, LinkedList<Order>> entry: AllOrders.entrySet()){
//                OrderHistory.addAll(entry.getValue());
//            }
            GUIBasedOrderView(panel, OrderHistory);
        });

        panel.add(BackButton);
        panel.add(ViewMenuButton);
        panel.add(ViewPendingOrders);
        frame.setContentPane(panel);
        frame.repaint();
    }
}
