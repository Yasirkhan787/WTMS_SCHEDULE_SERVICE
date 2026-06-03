package com.yasirkhan.schedule.listener;

import com.yasirkhan.schedule.models.dtos.ShiftTemplateResponseEventDto;
import com.yasirkhan.schedule.producers.ShiftTemplateEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ShiftTemplateEventListener {

    private final ShiftTemplateEventProducer producer;

    public ShiftTemplateEventListener(ShiftTemplateEventProducer producer) {
        this.producer = producer;
    }

    /**
     * Listens for RouteResponseEventDto events published by the RouteService.
     * The phase = TransactionPhase.AFTER_COMMIT ensures Kafka is only called
     * if the database transaction was successful.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRouteResponseEvent(ShiftTemplateResponseEventDto eventDto) {
        try {
            producer.sendShiftTemplateResponseEvent(eventDto);
            log.info("Successfully published Kafka event for Shift Template ID: {}", eventDto.getTemplateData().getTemplateId());
        } catch (Exception e) {
            // At this point, the DB commit is already done.
            // If Kafka is down, it won't roll back the database, but we MUST log it
            // deeply so an admin can manually reconcile the missed event,
            // or you can implement a retry mechanism (like Spring Retry) here.
            log.error("CRITICAL: Failed to publish Kafka event for Shift Template ID: {}. Reason: {}",
                    eventDto.getTemplateData().getTemplateId(), e.getMessage(), e);
        }
    }
}