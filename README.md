# Amazon SQS and SNS Fan-Out for Event Notifications

A beginner-friendly **Spring Boot 3.3 / Java 17 / Maven** project that demonstrates the **SNS Fan-Out** pattern.

> **One event is published once to SNS, and SNS fans the event out to multiple SQS queues so different consumers can process the same event independently.**

---

## 1. Project objective
Build a small event-driven Order Notification System: when an order is created, ONE event is published to Amazon SNS and three independent consumers each receive and process it.

## 2. Problem statement
If the Order API called the email service, the analytics service and the notification service directly, it would be tightly coupled: adding a new service means changing the API, and one slow or failing service would slow down or break the order flow.

## 3. Solution
The API only publishes one `ORDER_CREATED` event to an SNS topic. SNS copies the event into three SQS queues. Each queue has its own consumer, so every consumer works independently, at its own speed, and a failure in one does not affect the others.

## 4. Architecture

```
                         POSTMAN
                            |
                       POST /orders
                            |
                            v
                    SPRING BOOT API
                            |
                 +----------+----------+
                 |                     |
                 v                     v
          PostgreSQL Database       SNS Topic
         (event_notification_db)    OrderEvents
                                       |
                    +------------------+------------------+
                    |                  |                  |
                    v                  v                  v
             OrderEmailQueue   OrderAnalyticsQueue  OrderNotificationQueue
                    |                  |                  |
                    v                  v                  v
             Email Consumer     Analytics Consumer   Notification Consumer
```

## 5. Technologies
Java 17, Spring Boot 3.3.5, Maven, Spring Web, Spring Data JPA, PostgreSQL, AWS SDK for Java v2 (SNS + SQS), Jackson, Jakarta Validation.

## 6. AWS resources (ALREADY CREATED - this project creates nothing in AWS)

| Type | Name |
|------|------|
| Region | `ap-south-1` |
| SNS topic | `OrderEvents` |
| SQS queue | `OrderEmailQueue` |
| SQS queue | `OrderAnalyticsQueue` |
| SQS queue | `OrderNotificationQueue` |

All 3 queues are already subscribed to `OrderEvents` (protocol SQS, status Confirmed). The application **only publishes to SNS and reads from SQS**. It never creates topics, queues, subscriptions or queue policies.

## 7. PostgreSQL setup (pgAdmin)
pgAdmin is only the management tool; the database is PostgreSQL.

1. Open **pgAdmin** and connect to your local server (host `localhost`, port `5432`, user `postgres`).
2. Right-click **Databases > Create > Database...** or open **Query Tool** on the `postgres` database.
3. Run:

```sql
CREATE DATABASE event_notification_db;
```

The app does NOT create the database. It creates/updates the `orders` table automatically (`spring.jpa.hibernate.ddl-auto=update`). The table is called `orders` because `ORDER` is a reserved SQL keyword.

## 8. Environment variables (Windows PowerShell)
Run these in the **same PowerShell window** you start the app from:

```powershell
$env:DB_PASSWORD="YOUR_POSTGRES_PASSWORD"
$env:AWS_REGION="ap-south-1"
$env:SNS_TOPIC_ARN="YOUR_EXISTING_ORDER_EVENTS_TOPIC_ARN"
$env:EMAIL_QUEUE_URL="YOUR_EXISTING_ORDER_EMAIL_QUEUE_URL"
$env:ANALYTICS_QUEUE_URL="YOUR_EXISTING_ORDER_ANALYTICS_QUEUE_URL"
$env:NOTIFICATION_QUEUE_URL="YOUR_EXISTING_ORDER_NOTIFICATION_QUEUE_URL"
```

Where to find the values (AWS Console):
- **SNS topic ARN**: SNS > Topics > `OrderEvents` > ARN (looks like `arn:aws:sns:ap-south-1:<account-id>:OrderEvents`)
- **Queue URLs**: SQS > Queues > select the queue > URL (looks like `https://sqs.ap-south-1.amazonaws.com/<account-id>/OrderEmailQueue`)

