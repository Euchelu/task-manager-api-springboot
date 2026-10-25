package com.taskmanager.api.service;

import com.taskmanager.api.dto.request.TaskRequest;
import com.taskmanager.api.dto.response.TaskResponse;
import com.taskmanager.api.entity.Task;
import com.taskmanager.api.entity.TaskPriority;
import com.taskmanager.api.entity.TaskStatus;
import com.taskmanager.api.entity.User;
import com.taskmanager.api.exception.ResourceNotFoundException;
import com.taskmanager.api.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private User owner;
    private Task existingTask;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).fullName("Ada Lovelace").email("ada@example.com").build();

        existingTask = Task.builder()
                .id(10L)
                .title("Write unit tests")
                .description("Cover TaskService with Mockito")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(3))
                .owner(owner)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("creates a task owned by the given user")
    void create_savesAndReturnsTask() {
        TaskRequest request = new TaskRequest("Write unit tests", "Cover TaskService", TaskStatus.PENDING, TaskPriority.HIGH, LocalDate.now().plusDays(3));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        TaskResponse response = taskService.create(request, owner);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("Write unit tests");
        assertThat(response.ownerId()).isEqualTo(1L);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo(owner);
    }

    @Test
    @DisplayName("returns a task when it belongs to the requesting owner")
    void findByIdForOwner_returnsTask_whenOwnedByUser() {
        when(taskRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(existingTask));

        TaskResponse response = taskService.findByIdForOwner(10L, 1L);

        assertThat(response.id()).isEqualTo(10L);
        verify(taskRepository).findByIdAndOwnerId(10L, 1L);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when the task does not belong to the owner")
    void findByIdForOwner_throws_whenNotOwnedByUser() {
        when(taskRepository.findByIdAndOwnerId(10L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findByIdForOwner(10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("10");
    }

    @Test
    @DisplayName("updates mutable fields of an owned task")
    void update_updatesFieldsAndSaves() {
        TaskRequest request = new TaskRequest("Updated title", "Updated description", TaskStatus.IN_PROGRESS, TaskPriority.LOW, LocalDate.now().plusDays(7));
        when(taskRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.update(10L, 1L, request);

        assertThat(response.title()).isEqualTo("Updated title");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.priority()).isEqualTo(TaskPriority.LOW);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when updating a task the user does not own")
    void update_throws_whenTaskNotOwned() {
        TaskRequest request = new TaskRequest("Updated title", null, null, null, null);
        when(taskRepository.findByIdAndOwnerId(10L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(10L, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("lists tasks filtered by status when a status filter is provided")
    void findAllForOwner_filtersByStatus_whenStatusProvided() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Task> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(existingTask));
        when(taskRepository.findByOwnerIdAndStatus(1L, TaskStatus.PENDING, pageable)).thenReturn(page);

        var result = taskService.findAllForOwner(1L, TaskStatus.PENDING, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(taskRepository).findByOwnerIdAndStatus(1L, TaskStatus.PENDING, pageable);
        verify(taskRepository, never()).findByOwnerId(any(), any());
    }

    @Test
    @DisplayName("lists all tasks for the owner when no status filter is provided")
    void findAllForOwner_returnsAll_whenStatusNull() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Task> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(existingTask));
        when(taskRepository.findByOwnerId(1L, pageable)).thenReturn(page);

        var result = taskService.findAllForOwner(1L, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(taskRepository).findByOwnerId(1L, pageable);
    }

    @Test
    @DisplayName("deletes an owned task")
    void delete_removesOwnedTask() {
        when(taskRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(existingTask));

        taskService.delete(10L, 1L);

        verify(taskRepository).delete(existingTask);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when deleting a task the user does not own")
    void delete_throws_whenTaskNotOwned() {
        when(taskRepository.findByIdAndOwnerId(10L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).delete(any(Task.class));
    }
}
