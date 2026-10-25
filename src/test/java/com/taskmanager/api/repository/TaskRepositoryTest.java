package com.taskmanager.api.repository;

import com.taskmanager.api.entity.Role;
import com.taskmanager.api.entity.Task;
import com.taskmanager.api.entity.TaskPriority;
import com.taskmanager.api.entity.TaskStatus;
import com.taskmanager.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TaskRepository")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .fullName("Ada Lovelace")
                .email("ada@example.com")
                .password("hashed")
                .role(Role.ROLE_USER)
                .build());

        otherUser = userRepository.save(User.builder()
                .fullName("Alan Turing")
                .email("alan@example.com")
                .password("hashed")
                .role(Role.ROLE_USER)
                .build());

        taskRepository.save(Task.builder()
                .title("Pending task")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .owner(owner)
                .build());

        taskRepository.save(Task.builder()
                .title("Completed task")
                .status(TaskStatus.COMPLETED)
                .priority(TaskPriority.LOW)
                .owner(owner)
                .build());

        taskRepository.save(Task.builder()
                .title("Someone else's task")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .owner(otherUser)
                .build());
    }

    @Test
    @DisplayName("finds only tasks belonging to the given owner")
    void findByOwnerId_returnsOnlyOwnedTasks() {
        Page<Task> page = taskRepository.findByOwnerId(owner.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(task -> task.getOwner().getId().equals(owner.getId()));
    }

    @Test
    @DisplayName("filters owned tasks by status")
    void findByOwnerIdAndStatus_filtersCorrectly() {
        Page<Task> page = taskRepository.findByOwnerIdAndStatus(owner.getId(), TaskStatus.PENDING, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Pending task");
    }

    @Test
    @DisplayName("does not find a task owned by a different user")
    void findByIdAndOwnerId_returnsEmpty_forOtherUsersTask() {
        Task othersTask = taskRepository.findByOwnerId(otherUser.getId(), PageRequest.of(0, 10)).getContent().get(0);

        Optional<Task> result = taskRepository.findByIdAndOwnerId(othersTask.getId(), owner.getId());

        assertThat(result).isEmpty();
    }
}