Optional read-only checks with the AWS CLI:
```powershell
aws sns list-topics --region ap-south-1
aws sqs list-queues --region ap-south-1
aws sns list-subscriptions-by-topic --topic-arn $env:SNS_TOPIC_ARN --region ap-south-1
aws sqs get-queue-attributes --queue-url $env:EMAIL_QUEUE_URL --attribute-names ApproximateNumberOfMessages --region ap-south-1
```

If you run from Eclipse, set the same variables in **Run > Run Configurations > (your app) > Environment**.

## 9. AWS credentials
No credentials exist in this project. The AWS SDK **default credentials provider chain** finds them automatically. Use ONE of:

```powershell
aws configure
```
(enter your own Access Key, Secret Key, region `ap-south-1`) or the environment variables:
```powershell
$env:AWS_ACCESS_KEY_ID="..."
$env:AWS_SECRET_ACCESS_KEY="..."
```
or an IAM role if running on AWS.

The IAM user needs: `sns:Publish` on the topic, and `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes` on the three queues.

## 10. Eclipse setup
1. Install JDK 17 and set it in **Window > Preferences > Java > Installed JREs**.
2. Extract the ZIP.
3. **File > Import > Maven > Existing Maven Projects**, select the `event-notification-service` folder, Finish.
4. Wait for Maven to download dependencies (bottom-right progress bar).
5. If you see errors: right-click project > **Maven > Update Project...** (tick Force Update).
6. Make sure the project compiler level is Java 17 (Project > Properties > Java Compiler).

## 11. Maven setup
Install Maven 3.9+ and JDK 17 and check:
```powershell
java -version
mvn -version
```

## 12. How to run
From PowerShell (after setting the environment variables):
```powershell
cd event-notification-service
mvn clean package
mvn spring-boot:run
```
Or in Eclipse: right-click `EventNotificationApplication.java` > **Run As > Java Application** (with the environment variables set in the run configuration).

Startup success looks like: `Started EventNotificationApplication` plus three lines `... CONSUMER started polling its SQS queue`.

## 13. Postman testing

- Method: **POST**
- URL: `http://localhost:8080/orders`
- Header: `Content-Type: application/json`
- Body (raw > JSON):

```json
{
  "customer": "Divishka",
  "product": "Laptop",
  "amount": 55000
}
```

Expected: **HTTP 200**

```json
{
  "orderId": "ORD1001",
  "message": "Order created and event published successfully",
  "status": "ORDER_CREATED"
}
```

Then verify:
1. **PostgreSQL / pgAdmin**: `SELECT * FROM orders;` shows the new row.
2. **SNS** > `OrderEvents`: topic with its 3 subscriptions (Monitoring tab shows published messages).
3. **OrderEmailQueue**, **OrderAnalyticsQueue**, **OrderNotificationQueue**: each received the event. Because the app consumes and deletes messages quickly, they may show 0 available. Check the **Monitoring** tab (messages received/deleted) or temporarily stop the app and send an order to see 1 message sitting in each queue.
4. **Spring Boot console**: all three consumer blocks are printed.

Validation test (expects HTTP 400):
```json
{ "customer": "", "product": "Laptop", "amount": 0 }
```

## 14. SNS fan-out explanation
`SnsPublisherService` calls `SnsClient.publish(...)` ONCE with the topic ARN and the JSON event. SNS delivers a copy to every confirmed subscription - here, the three SQS queues. The API does not know (or care) who consumes the event. Adding a fourth consumer later only needs a new subscription in AWS - no change to the Order API.

## 15. SQS consumer explanation
`EmailQueueConsumer`, `AnalyticsQueueConsumer` and `NotificationQueueConsumer` extend `AbstractQueueConsumer`, which:
- runs one polling thread per consumer (so they are independent),
- uses SQS long polling (`receiveMessage`, wait 10 s, up to 5 messages),
- parses the JSON (handles both the SNS envelope and "raw message delivery"),
- calls the consumer's `process(...)`,
- **deletes the message only after success**; if processing throws, the message is NOT deleted and SQS will deliver it again after the visibility timeout.

These are demo consumers: they print to the console only (no real email/SMS).

## 16. Expected console output

