package Helper;

import java.io.Serializable;

public class Feedback implements Serializable {
    String Review;
    Integer Rating;

    public Feedback(String Review, Integer Rating){
        this.Rating = Rating;
        this.Review = Review;
    }

    @Override
    public String toString() {

        return "---------------------------------\n" +
                String.format("Rating: %s%n", Rating != -1 ? Rating : "No rating given") +
                "Review: " + Review + "\n" +
                "----------------------------------\n";
    }

}
