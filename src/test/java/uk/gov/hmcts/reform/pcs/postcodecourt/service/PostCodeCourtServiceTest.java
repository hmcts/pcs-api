package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtKey;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
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
    @DisplayName("Return CountyCourts for an existing PostCode")
    void shouldReturnCountyCourtsForExistingPostCode() {
        String postCode = "W3 7RX";
        int expectedEpimId = 20262;
        PostCodeCourtEntity postCodeCourtEntity = new PostCodeCourtEntity();
        postCodeCourtEntity.setId(new PostCodeCourtKey(postCode, expectedEpimId));
        when(postCodeCourtRepository.findByIdPostCode(postCode)).thenReturn(List.of(postCodeCourtEntity));
        when(locationReferenceService.getCountyCourts(null,  List.of(expectedEpimId)))
            .thenReturn(List.of(new CourtVenue(expectedEpimId, 101, "Royal Courts of Justice (Main Building)")));

        final List<Court> response = underTest.getCountyCourtsByPostCode(postCode, null);

        assertThat(response).isNotEmpty();
        assertThat(response)
                .singleElement()
                .satisfies(court -> {
                    assertThat(court.epimId()).isEqualTo(expectedEpimId);
                    assertThat(court.id()).isEqualTo(101);
                    assertThat(court.name()).isEqualTo("Royal Courts of Justice (Main Building)");
                });

        verify(postCodeCourtRepository).findByIdPostCode(postCode);
        verify(locationReferenceService).getCountyCourts(null, List.of(expectedEpimId));
    }

    @Test
    @DisplayName("Should return an empty list of CountyCourts for a non-existent postcode")
    void shouldReturnEmptyOfCountyCourtsForNonExistentPostCode() {
        String nonExistentPostCode = "XY1 2AB";
        when(postCodeCourtRepository.findByIdPostCode(nonExistentPostCode)).thenReturn(List.of());

        final List<Court> response = underTest.getCountyCourtsByPostCode(nonExistentPostCode, null);

        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findByIdPostCode(nonExistentPostCode);
    }

}
