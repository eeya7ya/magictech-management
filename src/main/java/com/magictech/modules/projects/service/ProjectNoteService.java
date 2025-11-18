package com.magictech.modules.projects.service;

import com.magictech.modules.projects.entity.ProjectNote;
import com.magictech.modules.projects.repository.ProjectNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProjectNoteService {

    @Autowired
    private ProjectNoteRepository repository;

    public List<ProjectNote> getNotesByProject(Long projectId) {
        return repository.findByProjectIdOrderByLastUpdatedDesc(projectId);
    }

    public ProjectNote createNote(ProjectNote note) {
        return repository.save(note);
    }

    public ProjectNote updateNote(Long id, ProjectNote updated) {
        ProjectNote existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        existing.setNoteTitle(updated.getNoteTitle());
        existing.setImportantDescription(updated.getImportantDescription());
        existing.setNoteType(updated.getNoteType());

        return repository.save(existing);
    }

    public void deleteNote(Long id) {
        repository.deleteById(id);
    }
}