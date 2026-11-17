package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementDocumentVisibilityServiceTest {

    private final EnforcementDocumentVisibilityService underTest = new EnforcementDocumentVisibilityService();

    @ParameterizedTest
    @EnumSource(value = DocumentType.class, names = {"WRIT_OF_POSSESSION", "WARRANT_OF_POSSESSION", "RISK_ASSESSMENT"})
    void shouldTreatWritWarrantAndRiskAssessmentAsEnforcementDocuments(DocumentType documentType) {
        assertThat(underTest.isEnforcementDocument(documentType)).isTrue();
    }

    @Test
    void shouldNotTreatOtherOrMissingTypesAsEnforcementDocuments() {
        assertThat(underTest.isEnforcementDocument(DocumentType.CLAIM)).isFalse();
        assertThat(underTest.isEnforcementDocument(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"hearing-centre-admin", "fee-paid-judge", "[CLAIMANTSOLICITOR]", "claimant"})
    void shouldBeVisibleToStaffJudiciaryAndClaimants(String role) {
        assertThat(underTest.isVisibleToUser(List.of(role))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"[DEFENDANT]", "[DEFENDANTSOLICITOR]", "defendant-solicitor"})
    void shouldNotBeVisibleToTheDefendantSide(String role) {
        assertThat(underTest.isVisibleToUser(List.of(role))).isFalse();
    }
}
