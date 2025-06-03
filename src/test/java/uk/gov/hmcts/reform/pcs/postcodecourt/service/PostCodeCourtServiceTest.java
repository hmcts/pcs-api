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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCodeCourtServiceTest {

    private static final String SYSTEM_USER_TOKEN = "some system user token";

    @Mock
    private PostCodeCourtRepository postCodeCourtRepository;

    @Mock
    private LocationReferenceService locationReferenceService;

    @Mock
    private IdamService idamService;

    private PostCodeCourtService underTest;

    @BeforeEach
    void setUp() {
        when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
        underTest = new PostCodeCourtService(postCodeCourtRepository, locationReferenceService, idamService);
    }

    @Test
    @DisplayName("Return CountyCourts for an existing PostCode with spaces")
    void shouldReturnCountyCourtsForExistingPostCodeWithSpaces() {
        String postCode = " W3 7RX ";
        String trimmedPostcode = "W37RX";
        int expectedEpimId = 20262;
        List<String> postcodes = getPostCodeCandidates(trimmedPostcode);

        PostCodeCourtEntity postCodeCourtEntity = new PostCodeCourtEntity();
        postCodeCourtEntity.setId(new PostCodeCourtKey(postCode, expectedEpimId));
        when(postCodeCourtRepository.findByIdPostCodeIn(postcodes)).thenReturn(List.of(postCodeCourtEntity));
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

        verify(postCodeCourtRepository).findByIdPostCodeIn(postcodes);
        verify(locationReferenceService).getCountyCourts(SYSTEM_USER_TOKEN, List.of(expectedEpimId));
    }

    @Test
    @DisplayName("Should return an empty list of CountyCourts for a non-existent postcode")
    void shouldReturnEmptyListOfCountyCourtsForNonExistentPostCode() {
        String nonExistentPostCode = "XY1 2AB";
        String trimmedPostcode = "XY12AB";
        List<String> postcodes = getPostCodeCandidates(trimmedPostcode);
        when(postCodeCourtRepository.findByIdPostCodeIn(postcodes)).thenReturn(List.of());

        final List<Court> response = underTest.getCountyCourtsByPostCode(nonExistentPostCode);

        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findByIdPostCodeIn(postcodes);
    }

    @Test
    @DisplayName("Should return a partial match if available when no full match is found")
    void shouldReturnPartiallyMatchedPostCode() {
        String postCode = "W37RX";
        String partialPostcode = "W37R";
        int expectedEpimId = 76598;
        PostCodeCourtEntity postCodeCourtEntity = new PostCodeCourtEntity();
        postCodeCourtEntity.setId(new PostCodeCourtKey(partialPostcode, expectedEpimId));
        List<String> postcodes = getPostCodeCandidates(postCode);

        when(postCodeCourtRepository.findByIdPostCodeIn(postcodes)).thenReturn(List.of(postCodeCourtEntity));
        when(locationReferenceService.getCountyCourts(SYSTEM_USER_TOKEN, List.of(expectedEpimId)))
            .thenReturn(List.of(new CourtVenue(expectedEpimId, 303, "Main Court of Justice")));

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode);

        assertThat(response)
            .isNotEmpty()
            .isEqualTo(List.of(new Court(303, "Main Court of Justice", expectedEpimId)));
        verify(postCodeCourtRepository).findByIdPostCodeIn(postcodes);
        verify(locationReferenceService).getCountyCourts(SYSTEM_USER_TOKEN, List.of(expectedEpimId));
    }

    @Test
    @DisplayName("Should throw InvalidPostCode exception when postcode empty")
    void shouldThrowInvalidPostCodeExceptionWhenPostcodeIsEmpty() {
        String emptyPostCode = "";
        assertThatThrownBy(() -> underTest.getCountyCourtsByPostCode(emptyPostCode)).isInstanceOf(
                InvalidPostCodeException.class)
            .hasMessage("Postcode can't be empty or null");
        verify(postCodeCourtRepository, never()).findByIdPostCodeIn(any());
    }

    @Test
    @DisplayName("Should throw InvalidPostCode exception when postcode null")
    void shouldThrowInvalidPostCodeExceptionWhenPostcodeIsNull() {
        assertThatThrownBy(() -> underTest.getCountyCourtsByPostCode(null)).isInstanceOf(
                InvalidPostCodeException.class)
            .hasMessage("Postcode can't be empty or null");
        verify(postCodeCourtRepository, never()).findByIdPostCodeIn(any());
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

}
