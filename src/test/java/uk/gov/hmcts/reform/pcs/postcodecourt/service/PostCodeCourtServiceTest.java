package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtKey;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.InvalidPostCodeException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class PostCodeCourtServiceTest {

    private static final String SYSTEM_USER_TOKEN = "some system user token";
    private static final LocalDate TEST_DATE = LocalDate.of(2025, JUNE, 15);

    @Mock
    private PostCodeCourtRepository postCodeCourtRepository;
    @Mock
    private PartialPostcodesGenerator partialPostcodesGenerator;
    @Mock
    private LocationReferenceService locationReferenceService;
    @Mock(strictness = LENIENT)
    private IdamService idamService;
    @Mock(strictness = LENIENT)
    private Clock ukClock;

    private PostCodeCourtService underTest;

    @BeforeEach
    void setUp() {
        when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
        when(ukClock.instant()).thenReturn(TEST_DATE.atTime(15, 21).atZone(UK_ZONE_ID).toInstant());
        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);

        underTest = new PostCodeCourtService(postCodeCourtRepository, partialPostcodesGenerator,
                                             locationReferenceService, idamService, ukClock);
    }

    @Test
    @DisplayName("Return CountyCourts for an existing PostCode with spaces")
    void shouldReturnCountyCourtsForExistingPostCodeWithSpaces() {
        String postCode = " W3 7RX ";
        int expectedEpimId = 20262;
        PostCodeCourtEntity postCodeCourtEntity = createPostCodeCourtEntity(postCode, expectedEpimId);
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(postCode);

        when(postCodeCourtRepository.findActiveByPostCodeIn(
            expectedPartialPostcodes,
            TEST_DATE
        )).thenReturn(List.of(postCodeCourtEntity));
        when(locationReferenceService.getCountyCourts(SYSTEM_USER_TOKEN, List.of(expectedEpimId)))
            .thenReturn(List.of(new CourtVenue(expectedEpimId, 101, "Royal Courts of Justice (Main Building)")));

        final List<Court> response = underTest.getCountyCourtsByPostCode(postCode);

        assertThat(response).isNotEmpty();
        assertThat(response)
                .singleElement()
                .satisfies(court -> {
                    assertThat(court.epimId()).isEqualTo(expectedEpimId);
                    assertThat(court.id()).isEqualTo(101);
                    assertThat(court.name()).isEqualTo("Royal Courts of Justice (Main Building)");
                });

        verify(postCodeCourtRepository).findActiveByPostCodeIn(expectedPartialPostcodes, TEST_DATE);
        verify(locationReferenceService).getCountyCourts(SYSTEM_USER_TOKEN, List.of(expectedEpimId));
    }

    @Test
    @DisplayName("Should return an empty list of CountyCourts for a non-existent postcode")
    void shouldReturnEmptyListOfCountyCourtsForNonExistentPostCode() {
        String nonExistentPostCode = "XY1 2AB";
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(nonExistentPostCode);
        when(postCodeCourtRepository.findActiveByPostCodeIn(expectedPartialPostcodes, TEST_DATE)).thenReturn(List.of());

        final List<Court> response = underTest.getCountyCourtsByPostCode(nonExistentPostCode);

        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findActiveByPostCodeIn(expectedPartialPostcodes, TEST_DATE);
    }

    @Test
    @DisplayName("Should return the longest active partial code when no full match is found")
    void shouldReturnTheLongestActivePartialCodeWhenNoFullMatchIsFound() {
        String postCode = "W37RX";
        String partialPostcode = "W37R";
        String secondPartialPostcode = "W37";
        int epimId = 76598;
        int secondEpimId = 89567;
        PostCodeCourtEntity partialEntity = createPostCodeCourtEntity(partialPostcode, epimId);
        PostCodeCourtEntity partialEntity2 = createPostCodeCourtEntity(secondPartialPostcode, secondEpimId);

        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(postCode);

        when(postCodeCourtRepository.findActiveByPostCodeIn(
            expectedPartialPostcodes,
            TEST_DATE
        )).thenReturn(List.of(partialEntity, partialEntity2));
        when(locationReferenceService.getCountyCourts(SYSTEM_USER_TOKEN, List.of(epimId)))
            .thenReturn(List.of(new CourtVenue(epimId, 303, "Main Court of Justice")));

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode);

        assertThat(response)
            .isNotEmpty()
            .isEqualTo(List.of(new Court(303, "Main Court of Justice", epimId)));
        verify(postCodeCourtRepository).findActiveByPostCodeIn(expectedPartialPostcodes, TEST_DATE);
        verify(locationReferenceService).getCountyCourts(SYSTEM_USER_TOKEN, List.of(epimId));
    }

    @Test
    @DisplayName("Should throw InvalidPostCode exception when postcode empty")
    void shouldThrowInvalidPostCodeExceptionWhenPostcodeIsEmpty() {
        String emptyPostCode = "";
        assertThatThrownBy(() -> underTest.getCountyCourtsByPostCode(emptyPostCode)).isInstanceOf(
                InvalidPostCodeException.class)
            .hasMessage("Postcode can't be empty or null");
        verify(postCodeCourtRepository, never()).findActiveByPostCodeIn(any(), any());
    }

    @Test
    @DisplayName("Should throw InvalidPostCode exception when postcode null")
    void shouldThrowInvalidPostCodeExceptionWhenPostcodeIsNull() {
        assertThatThrownBy(() -> underTest.getCountyCourtsByPostCode(null)).isInstanceOf(
                InvalidPostCodeException.class)
            .hasMessage("Postcode can't be empty or null");
        verify(postCodeCourtRepository, never()).findActiveByPostCodeIn(any(), any());
    }

    @Test
    @DisplayName("Should return postcode court mapping when EpimId is active")
    void shouldReturnPostcodeCourtMappingWhenEpimIdIsActive() {
        String postCode = "W37RX";
        int activeEpimId = 76598;
        PostCodeCourtEntity postCodeCourtEntity = createPostCodeCourtEntity(postCode, activeEpimId);
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(postCode);

        when(postCodeCourtRepository.findActiveByPostCodeIn(
            expectedPartialPostcodes,
            TEST_DATE
        )).thenReturn(List.of(postCodeCourtEntity));
        when(locationReferenceService.getCountyCourts(SYSTEM_USER_TOKEN, List.of(activeEpimId)))
            .thenReturn(List.of(new CourtVenue(activeEpimId, 458, "Central County Court")));

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode);

        assertThat(response)
            .isNotEmpty()
            .isEqualTo(List.of(new Court(458, "Central County Court", activeEpimId)));
        verify(postCodeCourtRepository).findActiveByPostCodeIn(expectedPartialPostcodes, TEST_DATE);
        verify(locationReferenceService).getCountyCourts(SYSTEM_USER_TOKEN, List.of(activeEpimId));
    }

    @Test
    @DisplayName("Should return empty list when EpimId is not active")
    void shouldReturnEmptyListWhenEpimIdIsNotActive() {
        String postCode = "W37RX";
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(postCode);

        when(postCodeCourtRepository.findActiveByPostCodeIn(
            anyList(),
            any(LocalDate.class)
        )).thenReturn(List.of());

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode);

        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findActiveByPostCodeIn(expectedPartialPostcodes, TEST_DATE);
    }

    @Test
    @DisplayName("Should return empty list when multiple active EpimId's are found")
    void shouldReturnEmptyListWhenMultipleActiveEpimIdsFound() {
        String postCode = "W37RX";
        int firstActiveEpimId = 76598;
        int secondActiveEpimId = 76536;

        PostCodeCourtEntity firstActiveEntity = createPostCodeCourtEntity(postCode, firstActiveEpimId);
        PostCodeCourtEntity secondActiveEntity = createPostCodeCourtEntity(postCode, secondActiveEpimId);

        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(postCode);
        when(postCodeCourtRepository.findActiveByPostCodeIn(expectedPartialPostcodes, TEST_DATE)).thenReturn(List.of(
            firstActiveEntity,
            secondActiveEntity
        ));

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode);
        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findActiveByPostCodeIn(expectedPartialPostcodes, TEST_DATE);
    }

    @Test
    @DisplayName("Should return active EpimId for case management location")
    void shoudReturnActiveEpimIdForCaseManagementLocation() {
        String postCode = "W37RX";
        int activeEpimId = 76598;
        PostCodeCourtEntity activeEntity = createPostCodeCourtEntity(postCode, activeEpimId);

        List<String> postcodes = stubPartialPostcodesGenerator(postCode);

        when(postCodeCourtRepository.findActiveByPostCodeIn(postcodes, TEST_DATE))
            .thenReturn(List.of(activeEntity));

        Integer response = underTest.getCourtManagementLocation(postCode);

        verify(postCodeCourtRepository).findActiveByPostCodeIn(postcodes, TEST_DATE);
        assertThat(response).isEqualTo(activeEpimId);
    }

    @Test
    @DisplayName("Should return null when active EpimId not found for case management location")
    void shouldReturnNullWhenEpimIdNotFoundForCaseManagementLocation() {
        String postCode = "W37RX";

        List<String> postcodes = stubPartialPostcodesGenerator(postCode);
        when(postCodeCourtRepository.findActiveByPostCodeIn(postcodes, TEST_DATE))
            .thenReturn(List.of());

        Integer response = underTest.getCourtManagementLocation(postCode);

        verify(postCodeCourtRepository).findActiveByPostCodeIn(postcodes, TEST_DATE);
        assertThat(response).isNull();
    }


    private List<String> stubPartialPostcodesGenerator(String postCode) {
        List<String> expectedPartialPostcodes = List.of("A", "B", "C");
        when(partialPostcodesGenerator.generateForPostcode(postCode)).thenReturn(expectedPartialPostcodes);
        return expectedPartialPostcodes;

    }

    private PostCodeCourtEntity createPostCodeCourtEntity(String postCode, int epimId) {
        PostCodeCourtEntity entity = new PostCodeCourtEntity();
        entity.setId(new PostCodeCourtKey(postCode, epimId));

        return entity;
    }

}
