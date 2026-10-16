package booking.data.exchange.catalog;

import booking.common.exception.BookingConflictException;
import booking.common.exception.BookingResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class CatalogAvailabilityClient {

    private final RestClient restClient = RestClient.create();

    @Value("${services.catalog.url}")
    private String catalogServiceUrl;

    public void reserve(Long resourceId) {
        sendAvailabilityRequest(resourceId, "reserve");
    }

    public void release(Long resourceId, String operationId) {
        sendAvailabilityRequest(resourceId, "release", operationId);
    }

    private void sendAvailabilityRequest(Long resourceId, String action) {
        sendAvailabilityRequest(resourceId, action, null);
    }

    private void sendAvailabilityRequest(
            Long resourceId,
            String action,
            String operationId
    ) {
        try {
            var request = restClient.post()
                    .uri(catalogServiceUrl + "/api/catalog/resources/"
                            + resourceId + "/availability/" + action);
            if (operationId != null) {
                request.header("Idempotency-Key", operationId);
            }
            request.retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.Conflict exception) {
            if ("reserve".equals(action)) {
                throw new BookingConflictException(
                        "No hay cupos disponibles para el recurso " + resourceId
                );
            }
            throw exception;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new BookingResourceNotFoundException(resourceId);
        }
    }
}
