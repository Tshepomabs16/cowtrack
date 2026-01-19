package com.cowtrack.service.mapper;

import com.cowtrack.dto.request.UserRequest;
import com.cowtrack.dto.response.UserResponse;
import com.cowtrack.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        // Password will be hashed in service
        user.setPasswordHash(request.getPassword());
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
        user.setCreatedAt(java.time.LocalDateTime.now());
        return user;
    }

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());
        // Additional stats can be populated in service
        return response;
    }
}