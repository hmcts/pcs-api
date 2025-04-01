package uk.gov.hmcts.reform.pcs.postalcode.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.postalcode.domain.PostCode;
import uk.gov.hmcts.reform.pcs.postalcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postalcode.repository.PostalCodeRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostalCodeServiceTest {

    @Mock
    private PostalCodeRepository postalCodeRepository;

    @InjectMocks
    private PostalCodeService underTest;

    @Test
    @DisplayName("Return null for non-existent postcode")
    void shouldReturnValidEpimIdForExistingPostcode() {
        // Given
        String postcode = "W3 7RX";
        int expectedEpimId = 20262;
        PostCode postalCode = new PostCode();
        postalCode.setEpimId(expectedEpimId);
        when(postalCodeRepository.findByPostCode(postcode)).thenReturn(Optional.of(postalCode));

        // When
        PostCodeResponse response = underTest.getEpimIdByPostCode(postcode);

        // Then
        assertThat(response.getEpimId()).isEqualTo(expectedEpimId);
        verify(postalCodeRepository).findByPostCode(postcode);
    }

    @Test
    @DisplayName("Should return an empty PostCodeResponse for a non-existent postcode")
    void shouldReturnEmptyResponseForNonExistentPostcode() {
        // Given
        String nonExistentPostcode = "XY1 2AB";
        when(postalCodeRepository.findByPostCode(nonExistentPostcode)).thenReturn(Optional.empty());

        // When
        PostCodeResponse response = underTest.getEpimIdByPostCode(nonExistentPostcode);

        // Then
        assertThat(response.getEpimId()).isZero();
        verify(postalCodeRepository).findByPostCode(nonExistentPostcode);
    }

}
