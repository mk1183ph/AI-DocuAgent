package com.docuagent.localapp.repository;

import com.docuagent.localapp.domain.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, Long> {
}
