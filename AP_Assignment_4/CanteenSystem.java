import Actors.*;
import Helper.*;

import java.io.*;
import java.util.*;

import static Actors.User.TakeSingleInput;

public class CanteenSystem implements Serializable {
    private final static String ANSI_RESET = "\u001B[0m";
    private final static String ANSI_RED = "\u001B[31m";
    private final static String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private Map<String, Customer> AllCustomers = new HashMap<>();
    private Map<Category, List<Item>> AllItemsByCategory = new HashMap<>();
    private Map<String, Item> AllItemsByNames = new HashMap<>();
    private Trie ItemNames = new Trie();
//    private static final PriorityQueue<Item> itemQueue = new PriorityQueue<>((item1, item2)-> Integer.compare(item2.getNumberOfOrders(), item1.getNumberOfOrders()));
    private PriorityQueue<Item> itemQueue = new PriorityQueue<>(new ItemComparator());

    public static class ItemComparator implements Comparator<Item>, Serializable {
        @Override
        public int compare(Item item1, Item item2) {
            return Integer.compare(item2.getNumberOfOrders(), item1.getNumberOfOrders());
        }
    }

    private Admin admin;
    private long startTime;
    private int membershipCost;
    private static final String path = "IOHandling/canteen_system_data.ser";
    private static final String pathForLoginInfo = "IOHandling/LoginInfo.txt";

