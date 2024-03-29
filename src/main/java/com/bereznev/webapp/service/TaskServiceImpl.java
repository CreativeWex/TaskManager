package com.bereznev.webapp.service;
/*
    =====================================
    @project TaskManager
    @created 06/02/2023    
    @author Bereznev Nikita @CreativeWex
    =====================================
*/

import com.bereznev.webapp.exception.ResourceNotFoundException;
import com.bereznev.webapp.exception.SessionOpeningException;
import com.bereznev.webapp.model.Task;
import com.bereznev.webapp.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    @PersistenceContext
    private EntityManager entityManager;

    private final TaskRepository taskRepository;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @CachePut(value = "tasks", key = "#result.id")
    public Task save(Task task) {
        return taskRepository.save(task);
    }

    @Override
    @CachePut(value = "tasks", key = "#id")
    public Task update(Long id, Task updatedTask) {
        Task existedTask = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", "Id", id));

        existedTask.setName(updatedTask.getName());
        existedTask.setDate(updatedTask.getDate());
        existedTask.setDescription(updatedTask.getDescription());

        taskRepository.save(existedTask);
        return existedTask;
    }

    @Override
    @CacheEvict(value = "tasks", key = "#id")
    public void delete(Long id) {
        taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", "Id", id));
        taskRepository.deleteById(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Cacheable(value = "finishedTasks")
    public List<Task> findFinishedTasks() {
        if (entityManager == null || entityManager.unwrap(Session.class) == null) {
            throw new SessionOpeningException("findFinishedTasks()");
        }
        return entityManager.createQuery("select task from Task task where task.isActive = false").getResultList();
    }

    @Override
    @Cacheable(value = "tasks", key = "#id")
    public Task findById(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", "Id", id));
    }

    @Override
    @SuppressWarnings("unchecked")
    @Cacheable(value = "activeTasks")
    public List<Task> findActiveTasks() {
        if (entityManager == null || entityManager.unwrap(Session.class) == null) {
            throw new SessionOpeningException("findActiveTasks()");
        }
        return entityManager.createQuery("select task from Task task where task.isActive = true order by task.isImportant desc").getResultList();
    }

    @Override
    @CacheEvict(value = {"activeTasks", "finishedTasks"}, allEntries = true)    public void switchActiveStatus(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", "Id", id));
        task.setActive(!task.isActive());
        taskRepository.save(task);
    }

    @Override
    @CacheEvict(value = "activeTasks", allEntries = true)
    public void switchImportantStatus(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", "Id", id));
        task.setImportant(!task.isImportant());
        taskRepository.save(task);
    }

    @Override
    public boolean isAlreadyPresent(Long id, String name) {
        if (entityManager == null || entityManager.unwrap(Session.class) == null) {
            throw new SessionOpeningException("isAlreadyPresent(Long id, String name)");
        }
        return entityManager.createQuery("select task from Task task where task.name = ?1 and task.isActive = true")
                .setParameter(1, name)
                .getResultStream()
                .findFirst()
                .isPresent();
    }
}
