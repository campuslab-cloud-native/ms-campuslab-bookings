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

    public void release(Long resourceId) {
        sendAvailabilityRequest(resourceId, "release");
    }

    private void sendAvailabilityRequest(Long resourceId, String action) {
        try {
            restClient.post()
                    .uri(catalogServiceUrl + "/api/catalog/resources/"
                            + resourceId + "/availability/" + action)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict exception) {
            throw new BookingConflictException(
                    "No hay cupos disponibles para el recurso " + resourceId
            );
        } catch (HttpClientErrorException.NotFound exception) {
            throw new BookingResourceNotFoundException(resourceId);
        }
    }
}
