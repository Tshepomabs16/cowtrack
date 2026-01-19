package com.cowtrack.service;

import com.cowtrack.dto.request.UserRequest;
import com.cowtrack.dto.response.UserResponse;
import com.cowtrack.entity.User;

import java.util.List;

public interface UserService {
    UserResponse registerUser(UserRequest request);
    UserResponse getUserById(Long userId);
    UserResponse getUserByEmail(String email);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long userId, UserRequest request);
    void deleteUser(Long userId);
    User getAuthenticatedUser();
    List<UserResponse> getUsersByRole(String role);
}