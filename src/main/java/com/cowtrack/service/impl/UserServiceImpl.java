package com.cowtrack.service.impl;

import com.cowtrack.dto.request.UserRequest;
import com.cowtrack.dto.response.UserResponse;
import com.cowtrack.entity.User;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.UserRepository;
import com.cowtrack.service.UserService;
import com.cowtrack.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse registerUser(UserRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }

        // Create user
        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse updateUser(Long userId, UserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setFullName(request.getFullName());
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already in use");
        }
        user.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    @Override
    public User getAuthenticatedUser() {
        // This will be implemented when we add Spring Security
        // For now, return the first user or null
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    @Override
    public List<UserResponse> getUsersByRole(String role) {
        User.Role userRole = User.Role.valueOf(role.toUpperCase());
        return userRepository.findByRole(userRole).stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }
}