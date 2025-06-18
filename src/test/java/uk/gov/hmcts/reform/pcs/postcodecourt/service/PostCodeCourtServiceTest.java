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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCodeCourtServiceTest {

    private static final String SYSTEM_USER_TOKEN = "some system user token";
    private static final LocalDate currentDate = LocalDate.now(ZoneId.of("Europe/London"));

    @Mock
    private PostCodeCourtRepository postCodeCourtRepository;

    @Mock
    private LocationReferenceService locationReferenceService;

    @Mock
    private IdamService idamService;

    private PostCodeCourtService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
        underTest = new PostCodeCourtService(postCodeCourtRepository, locationReferenceService, idamService);
    }

    @Test
    @DisplayName("Return CountyCourts for an existing PostCode with spaces")
    void shouldReturnCountyCourtsForExistingPostCodeWithSpaces() {
        String postCode = " W3 7RX ";
        String trimmedPostcode = "W37RX";
        int expectedEpimId = 20262;
        PostCodeCourtEntity postCodeCourtEntity = createPostCodeCourtEntity(postCode, expectedEpimId);
        List<String> postcodes = getPostCodeCandidates(trimmedPostcode);

        when(postCodeCourtRepository.findByIdPostCodeIn(
            postcodes,
            currentDate
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

        verify(postCodeCourtRepository).findByIdPostCodeIn(postcodes, currentDate);
        verify(locationReferenceService).getCountyCourts(SYSTEM_USER_TOKEN, List.of(expectedEpimId));
    }

    @Test
    @DisplayName("Should return an empty list of CountyCourts for a non-existent postcode")
    void shouldReturnEmptyListOfCountyCourtsForNonExistentPostCode() {
        String nonExistentPostCode = "XY1 2AB";
        String trimmedPostcode = "XY12AB";
        List<String> postcodes = getPostCodeCandidates(trimmedPostcode);
        when(postCodeCourtRepository.findByIdPostCodeIn(postcodes, currentDate)).thenReturn(List.of());

        final List<Court> response = underTest.getCountyCourtsByPostCode(nonExistentPostCode);

        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findByIdPostCodeIn(postcodes, currentDate);
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

        List<String> postcodes = getPostCodeCandidates(postCode);

        when(postCodeCourtRepository.findByIdPostCodeIn(
            postcodes,
            currentDate
        )).thenReturn(List.of(partialEntity, partialEntity2));
        when(locationReferenceService.getCountyCourts(SYSTEM_USER_TOKEN, List.of(epimId)))
            .thenReturn(List.of(new CourtVenue(epimId, 303, "Main Court of Justice")));

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode);

        assertThat(response)
            .isNotEmpty()
            .isEqualTo(List.of(new Court(303, "Main Court of Justice", epimId)));
        verify(postCodeCourtRepository).findByIdPostCodeIn(postcodes, currentDate);
        verify(locationReferenceService).getCountyCourts(SYSTEM_USER_TOKEN, List.of(epimId));
    }

    @Test
    @DisplayName("Should throw InvalidPostCode exception when postcode empty")
    void shouldThrowInvalidPostCodeExceptionWhenPostcodeIsEmpty() {
        String emptyPostCode = "";
        assertThatThrownBy(() -> underTest.getCountyCourtsByPostCode(emptyPostCode)).isInstanceOf(
                InvalidPostCodeException.class)
            .hasMessage("Postcode can't be empty or null");
        verify(postCodeCourtRepository, never()).findByIdPostCodeIn(any(), any());
    }

    @Test
    @DisplayName("Should throw InvalidPostCode exception when postcode null")
    void shouldThrowInvalidPostCodeExceptionWhenPostcodeIsNull() {
        assertThatThrownBy(() -> underTest.getCountyCourtsByPostCode(null)).isInstanceOf(
                InvalidPostCodeException.class)
            .hasMessage("Postcode can't be empty or null");
        verify(postCodeCourtRepository, never()).findByIdPostCodeIn(any(), any());
    }

    @Test
    @DisplayName("Should return postcode court mapping when EpimId is active")
    void shouldReturnPostcodeCourtMappingWhenEpimIdIsActive() {
        String postCode = "W37RX";
        int activeEpimId = 76598;
        PostCodeCourtEntity postCodeCourtEntity = createPostCodeCourtEntity(postCode, activeEpimId);
        List<String> postcodes = getPostCodeCandidates(postCode);

        when(postCodeCourtRepository.findByIdPostCodeIn(
            postcodes,
            currentDate
        )).thenReturn(List.of(postCodeCourtEntity));
        when(locationReferenceService.getCountyCourts(SYSTEM_USER_TOKEN, List.of(activeEpimId)))
            .thenReturn(List.of(new CourtVenue(activeEpimId, 458, "Central County Court")));

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode);

        assertThat(response)
            .isNotEmpty()
            .isEqualTo(List.of(new Court(458, "Central County Court", activeEpimId)));
        verify(postCodeCourtRepository).findByIdPostCodeIn(postcodes, currentDate);
        verify(locationReferenceService).getCountyCourts(SYSTEM_USER_TOKEN, List.of(activeEpimId));
    }

    @Test
    @DisplayName("Should return empty list when EpimId is not active")
    void shouldReturnEmptyListWhenEpimIdIsNotActive() {
        String postCode = "W37RX";
        List<String> postcodes = getPostCodeCandidates(postCode);

        when(postCodeCourtRepository.findByIdPostCodeIn(
            postcodes,
            currentDate
        )).thenReturn(List.of());

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode);

        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findByIdPostCodeIn(postcodes, currentDate);
    }

    @Test
    @DisplayName("Should return empty list when multiple active EpimId's are found")
    void shouldReturnEmptyListWhenMultipleActiveEpimIdsFound() {
        String postCode = "W37RX";
        int firstActiveEpimId = 76598;
        int secondActiveEpimId = 76536;

        PostCodeCourtEntity firstActiveEntity = createPostCodeCourtEntity(postCode, firstActiveEpimId);
        PostCodeCourtEntity secondActiveEntity = createPostCodeCourtEntity(postCode, secondActiveEpimId);

        List<String> postcodes = getPostCodeCandidates(postCode);
        when(postCodeCourtRepository.findByIdPostCodeIn(postcodes, currentDate)).thenReturn(List.of(
            firstActiveEntity,
            secondActiveEntity
        ));

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode);
        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findByIdPostCodeIn(postcodes, currentDate);
    }

    private List<String> getPostCodeCandidates(String postCode) {
        String partialPostcode = postCode;
        List<String> postCodes = new ArrayList<>();
        postCodes.add(postCode);
        for (int x = 0; x < 3 && partialPostcode.length() > 2; x++) {
            partialPostcode = partialPostcode.substring(0, partialPostcode.length() - 1);
            postCodes.add(partialPostcode);
        }
        return postCodes;
    }

    private PostCodeCourtEntity createPostCodeCourtEntity(String postCode, int epimId) {
        PostCodeCourtEntity entity = new PostCodeCourtEntity();
        entity.setId(new PostCodeCourtKey(postCode, epimId));

        return entity;
    }

}
