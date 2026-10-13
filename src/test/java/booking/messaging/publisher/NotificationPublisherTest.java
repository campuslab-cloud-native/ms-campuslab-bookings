package booking.messaging.publisher;

import booking.config.MessagingNames;
import booking.messaging.model.NotificationMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final NotificationPublisher publisher = new NotificationPublisher(rabbitTemplate);
    private final NotificationMessage message = NotificationMessage.builder()
            .eventId("event-1")
            .type("BOOKING_CREATED")
            .build();

    @Test
    void publishEmailUsesEmailRoutingKey() {
        verifyPublished(MessagingNames.EMAIL_ROUTING_KEY, publisher::publishEmail);
    }

    @Test
    void publishPreparationUsesPreparationRoutingKey() {
        verifyPublished(MessagingNames.PREP_ROUTING_KEY, publisher::publishPreparation);
    }

    @Test
    void publishVoucherUsesVoucherRoutingKey() {
        verifyPublished(MessagingNames.VOUCHER_ROUTING_KEY, publisher::publishVoucher);
    }

    @Test
    void publishRethrowsAmqpFailures() {
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(
                        MessagingNames.NOTIFY_EXCHANGE,
                        MessagingNames.EMAIL_ROUTING_KEY,
                        message
                );

        assertThrows(AmqpException.class, () -> publisher.publishEmail(message));
    }

    private void verifyPublished(
            String routingKey,
            java.util.function.Consumer<NotificationMessage> publish
    ) {
        publish.accept(message);

        verify(rabbitTemplate).convertAndSend(
                MessagingNames.NOTIFY_EXCHANGE,
                routingKey,
                message
        );
    }
}
