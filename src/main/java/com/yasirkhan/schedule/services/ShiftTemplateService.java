package com.yasirkhan.schedule.services;

import com.yasirkhan.schedule.requests.ShiftTemplateRequest;
import com.yasirkhan.schedule.responses.ShiftTemplateResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ShiftTemplateService {

    ShiftTemplateResponse createShiftTemplate(ShiftTemplateRequest shiftTemplateRequest);

    void updateShiftTemplate(Map<String, Object> updates);

    ShiftTemplateResponse getShiftTemplateById(UUID shiftTemplateId);

    List<ShiftTemplateResponse> getShiftTemplates();
}
