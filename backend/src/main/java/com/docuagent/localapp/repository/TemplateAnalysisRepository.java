package com.docuagent.localapp.repository;

import com.docuagent.localapp.domain.TemplateAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateAnalysisRepository extends JpaRepository<TemplateAnalysis, Long> {

    Optional<TemplateAnalysis> findFirstByTabIdOrderByUpdatedAtDesc(Long tabId);

    void deleteAllByTabId(Long tabId);
}
