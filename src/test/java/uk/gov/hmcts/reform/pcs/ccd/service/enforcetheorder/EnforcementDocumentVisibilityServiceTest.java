package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementDocumentVisibilityServiceTest {

    private static final String CLAIMANT_ORG_ID = "claimant-org";

    @Mock
    private PartyService partyService;

    @Mock
    private PcsCaseEntity pcsCaseEntity;

    private EnforcementDocumentVisibilityService underTest;

    @BeforeEach
    void setUp() {
        underTest = new EnforcementDocumentVisibilityService(partyService);
    }

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
    @ValueSource(strings = {"hearing-centre-admin", "fee-paid-judge"})
    void shouldBeVisibleToStaffAndJudiciary(String role) {
        assertThat(underTest.isVisibleToUser(List.of(role), pcsCaseEntity, null)).isTrue();
        verifyNoInteractions(partyService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"claimant", "claimant-solicitor", "[CLAIMANTSOLICITOR]"})
    void shouldBeVisibleToTheClaimantOrganisationOnTheCase(String role) {
        givenClaimantOrganisation();

        assertThat(underTest.isVisibleToUser(List.of(role), pcsCaseEntity, CLAIMANT_ORG_ID)).isTrue();
    }

    @Test
    void shouldNotBeVisibleToDefendantSolicitorHoldingOrganisationWideClaimantRole() {
        givenClaimantOrganisation();

        assertThat(underTest.isVisibleToUser(List.of("claimant", "[DEFENDANTSOLICITOR]"), pcsCaseEntity,
                                             "defendant-org")).isFalse();
    }

    @Test
    void shouldNotBeVisibleToExternalUserWithoutOrganisation() {
        assertThat(underTest.isVisibleToUser(List.of("[DEFENDANT]"), pcsCaseEntity, null)).isFalse();
        verifyNoInteractions(partyService);
    }

    private void givenClaimantOrganisation() {
        PartyEntity claimant = PartyEntity.builder().organisationId(CLAIMANT_ORG_ID).build();
        when(partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity)).thenReturn(claimant);
    }
}
