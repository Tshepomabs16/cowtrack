package com.cowtrack.controller;

import com.cowtrack.dto.request.UserRequest;
import com.cowtrack.dto.response.UserResponse;
import com.cowtrack.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRequest request) {
        UserResponse user = userService.registerUser(request);
        return created(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        UserResponse user = userService.getUserById(userId);
        return success(user);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return success(user);
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return success(users);
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        List<UserResponse> users = userService.getUsersByRole(role);
        return success(users);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest request) {
        UserResponse user = userService.updateUser(userId, request);
        return success("User updated successfully", user);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return success("User deleted successfully", null);
    }

    @PostMapping("/{cowId}/assign-caretaker/{caretakerId}")
    public ResponseEntity<?> assignCaretakerToCow(
            @PathVariable Long cowId,
            @PathVariable Long caretakerId) {
        // This should be in CowController, but for simplicity
        return success("Caretaker assignment endpoint - implement in CowController", null);
    }
}