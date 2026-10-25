package com.taskmanager.api.controller;

import com.taskmanager.api.dto.request.TaskRequest;
import com.taskmanager.api.dto.response.TaskResponse;
import com.taskmanager.api.entity.TaskStatus;
import com.taskmanager.api.entity.User;
import com.taskmanager.api.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "CRUD operations for the authenticated user's tasks")
@PreAuthorize("isAuthenticated()")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task owned by the current user")
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request, currentUser));
    }

    @GetMapping
    @Operation(summary = "List the current user's tasks, optionally filtered by status")
    public ResponseEntity<Page<TaskResponse>> findAll(
            @RequestParam(required = false) TaskStatus status,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable
    ) {
        return ResponseEntity.ok(taskService.findAllForOwner(currentUser.getId(), status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single task by id")
    public ResponseEntity<TaskResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(taskService.findByIdForOwner(id, currentUser.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing task")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(taskService.update(id, currentUser.getId(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        taskService.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
