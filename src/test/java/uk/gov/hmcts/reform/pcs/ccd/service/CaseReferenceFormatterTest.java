package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaseReferenceFormatterTest {

    private CaseReferenceFormatter underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseReferenceFormatter();
    }

    @Test
    void shouldFormatCaseReferenceWithDashes() {
        // When
        String formattedCaseReference = underTest.formatCaseReferenceWithDashes(1777545591382299L);

        // Then
        assertThat(formattedCaseReference).isEqualTo("1777-5455-9138-2299");
    }

    @Test
    void shouldReturnNullForNullCaseReference() {
        // When
        String formattedCaseReference = underTest.formatCaseReferenceWithDashes(null);

        // Then
        assertThat(formattedCaseReference).isNull();
    }

}
