package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourt;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

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
    void shouldReturnForExistingPostCode() {
        // Given
        String postCode = "W3 7RX";
        int expectedEpimId = 20262;
        PostCodeCourt postalCode = new PostCodeCourt();
        postalCode.setEpimId(expectedEpimId);
        when(postCodeCourtRepository.findByPostCode(postCode)).thenReturn(Optional.of(postalCode));

        // When
        final Optional<String> response = underTest.getEpimIdByPostCode(postCode);

        // Then
        assertThat(response).isPresent().contains(String.valueOf(expectedEpimId));
        verify(postCodeCourtRepository).findByPostCode(postCode);
    }

    @Test
    @DisplayName("Should return an empty Optional for a non-existent postcode")
    void shouldReturnEmptyOptionalForNonExistentPostCode() {
        // Given
        String nonExistentPostCode = "XY1 2AB";
        when(postCodeCourtRepository.findByPostCode(nonExistentPostCode)).thenReturn(Optional.empty());

        // When
        final Optional<String> response = underTest.getEpimIdByPostCode(nonExistentPostCode);

        // Then
        assertThat(response).isEmpty();
        verify(postCodeCourtRepository).findByPostCode(nonExistentPostCode);
    }

}
