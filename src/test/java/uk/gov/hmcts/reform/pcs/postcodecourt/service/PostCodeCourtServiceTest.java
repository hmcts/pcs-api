package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourt;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourtKey;
import uk.gov.hmcts.reform.pcs.postcodecourt.record.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCodeCourtServiceTest {

    @Mock
    private PostCodeCourtRepository postCodeCourtRepository;

    @Mock
    private LocationReferenceService locationReferenceService;

    @InjectMocks
    private PostCodeCourtService underTest;

    @Test
    @DisplayName("Return valid epimId for an existing PostCode")
    void shouldReturnForExistingPostCode() {
        // Given
        String postCode = "W3 7RX";
        int expectedEpimId = 20262;
        PostCodeCourt postCodeCourt = new PostCodeCourt();
        postCodeCourt.setId(new PostCodeCourtKey(postCode, expectedEpimId));
        when(postCodeCourtRepository.findByIdPostCode(postCode)).thenReturn(List.of(postCodeCourt));
        when(locationReferenceService.getCountyCourts(null, List.of(expectedEpimId)))
                .thenReturn(List.of(new CourtVenue(expectedEpimId, 101, "Royal Courts of Justice (Main Building)")));

        // When
        final List<CourtVenue> response = underTest.getEpimIdByPostCode(postCode, null);

        // Then
        assertThat(response).isNotEmpty(); // Check if the list is not empty
        assertThat(response).anyMatch(courtVenue -> expectedEpimId == courtVenue.epimmsId()); // Check if any CourtVenue in the list has the expected EpimId

        verify(postCodeCourtRepository).findByIdPostCode(postCode);
        verify(locationReferenceService).getCountyCourts(null, List.of(expectedEpimId));
    }

    @Test
    @DisplayName("Should return an empty Optional for a non-existent postcode")
    void shouldReturnEmptyOptionalForNonExistentPostCode() {
        // Given
        String nonExistentPostCode = "XY1 2AB";
        when(postCodeCourtRepository.findByIdPostCode(nonExistentPostCode)).thenReturn(List.of());

        // When
        final List<CourtVenue> response = underTest.getEpimIdByPostCode(nonExistentPostCode, null);

        // Then
        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findByIdPostCode(nonExistentPostCode);
    }

}
