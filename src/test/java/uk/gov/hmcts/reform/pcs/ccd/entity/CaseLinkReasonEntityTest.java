package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import  static org.assertj.core.api.Assertions.assertThat;


class CaseLinkReasonEntityTest {

    private CaseLinkReasonEntity underTest;


    @BeforeEach
    void setUp() {
        underTest = new CaseLinkReasonEntity();
    }

    @Test
    void shouldUpdateCaseLinkEntity() {
        // Given
        CaseLinkEntity updatedCaseLinkEntity = mock(CaseLinkEntity.class);

        // When
        underTest.setCaseLink(updatedCaseLinkEntity);

        // Then
        assertThat(underTest.getCaseLink()).isSameAs(updatedCaseLinkEntity);
    }
}
