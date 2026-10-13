package booking.messaging.publisher;

import booking.config.MessagingNames;
import booking.messaging.model.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishEmail(NotificationMessage message) {
        publish(MessagingNames.EMAIL_ROUTING_KEY, message);
    }

    public void publishPreparation(NotificationMessage message) {
        publish(MessagingNames.PREP_ROUTING_KEY, message);
    }

    public void publishVoucher(NotificationMessage message) {
        publish(MessagingNames.VOUCHER_ROUTING_KEY, message);
    }

    private void publish(String routingKey, NotificationMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    MessagingNames.NOTIFY_EXCHANGE,
                    routingKey,
                    message
            );

            log.info(
                    "Notification command published. eventId={}, type={}, routingKey={}",
                    message.getEventId(),
                    message.getType(),
                    routingKey
            );
        } catch (AmqpException exception) {
            log.error(
                    "Failed to publish notification command. eventId={}, routingKey={}",
                    message.getEventId(),
                    routingKey,
                    exception
            );
            throw exception;
        }
    }
}