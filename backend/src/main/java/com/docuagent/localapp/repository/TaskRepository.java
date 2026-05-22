package com.docuagent.localapp.repository;

import com.docuagent.localapp.domain.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByTabIdOrderByCreatedAtDesc(Long tabId);

    void deleteAllByTabId(Long tabId);
}
