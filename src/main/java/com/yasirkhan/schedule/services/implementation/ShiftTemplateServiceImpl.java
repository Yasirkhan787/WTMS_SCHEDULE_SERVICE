package com.yasirkhan.schedule.services.implementation;

import com.yasirkhan.schedule.exceptions.DataBaseException;
import com.yasirkhan.schedule.exceptions.ResourceNotFoundException;
import com.yasirkhan.schedule.models.entities.ShiftTemplate;
import com.yasirkhan.schedule.models.entities.Status;
import com.yasirkhan.schedule.repositories.ShiftTemplateRepository;
import com.yasirkhan.schedule.requests.ShiftTemplateRequest;
import com.yasirkhan.schedule.responses.ShiftTemplateResponse;
import com.yasirkhan.schedule.services.ShiftTemplateService;
import com.yasirkhan.schedule.utils.ResponseConversion;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShiftTemplateServiceImpl implements ShiftTemplateService {

    private final ShiftTemplateRepository shiftTemplateRepository;

    public ShiftTemplateServiceImpl(ShiftTemplateRepository shiftTemplateRepository) {
        this.shiftTemplateRepository = shiftTemplateRepository;
    }

    @Override
    @Transactional
    public ShiftTemplateResponse createShiftTemplate(ShiftTemplateRequest request) {

        // VALIDATION: Prevent Overlapping Shift Templates
        List<ShiftTemplate> existingTemplates = shiftTemplateRepository.findAll();
        for (ShiftTemplate existing : existingTemplates) {
            boolean isOverlapping = request.getStartTime().isBefore(existing.getEndTime()) &&
                    request.getEndTime().isAfter(existing.getStartTime());
            if (isOverlapping) {
                throw new IllegalArgumentException("Shift timings overlap with existing template: " + existing.getShiftName());
            }
        }

        ShiftTemplate shiftTemplate = new ShiftTemplate();
        shiftTemplate.setShiftName(request.getShiftName());
        shiftTemplate.setStartTime(request.getStartTime());
        shiftTemplate.setEndTime(request.getEndTime());
        shiftTemplate.setRemarks(request.getRemarks());
        shiftTemplate.setStatus(Status.ACTIVE);

        ShiftTemplate savedShiftTemplate = null;

        try {
            savedShiftTemplate = shiftTemplateRepository.save(shiftTemplate);

            // TODO: Store it into redis.
            // TODO: Send Kafka  event.
        } catch (Exception e) {
            throw new DataBaseException("Failed to save Shift Template: " + e.getMessage());
        }
        return ResponseConversion.toShiftTemplateResponse(savedShiftTemplate);
    }

    @Override
    @Transactional
    public void updateShiftTemplate(Map<String, Object> updates) {
        UUID shiftTemplateId = UUID.fromString(updates.get("shiftTemplateId").toString());

        ShiftTemplate dbShiftTemplate = shiftTemplateRepository.findById(shiftTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift Template with ID: " + shiftTemplateId + " Not Found."));

        updates.forEach((key, value) -> {
            switch (key){
                case "shiftName" -> dbShiftTemplate.setShiftName((String) value);
                case "startTime" -> dbShiftTemplate.setStartTime(LocalTime.parse(value.toString()));
                case "endTime" -> dbShiftTemplate.setEndTime(LocalTime.parse(value.toString()));
                case "remarks" -> dbShiftTemplate.setRemarks((String) value);
                case "status" -> dbShiftTemplate.setStatus(Status.valueOf((String) value));
            }
        });

        try {
            shiftTemplateRepository.saveAndFlush(dbShiftTemplate);
            // TODO: Store it into redis.
            // TODO: Send Kafka  event.
        } catch (Exception e){
            throw new DataBaseException(e.getMessage());
        }
    }

    @Override
    public ShiftTemplateResponse getShiftTemplateById(UUID shiftTemplateId) {
        ShiftTemplate shiftTemplate = shiftTemplateRepository.findById(shiftTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift Template with ID: " + shiftTemplateId + " Not Found."));

        return ResponseConversion.toShiftTemplateResponse(shiftTemplate);
    }

    @Override
    public List<ShiftTemplateResponse> getShiftTemplates() {
        return shiftTemplateRepository.findAll()
                .stream()
                .map(ResponseConversion::toShiftTemplateResponse)
                .collect(Collectors.toList());
    }
}