package uk.gov.hmcts.reform.pcs.ccd.service.party;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepForDefendantAccessValidatorTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private OrganisationDetailsService organisationDetailsService;
    @Mock
    private DefendantPartyExtractor defendantPartyExtractor;

    private LegalRepForDefendantAccessValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepForDefendantAccessValidator(organisationDetailsService, defendantPartyExtractor);
    }

    @Test
    void shouldReturnDefendantWhenAuthenticatedLegalRepBelongsToLinkedOrganisation() {
        UUID authenticatedUserId = UUID.randomUUID();
        String organisationId = "ORG-123";

        PartyEntity defendant = PartyEntity.builder().build();
        LegalRepresentativeOrganisationEntity linkedRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .organisationId(organisationId)
            .build();
        defendant.setPartyLegalRepresentativeOrganisationList(List.of(
            PartyLegalRepresentativeOrganisationEntity.builder()
                .party(defendant)
                .legalRepresentativeOrganisation(linkedRepresentative)
                .active(YesOrNo.YES)
                .build()
        ));

        PcsCaseEntity caseEntity = createCaseWithDefendant(defendant);

        when(organisationDetailsService.getOrganisationIdentifier(authenticatedUserId.toString()))
            .thenReturn(organisationId);
        List<PartyEntity> defendants = List.of(defendant);
        when(defendantPartyExtractor.extractDefendants(caseEntity, CASE_REFERENCE)).thenReturn(defendants);

        List<PartyEntity> result = underTest.validateAndGetDefendants(caseEntity, authenticatedUserId);

        assertThat(result).containsExactly(defendant);
    }

    @Test
    void shouldReturnDefendant() {
        UUID authenticatedUserId = UUID.randomUUID();
        String organisationId = "ORG-123";

        PartyEntity defendant = PartyEntity.builder().build();
        LegalRepresentativeOrganisationEntity linkedRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .organisationId(organisationId)
            .build();
        LegalRepresentativeOrganisationEntity linkedRepresentative2 = LegalRepresentativeOrganisationEntity.builder()
            .organisationId(organisationId + "1")
            .build();
        defendant.setPartyLegalRepresentativeOrganisationList(List.of(
            PartyLegalRepresentativeOrganisationEntity.builder()
                .party(defendant)
                .legalRepresentativeOrganisation(linkedRepresentative2)
                .active(YesOrNo.NO)
                .build(),
            PartyLegalRepresentativeOrganisationEntity.builder()
                .party(defendant)
                .legalRepresentativeOrganisation(linkedRepresentative)
                .active(YesOrNo.YES)
                .build()
        ));

        PcsCaseEntity caseEntity = createCaseWithDefendant(defendant);

        when(organisationDetailsService.getOrganisationIdentifier(authenticatedUserId.toString()))
            .thenReturn(organisationId);
        List<PartyEntity> defendants = List.of(defendant);
        when(defendantPartyExtractor.extractDefendants(caseEntity, CASE_REFERENCE)).thenReturn(defendants);

        List<PartyEntity> result = underTest.validateAndGetDefendants(caseEntity, authenticatedUserId);

        assertThat(result).containsExactly(defendant);
    }

    @Test
    void shouldThrowWhenLegalRepIsInDifferentOrganisation() {
        UUID authenticatedUserId = UUID.randomUUID();

        PartyEntity defendant = PartyEntity.builder().build();
        LegalRepresentativeOrganisationEntity linkedRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .organisationId("ORG-123")
            .build();
        defendant.setPartyLegalRepresentativeOrganisationList(List.of(
            PartyLegalRepresentativeOrganisationEntity.builder()
                .party(defendant)
                .legalRepresentativeOrganisation(linkedRepresentative)
                .active(YesOrNo.YES)
                .build()
        ));

        PcsCaseEntity caseEntity = createCaseWithDefendant(defendant);

        when(organisationDetailsService.getOrganisationIdentifier(authenticatedUserId.toString()))
            .thenReturn("ORG-999");

        assertThatThrownBy(() -> underTest.validateAndGetDefendants(caseEntity, authenticatedUserId))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant solicitor on this case");
    }

    @Test
    void shouldThrowWhenLegalRepIsLinkIsInactive() {
        UUID authenticatedUserId = UUID.randomUUID();

        PartyEntity defendant = PartyEntity.builder().build();
        LegalRepresentativeOrganisationEntity linkedRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .organisationId("ORG-123")
            .build();
        defendant.setPartyLegalRepresentativeOrganisationList(List.of(
            PartyLegalRepresentativeOrganisationEntity.builder()
                .party(defendant)
                .legalRepresentativeOrganisation(linkedRepresentative)
                .active(YesOrNo.NO)
                .build()
        ));

        PcsCaseEntity caseEntity = createCaseWithDefendant(defendant);

        when(organisationDetailsService.getOrganisationIdentifier(authenticatedUserId.toString()))
            .thenReturn("ORG-123");

        assertThatThrownBy(() -> underTest.validateAndGetDefendants(caseEntity, authenticatedUserId))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant solicitor on this case");
    }

    @Test
    void shouldThrowWhenAuthenticatedOrganisationIdIsBlankAndUserIdsDoNotMatch() {
        // Given
        UUID authenticatedUserId = UUID.randomUUID();

        PartyEntity defendant = PartyEntity.builder().build();

        LegalRepresentativeOrganisationEntity linkedRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .organisationId("ORG-123")
            .build();

        defendant.setPartyLegalRepresentativeOrganisationList(List.of(
            PartyLegalRepresentativeOrganisationEntity.builder()
                .party(defendant)
                .legalRepresentativeOrganisation(linkedRepresentative)
                .active(YesOrNo.YES)
                .build()
        ));

        PcsCaseEntity caseEntity = createCaseWithDefendant(defendant);

        when(organisationDetailsService.getOrganisationIdentifier(authenticatedUserId.toString()))
            .thenReturn("");

        List<PartyEntity> defendants = List.of(defendant);

        when(defendantPartyExtractor.extractDefendants(caseEntity, CASE_REFERENCE))
            .thenReturn(defendants);

        // When / Then
        assertThatThrownBy(() ->
                               underTest.validateAndGetDefendants(caseEntity, authenticatedUserId))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant solicitor on this case");
    }

    @Test
    void shouldThrowWhenOrganisationIdsDoNotMatchAndUserIdsDiffer() {
        // Given
        UUID authenticatedUserId = UUID.randomUUID();

        PartyEntity defendant = PartyEntity.builder().build();

        LegalRepresentativeOrganisationEntity linkedRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .organisationId("ORG-123")
            .build();

        defendant.setPartyLegalRepresentativeOrganisationList(List.of(
            PartyLegalRepresentativeOrganisationEntity.builder()
                .party(defendant)
                .legalRepresentativeOrganisation(linkedRepresentative)
                .active(YesOrNo.YES)
                .build()
        ));

        PcsCaseEntity caseEntity = createCaseWithDefendant(defendant);

        when(organisationDetailsService.getOrganisationIdentifier(authenticatedUserId.toString()))
            .thenReturn("ORG-999");

        List<PartyEntity> defendants = List.of(defendant);

        when(defendantPartyExtractor.extractDefendants(caseEntity, CASE_REFERENCE))
            .thenReturn(defendants);

        // When / Then
        assertThatThrownBy(() ->
                               underTest.validateAndGetDefendants(caseEntity, authenticatedUserId))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant solicitor on this case");
    }

    private PcsCaseEntity createCaseWithDefendant(PartyEntity defendant) {
        ClaimEntity claimEntity = ClaimEntity.builder().build();
        claimEntity.getClaimParties().add(ClaimPartyEntity.builder()
            .party(defendant)
            .role(PartyRole.DEFENDANT)
            .build());

        return PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(claimEntity))
            .build();
    }

}
