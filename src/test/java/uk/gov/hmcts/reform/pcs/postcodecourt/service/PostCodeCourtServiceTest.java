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
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.PostCodeNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    @DisplayName("Return CountyCourts for an existing PostCode with spaces")
    void shouldReturnCountyCourtsForExistingPostCodeWithSpaces() {
        String postCode = " W3 7RX ";
        String trimmedPostcode = "W37RX";
        int expectedEpimId = 20262;

        PostCodeCourtEntity postCodeCourtEntity = new PostCodeCourtEntity();
        postCodeCourtEntity.setId(new PostCodeCourtKey(postCode, expectedEpimId));
        when(postCodeCourtRepository.findByIdPostCode(trimmedPostcode)).thenReturn(List.of(postCodeCourtEntity));
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

        verify(postCodeCourtRepository).findByIdPostCode(trimmedPostcode);
        verify(locationReferenceService).getCountyCourts(null, List.of(expectedEpimId));
    }

    @Test
    @DisplayName("Should return an empty list of CountyCourts for a null postcode")
    void shouldReturnEmptyListOfCountyCourtsForNullPostCode() {
        String nullPostcode = null;

        final List<Court> response = underTest.getCountyCourtsByPostCode(nullPostcode, null);

        assertThat(response).isEmpty();
        verify(postCodeCourtRepository, never()).findByIdPostCode(any());
    }

    @Test
    @DisplayName("Should return a partial match when its available")
    void shouldReturnPartiallyMatchedCountyCourts() {
        String postCode = "W37RX";
        int expectedEpimId = 76598;
        PostCodeCourtEntity postCodeCourtEntity = new PostCodeCourtEntity();
        postCodeCourtEntity.setId(new PostCodeCourtKey(postCode, expectedEpimId));

        when(postCodeCourtRepository.findByIdPostCode(postCode)).thenReturn(List.of());
        when(postCodeCourtRepository.findByIdPostCode("W37R")).thenReturn(List.of());
        when(postCodeCourtRepository.findByIdPostCode("W37")).thenReturn(List.of());
        when(postCodeCourtRepository.findByIdPostCode("W3")).thenReturn(List.of(postCodeCourtEntity));
        when(locationReferenceService.getCountyCourts(null, List.of(expectedEpimId)))
            .thenReturn(List.of(new CourtVenue(expectedEpimId, 303, "Main Court of Justice")));

        List<Court> response = underTest.getCountyCourtsByPostCode(postCode, null);
        assertThat(response)
            .isNotEmpty()
            .isEqualTo(List.of(new Court(303, "Main Court of Justice", expectedEpimId)));
        verify(postCodeCourtRepository).findByIdPostCode("W3");
        verify(locationReferenceService).getCountyCourts(null, List.of(expectedEpimId));
        verify(postCodeCourtRepository, times(4)).findByIdPostCode(anyString());
    }

    @Test
    @DisplayName("Should throw a PostCodeNotFound exception when no partial or full match is found")
    void shouldThrowPostCodeNotFoundExceptionWhenMatchIsNotFound() {
        String postCode = "W37RX";
        when(postCodeCourtRepository.findByIdPostCode(anyString())).thenReturn(List.of());
        assertThatThrownBy(() -> underTest.getCountyCourtsByPostCode(postCode, null)).isInstanceOf(
                PostCodeNotFoundException.class)
            .hasMessage("No court mapping found for postcode " + postCode);
        verify(postCodeCourtRepository).findByIdPostCode(postCode);
        verify(postCodeCourtRepository, times(4)).findByIdPostCode(anyString());
    }
}
