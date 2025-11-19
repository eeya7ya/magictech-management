package com.magictech.modules.sales.service;

import com.magictech.modules.sales.entity.Customer;
import com.magictech.modules.sales.entity.CustomerTask;
import com.magictech.modules.sales.repository.CustomerTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerTaskService {

    @Autowired
    private CustomerTaskRepository repository;

    public List<CustomerTask> getCustomerTasks(Long customerId) {
        return repository.findByCustomerIdAndActiveTrue(customerId);
    }

    public List<CustomerTask> getPendingTasks(Long customerId) {
        return repository.findByCustomerIdAndIsCompletedAndActiveTrue(customerId, false);
    }

    public List<CustomerTask> getCompletedTasks(Long customerId) {
        return repository.findByCustomerIdAndIsCompletedAndActiveTrue(customerId, true);
    }

    public CustomerTask saveTask(CustomerTask task) {
        return repository.save(task);
    }

    public CustomerTask createTask(Customer customer, String title, String details, String priority, String createdBy) {
        CustomerTask task = new CustomerTask();
        task.setCustomer(customer);
        task.setTaskTitle(title);
        task.setTaskDetails(details);
        task.setPriority(priority);
        task.setCreatedBy(createdBy);
        return repository.save(task);
    }

    public Optional<CustomerTask> getTaskById(Long id) {
        return repository.findById(id);
    }

    public CustomerTask updateTask(CustomerTask updatedTask) {
        return repository.findById(updatedTask.getId())
                .map(existing -> {
                    existing.setTaskTitle(updatedTask.getTaskTitle());
                    existing.setTaskDetails(updatedTask.getTaskDetails());
                    existing.setIsCompleted(updatedTask.getIsCompleted());
                    existing.setPriority(updatedTask.getPriority());
                    existing.setDueDate(updatedTask.getDueDate());
                    existing.setAssignedTo(updatedTask.getAssignedTo());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Task not found: " + updatedTask.getId()));
    }

    public void deleteTask(Long id) {
        repository.findById(id).ifPresent(task -> {
            task.setActive(false);
            repository.save(task);
        });
    }

    public long getPendingTaskCount(Long customerId) {
        return repository.countByCustomerIdAndIsCompletedAndActiveTrue(customerId, false);
    }

    public long getCompletedTaskCount(Long customerId) {
        return repository.countByCustomerIdAndIsCompletedAndActiveTrue(customerId, true);
    }
}
