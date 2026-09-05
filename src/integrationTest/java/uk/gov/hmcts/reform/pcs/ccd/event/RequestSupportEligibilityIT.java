package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Request Support defendant eligibility")
class RequestSupportEligibilityIT extends AbstractPostgresContainerIT {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String CLAIMANT_FIRM = "CLAIMANT-FIRM";
    private static final String DEFENDANT_FIRM = "DEFENDANT-FIRM";
    private static final String REASONABLE_ADJUSTMENT_CODE = "RA0042";

    @Autowired
    private PcsCaseService underTest;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private FlagRefDataRepository flagRefDataRepository;

    @MockitoBean
    private SecurityContextService securityContextService;

    @MockitoBean
    private OrganisationService organisationService;

    @Test
    @DisplayName("A: dual-role user with only an active claimant relationship cannot request claimant support")
    void dualRoleWithClaimantAffiliationOnlyCannotRequestClaimantSupport() {
        PcsCaseEntity caseEntity = caseWithBothSidesRepresented();
        actingAs(CLAIMANT_FIRM);
        holdingBothSolicitorRoles();

        assertThatThrownBy(() -> requestSupportFor(caseEntity, claimantPartyOf(caseEntity)))
            .isInstanceOf(CaseAccessException.class);
    }

    @Test
    @DisplayName("A2: the same user cannot request support for the defendant they do not represent")
    void dualRoleWithClaimantAffiliationOnlyCannotRequestDefendantSupport() {
        PcsCaseEntity caseEntity = caseWithBothSidesRepresented();
        actingAs(CLAIMANT_FIRM);
        holdingBothSolicitorRoles();

        assertThatThrownBy(() -> requestSupportFor(caseEntity, defendantPartyOf(caseEntity)))
            .isInstanceOf(CaseAccessException.class);
    }

