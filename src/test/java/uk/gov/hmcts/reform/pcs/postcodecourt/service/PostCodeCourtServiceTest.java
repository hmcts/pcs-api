package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtKey;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class PostCodeCourtServiceTest {

    private static final String POSTCODE = "AB12 3CD";
    private static final LocalDate TEST_DATE = LocalDate.of(2026, JUNE, 3);
    private static final List<String> PARTIAL_POSTCODES = List.of("AB123CD", "AB123C", "AB123", "AB12");

    @Mock
    private PostCodeCourtRepository postCodeCourtRepository;
    @Mock
    private PartialPostcodesGenerator partialPostcodesGenerator;
    @Mock
    private Clock ukClock;

    private PostCodeCourtService underTest;

    @BeforeEach
    void setUp() {
        when(ukClock.instant()).thenReturn(TEST_DATE.atStartOfDay(UK_ZONE_ID).toInstant());
        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);
        underTest = new PostCodeCourtService(postCodeCourtRepository, partialPostcodesGenerator, ukClock);
    }

    @Test
    void shouldReturnEpimsIdWhenOneActiveMappingIsFound() {
        // Given
        int expectedEpimsId = 123456;
        when(partialPostcodesGenerator.generateForPostcode(POSTCODE)).thenReturn(PARTIAL_POSTCODES);
        when(postCodeCourtRepository.findActiveByPostCodeIn(PARTIAL_POSTCODES, TEST_DATE))
            .thenReturn(List.of(postCodeCourtEntity("AB123CD", expectedEpimsId)));

        // When
        Integer actualEpimsId = underTest.getCourtManagementLocation(POSTCODE);

        // Then
        assertThat(actualEpimsId).isEqualTo(expectedEpimsId);
        verify(partialPostcodesGenerator).generateForPostcode(POSTCODE);
        verify(postCodeCourtRepository).findActiveByPostCodeIn(PARTIAL_POSTCODES, TEST_DATE);
    }

    @Test
    void shouldReturnNullWhenNoActiveMappingIsFound() {
        // Given
        when(partialPostcodesGenerator.generateForPostcode(POSTCODE)).thenReturn(PARTIAL_POSTCODES);
        when(postCodeCourtRepository.findActiveByPostCodeIn(PARTIAL_POSTCODES, TEST_DATE)).thenReturn(List.of());

        // When
        Integer actualEpimsId = underTest.getCourtManagementLocation(POSTCODE);

        // Then
        assertThat(actualEpimsId).isNull();
    }

    @ParameterizedTest
    @MethodSource("activeMappingScenarios")
    void shouldResolveCourtManagementLocationFromActiveMappings(List<PostCodeCourtEntity> activeMappings,
                                                                Integer expectedEpimsId) {
        // Given
        when(partialPostcodesGenerator.generateForPostcode(POSTCODE)).thenReturn(PARTIAL_POSTCODES);
        when(postCodeCourtRepository.findActiveByPostCodeIn(PARTIAL_POSTCODES, TEST_DATE)).thenReturn(activeMappings);

        // When
        Integer actualEpimsId = underTest.getCourtManagementLocation(POSTCODE);

        // Then
        assertThat(actualEpimsId).isEqualTo(expectedEpimsId);
    }

    @Test
    void shouldUseLegislativeCountryWhenResolvingCourtManagementLocation() {
        // Given
        String crossBorderPostcode = "SY13 2LH";
        int expectedWelshEpimsId = 28837;
        List<String> partialPostcodes = List.of("SY132LH", "SY132L", "SY132", "SY13");
        when(partialPostcodesGenerator.generateForPostcode(crossBorderPostcode)).thenReturn(partialPostcodes);
        when(postCodeCourtRepository.findActiveByPostCodeIn(
            partialPostcodes,
            LegislativeCountry.WALES,
            TEST_DATE
        )).thenReturn(List.of(postCodeCourtEntity("SY132LH", expectedWelshEpimsId)));

        // When
        Integer actualEpimsId = underTest.getCourtManagementLocation(crossBorderPostcode, LegislativeCountry.WALES);

        // Then
        assertThat(actualEpimsId).isEqualTo(expectedWelshEpimsId);
        verify(postCodeCourtRepository).findActiveByPostCodeIn(
            partialPostcodes,
            LegislativeCountry.WALES,
            TEST_DATE
        );
    }

    private PostCodeCourtEntity postCodeCourtEntity(String postcode, int epimsId) {
        PostCodeCourtEntity entity = new PostCodeCourtEntity();
        entity.setId(new PostCodeCourtKey(postcode, epimsId));
        return entity;
    }

    private static Stream<Arguments> activeMappingScenarios() {
        return Stream.of(
            arguments(
                List.of(
                    postCodeCourtEntityForScenario("AB12", 111111),
                    postCodeCourtEntityForScenario("AB123", 222222),
                    postCodeCourtEntityForScenario("AB123CD", 123456)
                ),
                123456
            ),
            arguments(
                List.of(
                    postCodeCourtEntityForScenario("AB12", 111111),
                    postCodeCourtEntityForScenario("AB123CD", 222222),
                    postCodeCourtEntityForScenario("AB123CD", 333333)
                ),
                null
            ),
            arguments(
                List.of(
                    postCodeCourtEntityForScenario("AB123", 111111),
                    postCodeCourtEntityForScenario("AB123", 222222),
                    postCodeCourtEntityForScenario("AB123CD", 333333)
                ),
                333333
            )
        );
    }

    private static PostCodeCourtEntity postCodeCourtEntityForScenario(String postcode, int epimsId) {
        PostCodeCourtEntity entity = new PostCodeCourtEntity();
        entity.setId(new PostCodeCourtKey(postcode, epimsId));
        return entity;
    }
}
