package com.cowtrack;

import com.cowtrack.dto.request.UserRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {

    private final Validator validator;

    public DtoValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testUserRequestValidation() {
        // Test invalid user (empty fields)
        UserRequest invalidUser = new UserRequest();
        invalidUser.setFullName("");
        invalidUser.setEmail("invalid-email");
        invalidUser.setPassword("123");
        invalidUser.setRole("");

        Set violations = validator.validate(invalidUser);
        System.out.println("Validation errors for invalid user: " + violations.size());
        assertTrue(violations.size() > 0);

        // Test valid user
        UserRequest validUser = new UserRequest();
        validUser.setFullName("John Doe");
        validUser.setEmail("john@example.com");
        validUser.setPassword("password123");
        validUser.setRole("FARMER");

        violations = validator.validate(validUser);
        System.out.println("Validation errors for valid user: " + violations.size());
        assertEquals(0, violations.size());
    }
}