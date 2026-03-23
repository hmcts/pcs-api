package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;



class CaseLinkEntityTest {

    private CaseLinkEntity underTest;


    @BeforeEach
    void setUp() {
        underTest = new CaseLinkEntity();
    }

    @Test
    void shouldAddCaseLinkReason() {
        // Given
        String reasonCode = "CLR003";
        String reasonText = "Same Party";

        // When
        underTest.addReason(reasonCode, reasonText);

        // Then
        assertThat(underTest.getReasons()).hasSize(1);
        assertThat(underTest.getReasons().getFirst().getReasonCode()).isEqualTo("CLR003");
        assertThat(underTest.getReasons().getFirst().getReasonText()).isEqualTo("Same Party");
    }
}
