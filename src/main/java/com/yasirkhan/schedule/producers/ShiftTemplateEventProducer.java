package com.yasirkhan.schedule.producers;

import com.yasirkhan.schedule.models.dtos.ScheduleResponseEventDto;
import com.yasirkhan.schedule.models.dtos.ShiftTemplateResponseEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShiftTemplateEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ShiftTemplateEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Send Shift Template Created/Updated Response Event
    public void sendShiftTemplateResponseEvent(ShiftTemplateResponseEventDto eventDto) {
        kafkaTemplate.send("shift-template-response-topic", eventDto).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("SUCCESS: Schedule Response {} event sent for Vehicle NO: {} (Partition: {}, Offset: {})",
                        eventDto.getType(),
                        eventDto.getTemplateData().getTemplateId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("FAILED to send Vehicle Response {} event for Vehicle NO: {}. Reason: {}",
                        eventDto.getType(),
                        eventDto.getTemplateData().getTemplateId(),
                        ex.getMessage());
            }
        });
    }
}