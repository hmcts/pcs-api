package uk.gov.hmcts.reform.pcs.ccd.service.party;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepForDefendantAccessValidatorTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private DefendantPartyExtractor defendantPartyExtractor;

    private LegalRepForDefendantAccessValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepForDefendantAccessValidator(defendantPartyExtractor);
    }

    @Test
    void shouldReturnDefendantWhenAuthenticatedLegalRepBelongsToLinkedOrganisation() {
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

        List<PartyEntity> defendants = List.of(defendant);
        when(defendantPartyExtractor.extractDefendants(caseEntity, CASE_REFERENCE)).thenReturn(defendants);

        List<PartyEntity> result = underTest.validateAndGetDefendants(caseEntity, organisationId);

        assertThat(result).containsExactly(defendant);
    }

    @Test
    void shouldReturnDefendant() {
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

        List<PartyEntity> defendants = List.of(defendant);
        when(defendantPartyExtractor.extractDefendants(caseEntity, CASE_REFERENCE)).thenReturn(defendants);

        List<PartyEntity> result = underTest.validateAndGetDefendants(caseEntity, organisationId);

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

        assertThatThrownBy(() -> underTest.validateAndGetDefendants(caseEntity, "ORG-999"))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant solicitor on this case");
    }

    @Test
    void shouldThrowWhenLegalRepIsLinkIsInactive() {
        String organisationId = UUID.randomUUID().toString();

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

        assertThatThrownBy(() -> underTest.validateAndGetDefendants(caseEntity, organisationId))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant solicitor on this case");
    }

    @Test
    void shouldThrowWhenAuthenticatedOrganisationIdIsBlankAndUserIdsDoNotMatch() {
        // Given
        String organisationId = "id";

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


        List<PartyEntity> defendants = List.of(defendant);

        when(defendantPartyExtractor.extractDefendants(caseEntity, CASE_REFERENCE))
            .thenReturn(defendants);

        // When / Then
        assertThatThrownBy(() ->
                               underTest.validateAndGetDefendants(caseEntity, organisationId))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant solicitor on this case");
    }

    @Test
    void shouldThrowWhenOrganisationIdsDoNotMatchAndUserIdsDiffer() {
        // Given
        String organisationId = UUID.randomUUID().toString();

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

        List<PartyEntity> defendants = List.of(defendant);

        when(defendantPartyExtractor.extractDefendants(caseEntity, CASE_REFERENCE))
            .thenReturn(defendants);

        // When / Then
        assertThatThrownBy(() ->
                               underTest.validateAndGetDefendants(caseEntity, organisationId))
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
