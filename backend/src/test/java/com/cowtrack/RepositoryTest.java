package com.cowtrack;

import com.cowtrack.entity.User;
import com.cowtrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testUserRepository() {
        // Create a test user
        User user = new User();
        user.setFullName("Test Farmer");
        user.setEmail("test@cowtrack.com");
        user.setPasswordHash("hashed_password");
        user.setRole(User.Role.FARMER);

        // Save to database
        User savedUser = userRepository.save(user);

        // Verify
        System.out.println("Saved User ID: " + savedUser.getUserId());
        System.out.println("Total users: " + userRepository.count());

        // Clean up (optional)
        userRepository.delete(savedUser);
    }
}