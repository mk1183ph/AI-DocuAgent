package com.docuagent.localapp.repository;

import com.docuagent.localapp.domain.Tab;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TabRepository extends JpaRepository<Tab, Long> {

    List<Tab> findAllByOrderByCreatedAtDesc();
}
