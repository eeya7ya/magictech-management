package com.magictech.modules.sales.service;

import com.magictech.modules.sales.entity.Customer;
import com.magictech.modules.sales.entity.CustomerNote;
import com.magictech.modules.sales.repository.CustomerNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerNoteService {

    @Autowired
    private CustomerNoteRepository repository;

    public List<CustomerNote> getCustomerNotes(Long customerId) {
        return repository.findByCustomerIdAndActiveTrueOrderByDateAddedDesc(customerId);
    }

    public CustomerNote saveNote(CustomerNote note) {
        return repository.save(note);
    }

    public CustomerNote createNote(Customer customer, String content, String createdBy) {
        CustomerNote note = new CustomerNote();
        note.setCustomer(customer);
        note.setNoteContent(content);
        note.setCreatedBy(createdBy);
        return repository.save(note);
    }

    public Optional<CustomerNote> getNoteById(Long id) {
        return repository.findById(id);
    }

    public CustomerNote updateNote(Long id, String newContent) {
        return repository.findById(id)
                .map(note -> {
                    note.setNoteContent(newContent);
                    return repository.save(note);
                })
                .orElseThrow(() -> new RuntimeException("Note not found: " + id));
    }

    public void deleteNote(Long id) {
        repository.findById(id).ifPresent(note -> {
            note.setActive(false);
            repository.save(note);
        });
    }

    public long getNoteCount(Long customerId) {
        return repository.countByCustomerIdAndActiveTrue(customerId);
    }
}
