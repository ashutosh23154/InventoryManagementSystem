import Actors.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class CanteenSystemTest {

    private static final String VALID_EMAIL = "dummy123@iiitd.ac.in";
    private static final String VALID_PASSWORD = "password123";
    private static final String INVALID_PASSWORD = "wrongpassword";
    private static final String INVALID_EMAIL = "nonexistent@iiitd.ac.in";

    private Customer validCustomer;
    private CanteenSystem canteenSystem;

    @BeforeEach
    void setUp(){
        canteenSystem = new CanteenSystem();
        validCustomer = new Customer(VALID_EMAIL, "Dummy Customer", VALID_PASSWORD);
        canteenSystem.putIntoAllCustomer(validCustomer);
    }

    @AfterEach
    void cleanUp(){
        canteenSystem.removeFromAllCustomer(validCustomer);
    }

    @Test
    void testAuthenticateCustomerWithValidCredentials() {
        Customer authenticatedCustomer = canteenSystem.AuthenticateCustomer(VALID_EMAIL, VALID_PASSWORD);
        assertNotNull(authenticatedCustomer, "Customer should be authenticated.");
        assertEquals(validCustomer, authenticatedCustomer, "Authenticated customer should be the correct one.");
        System.out.println("Customer Authenticated when given credentials are valid");
    }

    @Test
    void testAuthenticateCustomerWithInvalidEmail() {
        Customer authenticatedCustomer = canteenSystem.AuthenticateCustomer(INVALID_EMAIL, VALID_PASSWORD);
        assertNull(authenticatedCustomer, "Authentication should fail for invalid email.");
        System.out.println("Access not granted when given email is invalid");
    }

    @Test
    void testAuthenticateCustomerWithIncorrectPassword() {
        Customer authenticatedCustomer = canteenSystem.AuthenticateCustomer(VALID_EMAIL, INVALID_PASSWORD);
        assertNull(authenticatedCustomer, "Authentication should fail for incorrect password.");
        System.out.println("Access not granted when given password is invalid");
    }

    @Test
    void testAuthenticateCustomerWithCaseInsensitiveEmail() {
        Customer authenticatedCustomer = canteenSystem.AuthenticateCustomer(VALID_EMAIL.toUpperCase(), VALID_PASSWORD);
        assertNotNull(authenticatedCustomer, "Authentication should succeed with case-insensitive email.");
        assertEquals(validCustomer, authenticatedCustomer, "Authenticated customer should be the correct one.");
        System.out.println("Customer Authenticated even when given email is in upper case");
    }

    @Test
    void testAuthenticateCustomerWithCaseInsensitivePassword() {
        Customer authenticatedCustomer = canteenSystem.AuthenticateCustomer(VALID_EMAIL, VALID_PASSWORD.toUpperCase());
        assertNull(authenticatedCustomer, "Authentication should be denied with case-insensitive password.");
        System.out.println("Access not granted when given password is in upper case");
    }

    @Test
    void testAuthenticateCustomerWithInvalidEmailAndPassword() {
        Customer authenticatedCustomer = canteenSystem.AuthenticateCustomer(INVALID_EMAIL, INVALID_PASSWORD);
        assertNull(authenticatedCustomer, "Authentication should fail for both invalid email and password.");
        System.out.println("Access not granted when given credentials are invalid");
    }
}