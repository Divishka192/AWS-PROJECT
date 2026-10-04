package com.example.eventnotification.service;

import com.example.eventnotification.model.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/** Publishes ONE event to the EXISTING SNS topic (OrderEvents). Never creates a topic. */
@Service
public class SnsPublisherService {

    private static final Logger log = LoggerFactory.getLogger(SnsPublisherService.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    public SnsPublisherService(SnsClient snsClient,
                               ObjectMapper objectMapper,
                               @Value("${sns.topic.arn}") String topicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    /** @throws EventPublishException if the event cannot be converted or published */
    public String publish(OrderEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);

            PublishResponse response = snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(json)
                    .build());

            log.info("Published {} for order {} to SNS topic OrderEvents (messageId={})",
                    event.getEvent(), event.getOrderId(), response.messageId());
            return response.messageId();
        } catch (Exception e) {
            // Only the message is logged - never credentials.
            log.error("Failed to publish order {} to SNS: {}", event.getOrderId(), e.getMessage());
            throw new EventPublishException("Failed to publish event to SNS", e);
        }
    }
}
