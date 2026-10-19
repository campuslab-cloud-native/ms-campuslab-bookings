package booking;

import booking.domain.model.BookingStatus;
import booking.data.model.BookingEntity;
import booking.data.repository.BookingJpaRepository;
import booking.presentation.response.BookingResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:bookings-test",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class BookingApplicationTests {

	@Autowired
	private JsonMapper objectMapper;

	@Autowired
	private BookingJpaRepository bookingJpaRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void bookingResponseRoundTripsSantiagoLocalTimesWithoutOffsetConversion()
			throws Exception {
		BookingResponse response = BookingResponse.builder()
				.id(12L)
				.resourceId(7L)
				.startTime(LocalDateTime.parse("2026-10-18T15:00:00"))
				.endTime(LocalDateTime.parse("2026-10-18T16:00:00"))
				.status(BookingStatus.REQUESTED)
				.createdAt(LocalDateTime.parse("2026-10-17T12:00:00"))
				.build();

		String json = objectMapper.writeValueAsString(response);
		BookingResponse deserialized = objectMapper.readValue(
				json,
				BookingResponse.class
		);

		Assertions.assertTrue(json.contains("\"startTime\":\"2026-10-18T15:00:00\""));
		Assertions.assertTrue(json.contains("\"endTime\":\"2026-10-18T16:00:00\""));
		Assertions.assertEquals(response.getStartTime(), deserialized.getStartTime());
		Assertions.assertEquals(response.getEndTime(), deserialized.getEndTime());
	}

	@Test
	void bookingTimestampPersistencePreservesLocalTime() {
		BookingEntity entity = BookingEntity.builder()
				.resourceId(7L)
				.startTime(LocalDateTime.parse("2026-10-18T15:00:00"))
				.endTime(LocalDateTime.parse("2026-10-18T16:00:00"))
				.createdAt(LocalDateTime.parse("2026-10-17T12:00:00"))
				.status(BookingStatus.REQUESTED)
				.build();

		BookingEntity saved = bookingJpaRepository.saveAndFlush(entity);
		BookingEntity loaded = bookingJpaRepository.findById(saved.getId())
				.orElseThrow();

		Assertions.assertEquals(
				LocalDateTime.parse("2026-10-18T15:00:00"),
				loaded.getStartTime()
		);
		Assertions.assertEquals(
				LocalDateTime.parse("2026-10-18T16:00:00"),
				loaded.getEndTime()
		);
	}

}
