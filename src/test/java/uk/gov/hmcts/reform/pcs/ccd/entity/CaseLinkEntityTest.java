package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.List;

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
        CaseLinkEntity caseLinkEntity = CaseLinkEntity.builder().build();
        CaseLinkReasonEntity caseLinkReasonEntity = CaseLinkReasonEntity.builder()
            .caseLink(caseLinkEntity)
            .reasonCode(reasonCode)
            .build();


        // When
        underTest.setReasons(List.of(caseLinkReasonEntity));

        // Then
        assertThat(underTest.getReasons()).hasSize(1);
        assertThat(underTest.getReasons().getFirst().getReasonCode()).isEqualTo("CLR003");
    }
}