```
================================
EMAIL CONSUMER
================================
Order received: ORD1001
Customer: Divishka
Product: Laptop
Email notification processed
================================
================================
ANALYTICS CONSUMER
================================
Order received: ORD1001
Amount: 55000.0
Analytics event processed
================================
================================
NOTIFICATION CONSUMER
================================
Order received: ORD1001
Notification processed
================================
```
(The three blocks can appear in any order because the consumers run independently.)

## 17. Error handling
| Situation | Result |
|-----------|--------|
| Invalid / missing fields | HTTP 400 with field errors |
| PostgreSQL problem | HTTP 503 |
| SNS publish fails | error logged, order status set to `EVENT_PUBLISH_FAILED`, HTTP 502 |

## 18. Troubleshooting

| Problem | Fix |
|---------|-----|
| `Could not resolve placeholder 'DB_PASSWORD'` (or SNS_TOPIC_ARN / queue URLs) | Environment variable not set in the same terminal / Eclipse run configuration |
| `password authentication failed for user "postgres"` | Wrong `DB_PASSWORD` |
| `database "event_notification_db" does not exist` | Run `CREATE DATABASE event_notification_db;` in pgAdmin |
| `Unable to load credentials` | Run `aws configure` or set AWS credential env variables |
| `AuthorizationError` / `AccessDenied` on publish or receive | IAM user lacks `sns:Publish` / `sqs:ReceiveMessage` / `sqs:DeleteMessage` |
| `NotFound` / `InvalidParameter` for topic | `SNS_TOPIC_ARN` is wrong or from another region |
| Queue not found / non-existent queue | Check queue URL and that region is `ap-south-1` |
| Order saved but no messages in queues | Check the SNS subscriptions are Confirmed and the queue access policies allow SNS (already working in your manual test) |
| Queue shows 0 messages | The consumers already processed and deleted them - check Monitoring or the console |
| Port 8080 in use | Change `server.port` in `application.properties` |
| Maven dependency errors in Eclipse | Maven > Update Project (Force Update), check internet |

## 19. HACKATHON DEMO FLOW
1. Open pgAdmin.
2. Show `event_notification_db`.
3. Start Spring Boot.
4. Open Postman.
5. Send `POST /orders`.
6. Show the successful response.
7. Show the order saved in PostgreSQL (`SELECT * FROM orders;`).
8. Explain that Spring Boot published ONE event to SNS.
9. Open SNS `OrderEvents`.
10. Show the three existing SQS subscriptions.
11. Show `OrderEmailQueue` received the event.
12. Show `OrderAnalyticsQueue` received the event.
13. Show `OrderNotificationQueue` received the event.
14. Show the three consumers processing independently in the console.
15. Explain SNS Fan-Out:

> "One event is published once to SNS, and SNS fans the event out to multiple SQS queues so different consumers can process the same event independently."

## 20. AWS cleanup guidance
This project created nothing in AWS, so there is nothing to delete for it. When your hackathon is over and you no longer need the manually created resources, you can delete them yourself from the AWS Console: unsubscribe/delete the three SQS queues and the `OrderEvents` topic, and deactivate/delete any IAM access keys you created for the demo. Stop the Spring Boot app so it stops polling SQS (long polling is cheap, but nothing needs to keep running).

## 21. Security notes
- No AWS keys, passwords or account IDs are in the source code, `application.properties` or this README.
- The DB password and AWS settings come from environment variables; AWS credentials come from the SDK default credentials chain.
- Never commit `.env` files or `~/.aws/credentials` (they are in `.gitignore`).
- Use an IAM user with only the permissions listed above; rotate/delete keys after the event.
- Error responses never include credentials or stack traces.

## Project structure
```
event-notification-service/
├── pom.xml
├── README.md
├── .gitignore
└── src/main
    ├── java/com/example/eventnotification
    │   ├── EventNotificationApplication.java
    │   ├── config/AwsConfig.java
    │   ├── controller/  OrderController.java, GlobalExceptionHandler.java
    │   ├── service/     OrderService, SnsPublisherService, AbstractQueueConsumer,
    │   │                EmailQueueConsumer, AnalyticsQueueConsumer, NotificationQueueConsumer,
    │   │                EventPublishException
    │   ├── model/       Order, OrderRequest, OrderEvent
    │   └── repository/  OrderRepository.java
    └── resources/application.properties
```
