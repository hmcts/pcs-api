package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import org.apache.qpid.jms.util.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourt;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourtKey;
import uk.gov.hmcts.reform.pcs.postcodecourt.record.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCodeCourtServiceTest {

    @Mock
    private PostCodeCourtRepository postCodeCourtRepository;

    @InjectMocks
    private PostCodeCourtService underTest;

    @Test
    @DisplayName("Return valid epimId for an existing PostCode")
    void shouldReturnForExistingPostCode() throws ResourceNotFoundException {
        // Given
        String postCode = "W3 7RX";
        int expectedEpimId = 20262;
        PostCodeCourt postCodeCourt = new PostCodeCourt();
        postCodeCourt.setId(new PostCodeCourtKey(postCode, expectedEpimId));
        when(postCodeCourtRepository.findByIdPostCode(postCode)).thenReturn(List.of(postCodeCourt));

        // When
        final List<CourtVenue> response = underTest.getEpimIdByPostCode(postCode, null);

        // Then
//        assertThat(response).isPresent().contains(String.valueOf(expectedEpimId));
//        verify(postCodeCourtRepository).findByPostCode(postCode);
    }

    @Test
    @DisplayName("Should return an empty Optional for a non-existent postcode")
    void shouldReturnEmptyOptionalForNonExistentPostCode() throws ResourceNotFoundException {
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
