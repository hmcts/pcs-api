package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class CaseTitleServiceTest {

    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private PCSCase pcsCase;

    private CaseTitleService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseTitleService(addressFormatter);
    }

    @Test
    void shouldIncludeCaseReferenceInPageTitle() {
        // When
        String actualCaseTitle = underTest.buildCaseTitle(pcsCase);

        // Then
        assertThat(actualCaseTitle).contains("${[CASE_REFERENCE]}");
    }

    @Test
    void shouldIncludeFormattedPropertyAddressInPageTitle() {
        // Given
        String expectedPropertyAddress = "expected property address";

        AddressUK propertyAddress = mock(AddressUK.class);
        when(pcsCase.getPropertyAddress()).thenReturn(propertyAddress);
        when(addressFormatter.formatShortAddress(propertyAddress, COMMA_DELIMITER))
            .thenReturn(expectedPropertyAddress);

        // When
        String actualCaseTitle = underTest.buildCaseTitle(pcsCase);

        // Then
        assertThat(actualCaseTitle).contains(expectedPropertyAddress);
    }

    @Test
    void shouldHandleNullAddressGracefully() {
        // Given
        when(pcsCase.getPropertyAddress()).thenReturn(null);
        when(addressFormatter.formatAddressWithCommas(null)).thenReturn("");

        // When
        String actualCaseTitle = underTest.buildCaseTitle(pcsCase);

        // Then
        assertThat(actualCaseTitle).contains("Property address:");
        assertThat(actualCaseTitle).doesNotContain("Not provided");
    }

    @Test
    void shouldHandleEmptyFormattedAddress() {
        // Given
        AddressUK propertyAddress = mock(AddressUK.class);
        when(pcsCase.getPropertyAddress()).thenReturn(propertyAddress);
        when(addressFormatter.formatAddressWithCommas(propertyAddress)).thenReturn("");

        // When
        String actualCaseTitle = underTest.buildCaseTitle(pcsCase);

        // Then
        assertThat(actualCaseTitle).contains("Property address:");
        assertThat(actualCaseTitle).doesNotContain("Not provided");
    }

}
