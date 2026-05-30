package com.yasirkhan.schedule.repositories;

import com.yasirkhan.schedule.models.entities.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, UUID> {
}
