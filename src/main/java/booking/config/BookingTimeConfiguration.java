package booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BookingTimeConfiguration {

    public static final ZoneId ZONE_ID = ZoneId.of("America/Santiago");

    @Bean
    public Clock bookingClock() {
        return Clock.system(ZONE_ID);
    }
}
