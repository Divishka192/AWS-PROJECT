package com.example.eventnotification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Demo consumer for the existing queue OrderEmailQueue */
@Service
public class EmailQueueConsumer extends AbstractQueueConsumer {

    public EmailQueueConsumer(SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${sqs.email.queue-url}") String queueUrl) {
        super(sqsClient, objectMapper, queueUrl, "EMAIL CONSUMER");
    }

    @Override
    protected void process(JsonNode event) {
        System.out.println("================================");
        System.out.println("EMAIL CONSUMER");
        System.out.println("================================");
        System.out.println("Order received: " + event.path("orderId").asText());
        System.out.println("Customer: " + event.path("customer").asText());
        System.out.println("Product: " + event.path("product").asText());
        System.out.println("Email notification processed");
        System.out.println("================================");
    }
}
