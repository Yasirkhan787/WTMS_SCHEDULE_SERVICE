package com.yasirkhan.schedule.listener;

import com.yasirkhan.schedule.producers.ScheduleEventProducer;
import com.yasirkhan.schedule.models.dtos.ScheduleResponseEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ScheduleEventListener {

    private final ScheduleEventProducer producer;

    public ScheduleEventListener(ScheduleEventProducer producer) {
        this.producer = producer;
    }

    // This ensures Kafka message is sent ONLY AFTER DB commit is successful
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScheduleResponseEvent(ScheduleResponseEventDto eventDto) {
        try {
            producer.sendScheduleResponseEvent(eventDto);
            log.info("Successfully published Kafka event for Schedule ID: {}", eventDto.getScheduleData().getScheduleId());
        } catch (Exception e) {
            // Note: Since DB is already committed, if Kafka fails here,
            // you might want to log it deeply or implement a retry mechanism.
            log.error("Failed to publish Kafka event for Schedule ID: {}", eventDto.getScheduleData().getScheduleId(), e);
        }
    }
}