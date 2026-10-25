package com.taskmanager.api.dto.response;

import com.taskmanager.api.entity.Role;
import com.taskmanager.api.entity.User;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