    public void loadState() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(path))) {
            CanteenSystem loadedData = (CanteenSystem) in.readObject();
            this.AllCustomers = loadedData.AllCustomers;
            this.AllItemsByCategory = loadedData.AllItemsByCategory;
            this.AllItemsByNames = loadedData.AllItemsByNames;
            this.ItemNames = loadedData.ItemNames;
            this.itemQueue = loadedData.itemQueue;
            this.admin = loadedData.admin;
            this.membershipCost = loadedData.membershipCost;
            this.startTime = loadedData.startTime;
        }
        catch(ClassNotFoundException e){
            System.out.println("Class not found exception thrown");
        }
        catch(IOException e){
            System.out.println("IO Exception thrown");
        }
    }

    public void saveState() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {
            out.writeObject(this);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "Error saving state: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void OpenPortal(User CurrentUser){
        System.out.println("Welcome to your account!");
        String notification = CurrentUser.getNotification();
        if(!notification.isEmpty()){
            System.out.println("Your pending notifications: ");
            System.out.print(ANSI_BLUE + notification + ANSI_RESET);
            CurrentUser.clearNotification();
        }
    }

    private String AskForPassword(Scanner sc) {
        System.out.print("Enter Password: ");
        String Password = sc.nextLine();
        System.out.print("Confirm Password: ");
        String ConfirmPassword = sc.nextLine();
        if (Password.equals(ConfirmPassword)) {
            return Password;
        } else {
            System.out.println(ANSI_RED + "Password Mismatch! Please try again" + ANSI_RESET);
            return AskForPassword(sc);
        }
    }

    private void RetrieveAccount(Scanner sc, int identifier){
        System.out.print("Enter your email: ");
        String Email = sc.nextLine();
        User TargetUser = null;
        if(identifier==0){
            TargetUser = getCustomerByEmail(Email);
        }
        else if(identifier==1){
            if(Email.equals(admin.getEmail())){
                TargetUser = admin;
            }
        }
        if(TargetUser==null){
            System.out.println(ANSI_RED  + "No User found with this email ID" + ANSI_RESET);
            System.out.println("Choose an option:\n1. Try Again\n2. Go Back");
            int option = TakeSingleInput(sc, 1, 3, "Enter an integer associated with an option to choose it: ");
            if(option==1){
                RetrieveAccount(sc, identifier);
            }
        } else{
            System.out.println("Would you like to change your password: ");
            int option = TakeSingleInput(sc, 1, 3, "Choose 1 for yes, 2 for no: ");
            if(option==1){
                String NewPassword = AskForPassword(sc);
                TargetUser.ChangePassword(NewPassword);
                System.out.println(ANSI_GREEN + "You have successfully changed your password" + ANSI_RESET);
            }
            if(identifier==0){
//                LoggedInAsCustomer((Customer) TargetUser, sc);
                ChooseBetweenGUIAndCLIForCustomer((Customer) TargetUser, sc);
            } else {
//                LoggedInAsAdmin(sc);
                ChooseBetweenGUIAndCLIForAdmin(sc);
            }
        }
    }

    private Customer getCustomerByEmail(String email) {
        email = email.toLowerCase();
        Customer foundCustomer = AllCustomers.get(email);
        return (foundCustomer != null && foundCustomer.getEmail().equalsIgnoreCase(email)) ? foundCustomer : null;
    }

    private void ViewAccount(User CurrentUser, Scanner sc){
        CurrentUser.ViewAccount();
        System.out.print("\nDo you wish to change your password: ");
        int option = TakeSingleInput(sc, 1, 3, "Choose 1 for yes and 2 for no: ");
        if(option==1){
            String newPassword = AskForPassword(sc);
            CurrentUser.ChangePassword(newPassword);
            System.out.println(ANSI_GREEN + "You have successfully changed your password" + ANSI_RESET);
        }
    }

    private void LoginAsAdmin(Scanner sc){
        if(this.admin==null){
            admin = new Admin("admin@iiitd.ac.in", "Admin", "password123");
        }
        String Email, Password;
        System.out.print("Enter your email ID: ");
        Email = sc.nextLine();
        System.out.print("Enter your password: ");
        Password = sc.nextLine();
        if(Objects.equals(Email, admin.getEmail()) && Objects.equals(Password, admin.getPassword())){
//            LoggedInAsAdmin(sc);
            ChooseBetweenGUIAndCLIForAdmin(sc);
        }
        else{
            System.out.println(ANSI_RED + "Invalid Login Credentials! Please try Again" + ANSI_RESET);
            System.out.println("Choose an option: \n1. Try Again\n2. Retrieve Account\n3. Go Back");
            int option = TakeSingleInput(sc, 1, 4, "Enter the integer associated with an option to choose it: ");
            if(option==1){
                LoginAsAdmin(sc);
            } else if(option == 2){
                RetrieveAccount(sc, 1);
            }
        }
    }

    private void HandleAdminOptions(Scanner sc){
        while(true){
            System.out.println("Choose an option:\n1. Login as an admin\n2. Go Back");
            int option = TakeSingleInput(sc, 1, 3, "Enter a serial number associated with an option to choose it: ");
            if(option == 1){
                LoginAsAdmin(sc);
            }
            else {
                return;
            }
        }
    }

    private void LoggedInAsAdmin(Scanner sc){
        while(true){
            OpenPortal(admin);
            System.out.println("Choose an option:\n1. Manage Menu\n2. Manage Order\n3. Daily Sales Report\n4. My Account\n5. Modify VIP Membership Cost\n6. Go Back");
            int option = TakeSingleInput(sc, 1, 7, "Enter the integer associated with an option to choose it: ");
            if(option==1){
                admin.MenuManagement(sc, AllItemsByCategory, AllItemsByNames, ItemNames, itemQueue);
            } else if(option == 2){
                admin.OrderManagement(sc);
            } else if(option == 3){
                admin.DailySalesReport(sc, itemQueue);
            } else if(option == 4){
                ViewAccount(admin, sc);
            } else if(option == 5){
                int cost;
                while(true){
                    System.out.print("What should be the VIP Membership cost: ");
                    String strInput = sc.nextLine();
                    try{
                        cost = Integer.parseInt(strInput);
                        if(cost<0){
                            System.out.println("Please enter a valid number");
                        } else {
                            break;
                        }
                    } catch (NumberFormatException e){
                        System.out.println(ANSI_RED + "Invalid Integer! Enter a valid integer!" + ANSI_RESET);
                    }
                }
                membershipCost = cost;
                System.out.println(ANSI_GREEN + "Successfully updated VIP Membership cost to: "  + membershipCost + ANSI_RESET);
            } else{
                return;
            }
        }
    }

    private boolean fromIIITDorNot(String Email){
        int atIndex = Email.indexOf('@');
        boolean This = atIndex>0;
        boolean That = Email.endsWith("@iiitd.ac.in");
        return This && That;
    }

    private String AskForEmail(Scanner sc){
        while(true){
            System.out.print("Enter your Email ID: ");
            String Email = sc.nextLine();
            String ModifiedEmail = Email.toLowerCase();
            if(AllCustomers.get(ModifiedEmail)!=null){
                System.out.println(ANSI_RED + Email + " is already associated with another account. Try with a different email ID"+ ANSI_RESET);
                continue;
            }
            if(ModifiedEmail.equalsIgnoreCase("admin@iiitd.ac.in")){
                System.out.println(ANSI_RED+ "You can't enter with this email ID. Try entering with a different Email ID." + ANSI_RESET);
                continue;
            }
            if(!fromIIITDorNot(ModifiedEmail)){
                System.out.println(ANSI_RED + "Try again with a valid IIIT Delhi Email ID" + ANSI_RESET);
                continue;
            }
            return ModifiedEmail;
        }
    }

    private void RegisterAsCustomer(Scanner sc){
        String Email, Password, Name;
        Email = AskForEmail(sc);
        Password = AskForPassword(sc);
        System.out.print("Enter your Name: ");
        Name = sc.nextLine();
        Customer TargetCustomer = new Customer(Email, Name, Password);
        this.storeCustomerIDPassword(TargetCustomer);
        AllCustomers.put(Email, TargetCustomer);
        System.out.println(ANSI_GREEN + "You have successfully registered to the server!" + ANSI_RESET);
//        LoggedInAsCustomer(TargetCustomer, sc);
        ChooseBetweenGUIAndCLIForCustomer(TargetCustomer, sc);
    }

    Customer AuthenticateCustomer(String Email, String Password){
        Customer TargetCustomer = getCustomerByEmail(Email);
        if(TargetCustomer!=null ){
            if(!TargetCustomer.getPassword().equals(Password)){
                TargetCustomer=null;
            }
        }
        return TargetCustomer;
    }

    private void LoginAsCustomer(Scanner sc){
        String Email, Password;
        System.out.print("Enter your email ID: ");
        Email = sc.nextLine();
        System.out.print("Enter your password: ");
        Password = sc.nextLine();
        Customer TargetCustomer = AuthenticateCustomer(Email, Password);
        if(TargetCustomer!=null){
            ChooseBetweenGUIAndCLIForCustomer(TargetCustomer, sc);
        }
        else{
            System.out.println(ANSI_RED + "Invalid Login Credentials! Please try Again" + ANSI_RESET);
            System.out.println("Choose an option: \n1. Try Again\n2. Retrieve Account\n3. Go Back");
            int option = TakeSingleInput(sc, 1, 4, "Enter the integer associated with an option to choose it: ");
            if(option==1){
                LoginAsCustomer(sc);
            } else if(option == 2){
                RetrieveAccount(sc, 0);
            }
        }
    }

    private void ChooseBetweenGUIAndCLIForCustomer(Customer CurrentCustomer, Scanner sc){
        while(true){
            System.out.println("Choose an option:\n1. CLI Based Interface\n2. GUI Based Interface\n3. Log Out");
            int option = TakeSingleInput(sc, 1, 4, "Enter the serial number associated with an option to choose it: ");
            if(CurrentCustomer.getFrame()!=null && CurrentCustomer.isFrameVisible()){
                System.out.println("Close GUI First to interact with CLI");
                continue;
            }
            if (option==1){
                LoggedInAsCustomer(CurrentCustomer, sc);
            } else if(option==2){
                CurrentCustomer.GUIBasedHomeScreen(AllItemsByCategory);
            } else{
                return;
            }
        }
    }

    private void ChooseBetweenGUIAndCLIForAdmin(Scanner sc){
        while(true){
            System.out.println("Choose an option:\n1. CLI Based Interface\n2. GUI Based Interface\n3. Log Out");
            int option = TakeSingleInput(sc, 1, 4, "Enter the serial number associated with an option to choose it: ");
            if(admin.getFrame()!=null && admin.isFrameVisible()){
                System.out.println("Close GUI First to interact with CLI");
                continue;
            }
            if (option==1){
                LoggedInAsAdmin(sc);
            } else if(option==2){
                admin.GUIBasedHomeScreen(AllItemsByCategory);
            } else{
                return;
            }
        }
    }

    private void LoggedInAsCustomer(Customer CurrentCustomer, Scanner sc){
        while(true){
            OpenPortal(CurrentCustomer);
            System.out.println("Choose an option:\n1. Canteen Menu\n2. My Orders\n3. Go To Cart\n4. My Account\n5. My Smart Card\n6. Upgrade to VIP membership\n7. Log Out");
            int option = TakeSingleInput(sc, 1, 8, "Enter the integer associated with an option to choose it: ");
            if(option==1){
                CurrentCustomer.CanteenMenu(sc, AllItemsByCategory, ItemNames, AllItemsByNames);
            } else if(option == 2){
                CurrentCustomer.OrderOperations(sc, admin, startTime, AllItemsByNames, itemQueue);
            } else if(option == 3){
                CurrentCustomer.CartOperations(sc, admin, ItemNames, AllItemsByNames, startTime, itemQueue);
            } else if(option == 4){
                ViewAccount(CurrentCustomer, sc);
            } else if (option == 5){
                CurrentCustomer.CardOperations(sc);
            } else if (option == 6){
                CurrentCustomer.UpgradeToVIP(sc, membershipCost);
            } else {
                return;
            }
        }
    }

    private void HandleCustomerOptions(Scanner sc){
        while(true){
            System.out.println("I am:\n1. A New User\n2. An Existing User\n3. Go Back");
            int option = TakeSingleInput(sc, 1, 4, "Enter a serial number associated with an option to choose it: ");
            if(option == 1){
                RegisterAsCustomer(sc);
            }
            else if (option == 2){
                LoginAsCustomer(sc);
            }
            else{
                return;
            }
        }
    }

    private void HomeScreen(Scanner sc){
        while(true){
            System.out.println("I am:\n1. A Customer\n2. An Admin\n3. Go Back");
            int option = TakeSingleInput(sc, 1,4 , "Choose an option: ");
            if(option==1){
                HandleCustomerOptions(sc);
            }
            else if (option == 2){
                HandleAdminOptions(sc);
            }
            else{
                return;
            }
        }
    }

    private void AddItemsToCanteen(Item currentItem){
        AllItemsByCategory.putIfAbsent(currentItem.getCategory(), new ArrayList<>());
        AllItemsByCategory.get(currentItem.getCategory()).add(currentItem);
        AllItemsByNames.put(currentItem.getName(), currentItem);
        ItemNames.insert(currentItem.getName());
        itemQueue.add(currentItem);
    }

    private void storeCustomerIDPassword(Customer customer) {
        try (FileWriter fileWriter = new FileWriter(pathForLoginInfo, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            String customerData = String.format("ID: %s, Password: %s, Name: %s%n",
                    customer.getEmail(), customer.getPassword(), customer.getName());

            bufferedWriter.write(customerData);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    void putIntoAllCustomer(Customer customer){
        AllCustomers.put(customer.getEmail().toLowerCase(), customer);
    }

    void removeFromAllCustomer(Customer customer){
        AllCustomers.remove(customer.getEmail().toLowerCase());
    }

    private void InitialiseCanteen(){
        membershipCost = 10000;
        startTime = System.currentTimeMillis();
        this.admin = new Admin("admin@iiitd.ac.in", "Admin", "password123");
        AddItemsToCanteen(new Item("garlic bread", 150, Category.Starters, true));
        AddItemsToCanteen(new Item("mozzarella sticks", 250, Category.Starters, false));
        AddItemsToCanteen(new Item("spring rolls", 200, Category.Starters, true));
        AddItemsToCanteen(new Item("tomato basil soup", 150, Category.Soups, true));
        AddItemsToCanteen(new Item("chicken soup", 200, Category.Soups, true));
        AddItemsToCanteen(new Item("minestrone", 180, Category.Starters, true));
        AddItemsToCanteen(new Item("caesar salad", 300, Category.Salads, false));
        AddItemsToCanteen(new Item("greek salad", 350, Category.Salads, false));
        AddItemsToCanteen(new Item("quinoa salad", 400, Category.Salads, true));
        AddItemsToCanteen(new Item("paneer butter masala", 400, Category.MainCourse, true));
        AddItemsToCanteen(new Item("veg biryani", 350, Category.MainCourse, true));
        AddItemsToCanteen(new Item("lasagna", 450, Category.MainCourse, true));
        AddItemsToCanteen(new Item("butter chicken", 500, Category.MainCourse, true));
        AddItemsToCanteen(new Item("grilled salmon", 500, Category.MainCourse, true));
        AddItemsToCanteen(new Item("lamb chops", 900, Category.MainCourse, true));
        AddItemsToCanteen(new Item("spaghetti carbonara", 450, Category.PizzaAndPasta, true));
        AddItemsToCanteen(new Item("margherita pizza", 400, Category.PizzaAndPasta, true));
        AddItemsToCanteen(new Item("penne alfredo", 450, Category.PizzaAndPasta, true));
        AddItemsToCanteen(new Item("club sandwich", 300, Category.SandwichesAndBurgers, true));
        AddItemsToCanteen(new Item("cheese burger", 350, Category.SandwichesAndBurgers, true));
        AddItemsToCanteen(new Item("veggie wrap", 250, Category.SandwichesAndBurgers, true));
        AddItemsToCanteen(new Item("fried rice", 250, Category.RiceAndNoodles, true));
        AddItemsToCanteen(new Item("pad thai", 400, Category.RiceAndNoodles, true));
        AddItemsToCanteen(new Item("chicken biryani", 400, Category.RiceAndNoodles, true));
        AddItemsToCanteen(new Item("chocolate lava cake", 250, Category.Desserts, true));
        AddItemsToCanteen(new Item("tiramisu", 300, Category.Desserts, false));
        AddItemsToCanteen(new Item("ice cream subdae", 200, Category.Desserts, true));
        AddItemsToCanteen(new Item("mojito", 300, Category.Beverages, true));
        AddItemsToCanteen(new Item("fresh juices", 150, Category.Beverages, true));
        AddItemsToCanteen(new Item("coffee", 120, Category.Beverages, false));
        AddItemsToCanteen(new Item("beer", 250, Category.Beverages, true));
        AddItemsToCanteen(new Item("smoothies", 180, Category.Beverages, false));
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        CanteenSystem DummyObj = new CanteenSystem();
        File file = new File(path);
        if(file.exists() && file.length()!=0){
            DummyObj.loadState();
        } else {
            DummyObj.InitialiseCanteen();
        }
        while(true){
            System.out.println("Welcome to IIITD Canteen Server\n1. Enter the server\n2. Exit the server");
            int option = TakeSingleInput(sc, 1,3, "Choose 1 or 2: ");
            if(option==1){
                DummyObj.HomeScreen(sc);
            }
            else{
                DummyObj.saveState();
                System.exit(0);
                return;
            }
        }
    }
}
