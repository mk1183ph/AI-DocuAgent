package com.docuagent.localapp.repository;

import com.docuagent.localapp.domain.TemplateFieldMapping;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateFieldMappingRepository extends JpaRepository<TemplateFieldMapping, Long> {

    List<TemplateFieldMapping> findAllByTabIdOrderByCreatedAtAsc(Long tabId);

    void deleteAllByTabId(Long tabId);
}