    @Test
    @DisplayName("B: dual-role user with an active defendant relationship can request that defendant's support")
    void dualRoleWithDefendantAffiliationCanRequestDefendantSupport() {
        PcsCaseEntity caseEntity = caseWithBothSidesRepresented();
        actingAs(DEFENDANT_FIRM);
        holdingBothSolicitorRoles();

        assertThatCode(() -> requestSupportFor(caseEntity, defendantPartyOf(caseEntity)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("C: a user representing both sides may request support for the defendant only")
    void userRepresentingBothSidesMayRequestDefendantSupportOnly() {
        PcsCaseEntity caseEntity = caseWithBothSidesRepresented(DEFENDANT_FIRM);
        actingAs(DEFENDANT_FIRM);
        holdingBothSolicitorRoles();

        assertThatCode(() -> requestSupportFor(caseEntity, defendantPartyOf(caseEntity)))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> requestSupportFor(caseEntity, claimantPartyOf(caseEntity)))
            .isInstanceOf(CaseAccessException.class);
    }

    @Test
    @DisplayName("D: a defendant representative with an active relationship can request support")
    void defendantRepresentativeCanRequestSupport() {
        PcsCaseEntity caseEntity = caseWithBothSidesRepresented();
        actingAs(DEFENDANT_FIRM);

        assertThatCode(() -> requestSupportFor(caseEntity, defendantPartyOf(caseEntity)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("E: ended defendant representation cannot request support")
    void endedDefendantRepresentationCannotRequestSupport() {
        PcsCaseEntity caseEntity = caseWithBothSidesRepresented();
        defendantPartyOf(caseEntity).getClaimPartyOrganisationList()
            .forEach(representation -> representation.setActive(YesOrNo.NO));
        pcsCaseRepository.saveAndFlush(caseEntity);
        actingAs(DEFENDANT_FIRM);

        assertThatThrownBy(() -> requestSupportFor(caseEntity, defendantPartyOf(caseEntity)))
            .isInstanceOf(CaseAccessException.class);
    }

    @Test
    @DisplayName("F: a self-representing defendant can still request their own support")
    void selfRepresentingDefendantCanRequestOwnSupport() {
        PcsCaseEntity caseEntity = caseWithBothSidesRepresented();
        PartyEntity defendantParty = defendantPartyOf(caseEntity);
        defendantParty.setIdamId(USER_ID);
        defendantParty.getClaimPartyOrganisationList().clear();
        pcsCaseRepository.saveAndFlush(caseEntity);
        actingAs("UNRELATED-FIRM");

        assertThatCode(() -> requestSupportFor(caseEntity, defendantPartyOf(caseEntity)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("G: a crafted payload naming the claimant party is rejected for a defendant representative")
    void craftedPayloadNamingTheClaimantPartyIsRejected() {
        PcsCaseEntity caseEntity = caseWithBothSidesRepresented();
        actingAs(DEFENDANT_FIRM);
        holdingBothSolicitorRoles();

        assertThatThrownBy(() -> requestSupportFor(caseEntity, claimantPartyOf(caseEntity)))
            .isInstanceOf(CaseAccessException.class);
    }

    private void requestSupportFor(PcsCaseEntity caseEntity, PartyEntity partyEntity) {
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(ListValue.<PartySupport>builder()
                                      .id(partyEntity.getId().toString())
                                      .value(PartySupport.builder()
                                                 .supportFlags(newExternalSupport())
                                                 .build())
                                      .build()))
            .build();

        underTest.patchRequestedSupportFlags(caseEntity.getCaseReference(), pcsCase);
    }

    private Flags newExternalSupport() {
        return Flags.builder()
            .details(List.of(ListValue.<FlagDetail>builder()
                                 .id(UUID.randomUUID().toString())
                                 .value(FlagDetail.builder()
                                            .flagCode(REASONABLE_ADJUSTMENT_CODE)
                                            .name("Reasonable adjustment")
                                            .flagComment("eligibility probe")
                                            .status("Requested")
                                            .build())
                                 .build()))
            .build();
    }

    private void actingAs(String organisationId) {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
    }

    private void holdingBothSolicitorRoles() {
        when(securityContextService.getCurrentUserDetails()).thenReturn(
            UserInfo.builder()
                .uid(USER_ID.toString())
                .roles(List.of("caseworker-pcs-solicitor", "claimant-solicitor", "defendant-solicitor",
                               "[CLAIMANTSOLICITOR]", "[DEFENDANTSOLICITOR]"))
                .build());
    }

    private PcsCaseEntity caseWithBothSidesRepresented() {
        return caseWithBothSidesRepresented(CLAIMANT_FIRM);
    }

    private PcsCaseEntity caseWithBothSidesRepresented(String claimantOrganisationId) {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(
            1781000000000000L + Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1000000000L));

        PartyEntity claimantParty = PartyEntity.builder()
            .orgName("Anytown Housing Association")
            .organisationId(claimantOrganisationId)
            .build();
        PartyEntity defendantParty = PartyEntity.builder()
            .firstName("Danny")
            .lastName("Defendant")
            .build();

        caseEntity.addParty(claimantParty);
        caseEntity.addParty(defendantParty);

        ClaimEntity claim = ClaimEntity.builder().build();
        caseEntity.addClaim(claim);
        claim.addParty(claimantParty, PartyRole.CLAIMANT);
        claim.addParty(defendantParty, PartyRole.DEFENDANT);

        OrganisationEntity defendantFirm = OrganisationEntity.builder()
            .organisationId(DEFENDANT_FIRM)
            .organisationName(DEFENDANT_FIRM)
            .build();
        ClaimPartyOrganisationEntity representation = ClaimPartyOrganisationEntity.builder()
            .party(defendantParty)
            .organisation(defendantFirm)
            .active(YesOrNo.YES)
            .startDate(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
        defendantFirm.getClaimPartyOrganisationList().add(representation);
        defendantParty.getClaimPartyOrganisationList().add(representation);

        flagRefDataRepository.findByFlagCode(REASONABLE_ADJUSTMENT_CODE)
            .orElseGet(() -> flagRefDataRepository.saveAndFlush(FlagRefDataEntity.builder()
                                                                    .flagCode(REASONABLE_ADJUSTMENT_CODE)
                                                                    .flagName("Reasonable adjustment")
                                                                    .visibility("External")
                                                                    .hearingRelevant(true)
                                                                    .availableExternally(true)
                                                                    .build()));

        return pcsCaseRepository.saveAndFlush(caseEntity);
    }

    private PartyEntity claimantPartyOf(PcsCaseEntity caseEntity) {
        return partyWithRole(caseEntity, PartyRole.CLAIMANT);
    }

    private PartyEntity defendantPartyOf(PcsCaseEntity caseEntity) {
        return partyWithRole(caseEntity, PartyRole.DEFENDANT);
    }

    private PartyEntity partyWithRole(PcsCaseEntity caseEntity, PartyRole partyRole) {
        return caseEntity.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == partyRole)
            .map(claimParty -> claimParty.getParty())
            .findFirst()
            .orElseThrow();
    }
}
