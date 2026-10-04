package com.example.eventnotification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Demo consumer for the existing queue OrderNotificationQueue */
@Service
public class NotificationQueueConsumer extends AbstractQueueConsumer {

    public NotificationQueueConsumer(SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${sqs.notification.queue-url}") String queueUrl) {
        super(sqsClient, objectMapper, queueUrl, "NOTIFICATION CONSUMER");
    }

    @Override
    protected void process(JsonNode event) {
        System.out.println("================================");
        System.out.println("NOTIFICATION CONSUMER");
        System.out.println("================================");
        System.out.println("Order received: " + event.path("orderId").asText());
        System.out.println("Notification processed");
        System.out.println("================================");
    }
}
