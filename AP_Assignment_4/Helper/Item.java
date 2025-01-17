package Helper;

import Actors.Customer;
import Actors.User;

import java.io.Serializable;
import java.util.*;

public class Item implements Serializable {
    private String Name;
    private float Price;
    private boolean isAvailable;
    private Category category;
    private final Map<Customer, Feedback> feedbackMap;
    private int numberOfOrders;

    public Item(String Name, float Price, Category category, boolean isAvailable){
        this.Name = Name;
        this.Price = Price;
        this.isAvailable = isAvailable;
        this.category = category;
        feedbackMap = new HashMap<>();
        numberOfOrders = 0;
    }

    public Item(Item obj) {
        this.Name = obj.Name;
        this.Price = obj.Price;
        this.isAvailable = obj.isAvailable;
        this.category = obj.category;
        this.feedbackMap = obj.feedbackMap;
        this.numberOfOrders = obj.numberOfOrders;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Item item = (Item) obj;
        return Name.equals(item.Name);
    }

    @Override
    public int hashCode() {
        return Name.hashCode();
    }

    public void incrementNumberOfOrders(int num, PriorityQueue<Item> queue){
        queue.remove(this);
        this.numberOfOrders += num;
        queue.add(this);
    }

    public void decrementNumberOfOrders(int num, PriorityQueue<Item> queue){
        queue.remove(this);
        this.numberOfOrders -= num;
        queue.add(this);
    }

    public int getNumberOfOrders(){
        return this.numberOfOrders;
    }

    public void setPrice(float Price){
        this.Price = Price;
    }

    public boolean getIsAvailable(){
        return this.isAvailable;
    }

    public void setAvailability(boolean value){
        isAvailable = value;
    }

    public void setCategory(Category category){
        this.category = category;
    }

    public String getName(){
        return this.Name;
    }

    public float getPrice(){
        return this.Price;
    }

    public void ShowInfo() {
        System.out.println("=================================");
        System.out.printf("Name         : %s%n", Name);
        System.out.printf("Price        : Rs. %.2f%n", Price);
        System.out.printf("Category     : %s%n", category);
        System.out.printf("Availability : %s%n", isAvailable);
        System.out.println("=================================\n");
    }

    public Category getCategory(){
        return this.category;
    }

    public void ChangeName(String name){
        this.Name = name;
    }

    private Feedback GenerateFeedback(Scanner sc){
        int Rating;
        String Review;

        while(true){
            Rating = User.TakeSingleInput(sc, -1, 6, "How would you rate " + this.getName() + " between 1 to 5 (-1 to skip rating): ");
            if(Rating!=0){
                break;
            }
            System.out.println("Please rate between 1 to 5");
        }

        System.out.print("Enter the review: ");
        Review = sc.nextLine();
        return new Feedback(Review, Rating);
    }


    public void GiveFeedback(Scanner sc, Customer CurrentCustomer){
        if(feedbackMap.get(CurrentCustomer)!=null){
            System.out.println(User.ANSI_RED + "You have already given a feedback to this item. So, you can't give a new feedback again. Consider editing the previous one!" + User.ANSI_RESET);
            return;
        }
        this.feedbackMap.put(CurrentCustomer, (GenerateFeedback(sc)));
        System.out.println(User.ANSI_GREEN + "You have successfully given a feedback to " + this.getName() + User.ANSI_RESET);
    }

    public void EditFeedback(Scanner sc, Customer CurrentCustomer){
        Feedback Target = this.feedbackMap.get(CurrentCustomer);
        if(Target==null){
            System.out.println("You have not given any feedback to " + this.Name + " yet!");
            return;
        }
        Target = GenerateFeedback(sc);
        feedbackMap.put(CurrentCustomer, Target);
        System.out.println(User.ANSI_GREEN + "You have successfully edited your feedback for " + this.getName() + User.ANSI_RESET);
    }

    public void DeleteFeedback(Customer CurrentCustomer){
        Feedback Target = this.feedbackMap.get(CurrentCustomer);
        if(Target==null){
            System.out.println("You have not given a feedback to " + this.getName() +  "yet!");
            return;
        }
        this.feedbackMap.remove(CurrentCustomer);
        System.out.println(User.ANSI_GREEN + "Successfully deleted your feedback for " + this.Name + User.ANSI_RESET);
    }

    public void ViewAllFeedbacks(){
        if(feedbackMap.isEmpty()){
            System.out.println("No customer have given any feedback to this item yet!");
            return;
        }
        int i=1;
        for(Map.Entry<Customer, Feedback> entry: this.feedbackMap.entrySet()){
            System.out.println(i++ + ". Customer Name: " + entry.getKey().getName());
            System.out.println(entry.getValue());
        }
    }
}
