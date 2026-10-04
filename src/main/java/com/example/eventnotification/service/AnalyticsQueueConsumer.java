package com.example.eventnotification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Demo consumer for the existing queue OrderAnalyticsQueue */
@Service
public class AnalyticsQueueConsumer extends AbstractQueueConsumer {

    public AnalyticsQueueConsumer(SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${sqs.analytics.queue-url}") String queueUrl) {
        super(sqsClient, objectMapper, queueUrl, "ANALYTICS CONSUMER");
    }

    @Override
    protected void process(JsonNode event) {
        System.out.println("================================");
        System.out.println("ANALYTICS CONSUMER");
        System.out.println("================================");
        System.out.println("Order received: " + event.path("orderId").asText());
        System.out.println("Amount: " + event.path("amount").asText());
        System.out.println("Analytics event processed");
        System.out.println("================================");
    }
}
