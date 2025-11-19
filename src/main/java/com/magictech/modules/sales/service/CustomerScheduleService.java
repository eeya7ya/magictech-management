package com.magictech.modules.sales.service;

import com.magictech.modules.sales.entity.Customer;
import com.magictech.modules.sales.entity.CustomerSchedule;
import com.magictech.modules.sales.repository.CustomerScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerScheduleService {

    @Autowired
    private CustomerScheduleRepository repository;

    public List<CustomerSchedule> getCustomerSchedules(Long customerId) {
        return repository.findByCustomerIdAndActiveTrueOrderByStartDateAsc(customerId);
    }

    public CustomerSchedule saveSchedule(CustomerSchedule schedule) {
        return repository.save(schedule);
    }

    public CustomerSchedule createSchedule(Customer customer, String taskName, LocalDate startDate, LocalDate endDate, String createdBy) {
        CustomerSchedule schedule = new CustomerSchedule();
        schedule.setCustomer(customer);
        schedule.setTaskName(taskName);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
        schedule.setCreatedBy(createdBy);
        schedule.setStatus("SCHEDULED");
        return repository.save(schedule);
    }

    public Optional<CustomerSchedule> getScheduleById(Long id) {
        return repository.findById(id);
    }

    public CustomerSchedule updateSchedule(CustomerSchedule updatedSchedule) {
        return repository.findById(updatedSchedule.getId())
                .map(existing -> {
                    existing.setTaskName(updatedSchedule.getTaskName());
                    existing.setStartDate(updatedSchedule.getStartDate());
                    existing.setEndDate(updatedSchedule.getEndDate());
                    existing.setDescription(updatedSchedule.getDescription());
                    existing.setStatus(updatedSchedule.getStatus());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + updatedSchedule.getId()));
    }

    public void deleteSchedule(Long id) {
        repository.findById(id).ifPresent(schedule -> {
            schedule.setActive(false);
            repository.save(schedule);
        });
    }

    public long getScheduleCount(Long customerId) {
        return repository.countByCustomerIdAndActiveTrue(customerId);
    }
}
