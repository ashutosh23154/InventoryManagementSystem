package Helper;

import Actors.Customer;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Order implements Serializable {
    private final String OrderID;
    private final Map<Item, Integer> Items;
    private OrderStatus Status;
    private final LocalDate date;
    private final float price;
    private final Customer AssociatedCustomer;
    private final String specialRequest;
    private final boolean VIPOrder;
    private final String DeliveryDetails;

    public Order(String OrderID, HashMap<Item, Integer> Items, LocalDate Date, float price, Customer AssociatedCustomer, String specialRequest, boolean VIPOrder, String Details){
        this.OrderID = OrderID;
        this.Items = Items;
        Status = OrderStatus.OrderPlaced;
        this.date = Date;
        this.price = price;
        this.AssociatedCustomer = AssociatedCustomer;
        this.specialRequest = specialRequest;
        this.VIPOrder = VIPOrder;
        DeliveryDetails = Details;
    }

    public String getDeliveryDetails(){
        return this.DeliveryDetails;
    }

    public String getSpecialRequest(){
        return this.specialRequest;
    }

    public boolean getVIPOrder(){
        return VIPOrder;
    }

    public LocalDate getDate(){
        return this.date;
    }

    public Customer getAssociatedCustomer(){
        return AssociatedCustomer;
    }

    public float getPrice(){
        return price;
    }

    public String getOrderID(){
        return this.OrderID;
    }

    public Map<Item, Integer> getItems(){
        return Items;
    }

    public void setStatus(OrderStatus order){
        Status = order;
    }

    public OrderStatus getStatus(){
        return this.Status;
    }

    public void ShowInfo() {
        System.out.println("\n=================================");
        System.out.printf("Order ID        : %s%n", OrderID);
        System.out.printf("Status          : %s%n", Status);
        System.out.printf("Date            : %s%n", date);
        System.out.printf("Customer        : %s%n", AssociatedCustomer.getName());
        System.out.printf("VIP Customer    : %s%n", AssociatedCustomer.getIsVIP());
        System.out.printf("Delivery Details: %s%n", DeliveryDetails);
        System.out.printf("Total Price     : Rs. %.2f%n", price);
        System.out.printf("Special Request : %s%n", specialRequest);
        System.out.println("=================================");

        System.out.println("\nItems in this Order:");
        for (Map.Entry<Item, Integer> current : Items.entrySet()) {
            System.out.print("  ");
            current.getKey().ShowInfo();
            System.out.printf("  Quantity Ordered : %d%n", current.getValue());
            System.out.println("---------------------------------\n");
        }
        System.out.println("=================================\n");
    }
}
