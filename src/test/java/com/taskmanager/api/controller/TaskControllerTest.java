package com.taskmanager.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.api.dto.request.TaskRequest;
import com.taskmanager.api.dto.response.TaskResponse;
import com.taskmanager.api.entity.Role;
import com.taskmanager.api.entity.TaskPriority;
import com.taskmanager.api.entity.TaskStatus;
import com.taskmanager.api.entity.User;
import com.taskmanager.api.exception.ResourceNotFoundException;
import com.taskmanager.api.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TaskController")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private User currentUser;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).fullName("Ada Lovelace").email("ada@example.com").role(Role.ROLE_USER).build();
        taskResponse = new TaskResponse(10L, "Write unit tests", "Cover controllers", TaskStatus.PENDING,
                TaskPriority.HIGH, LocalDate.now().plusDays(3), 1L, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("POST /api/v1/tasks creates a task for the authenticated user")
    void create_returns201() throws Exception {
        TaskRequest request = new TaskRequest("Write unit tests", "Cover controllers", TaskStatus.PENDING, TaskPriority.HIGH, LocalDate.now().plusDays(3));
        when(taskService.create(any(TaskRequest.class), eq(currentUser))).thenReturn(taskResponse);

        mockMvc.perform(post("/api/v1/tasks")
                        .with(user(currentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Write unit tests"));
    }

    @Test
    @DisplayName("POST /api/v1/tasks returns 400 when the title is blank")
    void create_returns400_whenTitleBlank() throws Exception {
        TaskRequest invalidRequest = new TaskRequest("", null, null, null, null);

        mockMvc.perform(post("/api/v1/tasks")
                        .with(user(currentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/tasks returns a page of the current user's tasks")
    void findAll_returnsPageOfTasks() throws Exception {
        Page<TaskResponse> page = new PageImpl<>(List.of(taskResponse), PageRequest.of(0, 10), 1);
        when(taskService.findAllForOwner(eq(1L), eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/tasks").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} returns 200 when the task exists")
    void findById_returns200_whenFound() throws Exception {
        when(taskService.findByIdForOwner(10L, 1L)).thenReturn(taskResponse);

        mockMvc.perform(get("/api/v1/tasks/10").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} returns 404 when the task does not exist")
    void findById_returns404_whenNotFound() throws Exception {
        when(taskService.findByIdForOwner(99L, 1L)).thenThrow(new ResourceNotFoundException("Task not found with id: 99"));

        mockMvc.perform(get("/api/v1/tasks/99").with(user(currentUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/tasks/{id} updates an existing task")
    void update_returns200() throws Exception {
        TaskRequest request = new TaskRequest("Updated title", "Updated", TaskStatus.IN_PROGRESS, TaskPriority.LOW, null);
        TaskResponse updated = new TaskResponse(10L, "Updated title", "Updated", TaskStatus.IN_PROGRESS,
                TaskPriority.LOW, null, 1L, Instant.now(), Instant.now());
        when(taskService.update(eq(10L), eq(1L), any(TaskRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/tasks/10")
                        .with(user(currentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} returns 204 on success")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/10").with(user(currentUser)))
                .andExpect(status().isNoContent());
    }
}
