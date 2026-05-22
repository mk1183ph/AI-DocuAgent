package com.docuagent.localapp.repository;

import com.docuagent.localapp.domain.Document;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByTaskIdOrderByCreatedAtDesc(Long taskId);

    Optional<Document> findFirstByTaskIdOrderByCreatedAtDesc(Long taskId);

    void deleteAllByTaskIdIn(Collection<Long> taskIds);
}
