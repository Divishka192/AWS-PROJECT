package com.example.eventnotification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

/**
 * Simple polling loop shared by the 3 consumers. Each consumer runs in its OWN thread,
 * so they process the same event independently.
 *
 * - Success -> message is deleted from the queue.
 * - Failure -> message is NOT deleted (it reappears after the visibility timeout).
 */
public abstract class AbstractQueueConsumer {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;
    private final String consumerName;

    private volatile boolean running = true;
    private Thread worker;

    protected AbstractQueueConsumer(SqsClient sqsClient, ObjectMapper objectMapper,
                                    String queueUrl, String consumerName) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
        this.consumerName = consumerName;
    }

    /** Business logic of the consumer. Throw an exception to keep the message in the queue. */
    protected abstract void process(JsonNode event);

    @PostConstruct
    public void start() {
        worker = new Thread(this::pollLoop, consumerName.replace(' ', '-') + "-poller");
        worker.setDaemon(true);
        worker.start();
        log.info("{} started polling its SQS queue", consumerName);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void pollLoop() {
        while (running) {
            try {
                ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(5)
                        .waitTimeSeconds(10) // long polling
                        .build();

                List<Message> messages = sqsClient.receiveMessage(request).messages();

                for (Message message : messages) {
                    handle(message);
                }
            } catch (Exception e) {
                if (!running) {
                    return;
                }
                log.error("{}: error while polling queue: {}", consumerName, e.getMessage());
                sleep(5000);
            }
        }
    }

    private void handle(Message message) {
        try {
            JsonNode event = extractEvent(message.body());
            process(event);

            // Delete ONLY after successful processing
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception e) {
            log.error("{}: processing failed, message NOT deleted: {}", consumerName, e.getMessage());
        }
    }

    /**
     * SNS wraps the event in an envelope ({"Type":"Notification","Message":"{...}"})
     * unless "Raw message delivery" is enabled. This handles BOTH cases.
     */
    private JsonNode extractEvent(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (root.has("Type") && root.has("Message") && root.get("Message").isTextual()) {
            return objectMapper.readTree(root.get("Message").asText());
        }
        return root;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
