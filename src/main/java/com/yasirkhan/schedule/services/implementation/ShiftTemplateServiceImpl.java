package com.yasirkhan.schedule.services.implementation;

import com.yasirkhan.schedule.exceptions.DataBaseException;
import com.yasirkhan.schedule.exceptions.ResourceNotFoundException;
import com.yasirkhan.schedule.models.dtos.ShiftTemplateResponseEventDto;
import com.yasirkhan.schedule.models.entities.ShiftTemplate;
import com.yasirkhan.schedule.models.enums.Status;
import com.yasirkhan.schedule.models.enums.EventStatus;
import com.yasirkhan.schedule.models.enums.EventType;
import com.yasirkhan.schedule.repositories.ShiftTemplateRepository;
import com.yasirkhan.schedule.requests.ShiftTemplateRequest;
import com.yasirkhan.schedule.responses.ShiftTemplateResponse;
import com.yasirkhan.schedule.services.ShiftTemplateService;
import com.yasirkhan.schedule.utils.ResponseConversion;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShiftTemplateServiceImpl implements ShiftTemplateService {

    private final ShiftTemplateRepository shiftTemplateRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public ShiftTemplateServiceImpl(ShiftTemplateRepository shiftTemplateRepository,
                                    RedisTemplate<String, Object> redisTemplate,
                                    ApplicationEventPublisher eventPublisher) {
        this.shiftTemplateRepository = shiftTemplateRepository;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ShiftTemplateResponse createShiftTemplate(ShiftTemplateRequest request) {

        ShiftTemplate shiftTemplate = new ShiftTemplate();
        shiftTemplate.setShiftName(request.getShiftName());
        shiftTemplate.setStartTime(request.getStartTime());
        shiftTemplate.setEndTime(request.getEndTime());
        shiftTemplate.setRemarks(request.getRemarks());
        shiftTemplate.setStatus(Status.ACTIVE);

        try {
            ShiftTemplate savedShiftTemplate = shiftTemplateRepository.save(shiftTemplate);

            // Sync to Redis immediately
            syncShiftTemplateToRedis(savedShiftTemplate);

            ShiftTemplateResponse response = ResponseConversion.toShiftTemplateResponse(savedShiftTemplate);
            publishShiftTemplateEvent(EventType.CREATE, EventStatus.SUCCESS, response);

            return response;

        } catch (Exception e) {
            throw new DataBaseException("Failed to save Shift Template: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateShiftTemplate(Map<String, Object> updates) {
        UUID shiftTemplateId = UUID.fromString(updates.get("shiftTemplateId").toString());

        ShiftTemplate dbShiftTemplate = shiftTemplateRepository.findById(shiftTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift Template with ID: " + shiftTemplateId + " Not Found."));

        updates.forEach((key, value) -> {
            if (value != null) {
                switch (key){
                    case "shiftName" -> dbShiftTemplate.setShiftName((String) value);
                    case "startTime" -> dbShiftTemplate.setStartTime(LocalTime.parse(value.toString()));
                    case "endTime" -> dbShiftTemplate.setEndTime(LocalTime.parse(value.toString()));
                    case "remarks" -> dbShiftTemplate.setRemarks((String) value);
                    case "status" -> dbShiftTemplate.setStatus(Status.valueOf((String) value));
                }
            }
        });

        try {
            ShiftTemplate updatedShiftTemplate = shiftTemplateRepository.save(dbShiftTemplate);

            syncShiftTemplateToRedis(updatedShiftTemplate);

            publishShiftTemplateEvent(EventType.UPDATE, EventStatus.SUCCESS, ResponseConversion.toShiftTemplateResponse(updatedShiftTemplate));

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

    // Helper Methods
    private void syncShiftTemplateToRedis(ShiftTemplate template) {
        String redisKey = "wtms:template:" + template.getTemplateId().toString();
        Map<String, Object> data = new HashMap<>();

        data.put("shiftName", template.getShiftName());
        data.put("startTime", template.getStartTime() != null ? template.getStartTime().toString() : "");
        data.put("endTime", template.getEndTime() != null ? template.getEndTime().toString() : "");
        data.put("remarks", template.getRemarks() != null ? template.getRemarks() : "");
        data.put("status", template.getStatus().name());

        redisTemplate.opsForHash().putAll(redisKey, data);
    }

    private void publishShiftTemplateEvent(EventType type, EventStatus status, ShiftTemplateResponse response) {
        ShiftTemplateResponseEventDto eventDto = ShiftTemplateResponseEventDto.builder()
                .type(type)
                .eventTypeStatus(status)
                .templateData(response)
                .build();
        eventPublisher.publishEvent(eventDto);
    }
}