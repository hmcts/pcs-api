package uk.gov.hmcts.reform.pcs.ccd.service.party;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefendantPartyExtractorTest {

    private static final long CASE_REFERENCE = 12345L;

    private DefendantPartyExtractor underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantPartyExtractor();
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoDefendantsFound() {
        // Given
        ClaimPartyEntity claimantParty = ClaimPartyEntity.builder()
            .role(PartyRole.CLAIMANT)
            .party(PartyEntity.builder().build())
            .build();

        ClaimEntity claimEntity = ClaimEntity.builder()
            .claimParties(List.of(claimantParty))
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .claims(List.of(claimEntity))
            .build();

        // When / Then
        assertThatThrownBy(() ->
                               underTest.extractDefendants(caseEntity, CASE_REFERENCE))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No defendants associated with this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoClaimExists() {
        // Given
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(Collections.emptyList())
            .build();

        // When / Then
        assertThatThrownBy(() ->
                               underTest.extractDefendants(caseEntity, CASE_REFERENCE))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No claim found for this case");
    }

    @Test
    void shouldReturnDefendantsWhenDefendantsExist() {
        // Given
        PartyEntity defendant1 = PartyEntity.builder().build();
        PartyEntity defendant2 = PartyEntity.builder().build();

        ClaimPartyEntity defendantParty1 = ClaimPartyEntity.builder()
            .role(PartyRole.DEFENDANT)
            .party(defendant1)
            .build();

        ClaimPartyEntity defendantParty2 = ClaimPartyEntity.builder()
            .role(PartyRole.DEFENDANT)
            .party(defendant2)
            .build();

        ClaimPartyEntity claimantParty = ClaimPartyEntity.builder()
            .role(PartyRole.CLAIMANT)
            .party(PartyEntity.builder().build())
            .build();

        ClaimEntity claimEntity = ClaimEntity.builder()
            .claimParties(List.of(
                claimantParty,
                defendantParty1,
                defendantParty2
            ))
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .claims(List.of(claimEntity))
            .build();

        // When
        List<PartyEntity> result =
            underTest.extractDefendants(caseEntity, CASE_REFERENCE);

        // Then
        assertThat(result)
            .containsExactly(defendant1, defendant2);
    }
}
