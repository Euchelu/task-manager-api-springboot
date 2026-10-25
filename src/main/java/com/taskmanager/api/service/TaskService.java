package com.taskmanager.api.service;

import com.taskmanager.api.dto.request.TaskRequest;
import com.taskmanager.api.dto.response.TaskResponse;
import com.taskmanager.api.entity.Task;
import com.taskmanager.api.entity.TaskStatus;
import com.taskmanager.api.entity.User;
import com.taskmanager.api.exception.ResourceNotFoundException;
import com.taskmanager.api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponse create(TaskRequest request, User owner) {
        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status())
                .priority(request.priority())
                .dueDate(request.dueDate())
                .owner(owner)
                .build();

        return TaskResponse.from(taskRepository.save(task));
    }

    public Page<TaskResponse> findAllForOwner(Long ownerId, TaskStatus status, Pageable pageable) {
        Page<Task> tasks = status == null
                ? taskRepository.findByOwnerId(ownerId, pageable)
                : taskRepository.findByOwnerIdAndStatus(ownerId, status, pageable);

        return tasks.map(TaskResponse::from);
    }

    public TaskResponse findByIdForOwner(Long id, Long ownerId) {
        return TaskResponse.from(getOwnedTaskOrThrow(id, ownerId));
    }

    @Transactional
    public TaskResponse update(Long id, Long ownerId, TaskRequest request) {
        Task task = getOwnedTaskOrThrow(id, ownerId);

        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDueDate(request.dueDate());

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id, Long ownerId) {
        Task task = getOwnedTaskOrThrow(id, ownerId);
        taskRepository.delete(task);
    }

    private Task getOwnedTaskOrThrow(Long id, Long ownerId) {
        return taskRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }
}
