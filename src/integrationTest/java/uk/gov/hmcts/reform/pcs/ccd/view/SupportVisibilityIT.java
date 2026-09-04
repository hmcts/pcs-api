package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.stream.Collectors.toSet;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("External support visibility")
class SupportVisibilityIT extends AbstractPostgresContainerIT {

    private static final UUID CLAIMANT_SOLICITOR_USER_ID = UUID.randomUUID();
    private static final String CLAIMANT_FIRM = "CLAIMANT-FIRM";
    private static final String DEFENDANT_FIRM = "DEFENDANT-FIRM";
    private static final String UNRELATED_FIRM = "UNRELATED-FIRM";

    private static final String REASONABLE_ADJUSTMENT_CODE = "RA0042";
    private static final String SPECIAL_MEASURE_CODE = "SM0002";
    private static final String LANGUAGE_INTERPRETER_CODE = "PF0015";
    private static final String INTERNAL_FLAG_CODE = "PF0011";

    @Autowired
    private CaseFlagsView underTest;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private FlagRefDataRepository flagRefDataRepository;

    @MockitoBean
    private SecurityContextService securityContextService;

    @MockitoBean
    private OrganisationService organisationService;

    @Test
    @DisplayName("shows the claimant representative their own party's support and none of the defendant's")
    void showsOnlyClaimantSupportToTheClaimantRepresentative() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        readingAsUserFromOrganisation(CLAIMANT_FIRM);

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(supportPartyIds(pcsCase)).containsExactly(claimantPartyId(caseEntity).toString());
        assertThat(supportFlagCodes(pcsCase))
            .containsExactlyInAnyOrder(REASONABLE_ADJUSTMENT_CODE, SPECIAL_MEASURE_CODE,
                                       LANGUAGE_INTERPRETER_CODE);
    }

    @Test
    @DisplayName("shows the defendant representative their own party's support and none of the claimant's")
    void showsOnlyDefendantSupportToTheDefendantRepresentative() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        readingAsUserFromOrganisation(DEFENDANT_FIRM);

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(supportPartyIds(pcsCase)).containsExactly(defendantPartyId(caseEntity).toString());
        assertThat(supportFlagCodes(pcsCase)).containsExactly(REASONABLE_ADJUSTMENT_CODE);
    }

    @Test
    @DisplayName("keeps the defendant out of a claimant professional who also holds the defendant solicitor role")
    void keepsDefendantSupportFromAClaimantProfessionalHoldingBothSolicitorRoles() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        readingAsUserFromOrganisation(CLAIMANT_FIRM);
        holdingBothSolicitorRoles();

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(supportPartyIds(pcsCase)).containsExactly(claimantPartyId(caseEntity).toString());
        assertThat(supportPartyIds(pcsCase)).doesNotContain(defendantPartyId(caseEntity).toString());
    }

    @Test
    @DisplayName("keeps the claimant out of a defendant professional who also holds the claimant solicitor role")
    void keepsClaimantSupportFromADefendantProfessionalHoldingBothSolicitorRoles() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        readingAsUserFromOrganisation(DEFENDANT_FIRM);
        holdingBothSolicitorRoles();

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(supportPartyIds(pcsCase)).containsExactly(defendantPartyId(caseEntity).toString());
        assertThat(supportPartyIds(pcsCase)).doesNotContain(claimantPartyId(caseEntity).toString());
    }

    @Test
    @DisplayName("keeps both parties out of an unrelated professional who holds both solicitor roles")
    void keepsBothPartiesFromAnUnrelatedProfessionalHoldingBothSolicitorRoles() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        readingAsUserFromOrganisation(UNRELATED_FIRM);
        holdingBothSolicitorRoles();

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(pcsCase.getPartySupport()).isEmpty();
    }

    @Test
    @DisplayName("shows no support once representation has ended even while both solicitor roles are held")
    void keepsSupportFromAProfessionalWhoseRepresentationHasEndedDespiteBothSolicitorRoles() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        endDefendantRepresentation(caseEntity);
        readingAsUserFromOrganisation(DEFENDANT_FIRM);
        holdingBothSolicitorRoles();

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(pcsCase.getPartySupport()).isEmpty();
    }

    @Test
    @DisplayName("shows no support to a professional from an organisation that represents neither party")
    void showsNoSupportToAnUnrelatedProfessional() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        readingAsUserFromOrganisation(UNRELATED_FIRM);

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(pcsCase.getPartySupport()).isEmpty();
    }

    @Test
    @DisplayName("keeps internal flags out of the support projection for a represented party")
    void keepsInternalFlagsOutOfSupport() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        readingAsUserFromOrganisation(CLAIMANT_FIRM);

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(supportFlagCodes(pcsCase)).doesNotContain(INTERNAL_FLAG_CODE);
    }

    @Test
    @DisplayName("ignores representation that has been ended")
    void ignoresEndedRepresentation() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        endDefendantRepresentation(caseEntity);
        readingAsUserFromOrganisation(DEFENDANT_FIRM);

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(pcsCase.getPartySupport()).isEmpty();
    }

    @Test
    @DisplayName("shows the party themselves their own support when they act without representation")
    void showsSupportToThePartyActingForThemselves() {
        PcsCaseEntity caseEntity = caseWithSupportOnBothSides();
        claimantPartyOf(caseEntity).setIdamId(CLAIMANT_SOLICITOR_USER_ID);
        claimantPartyOf(caseEntity).setOrganisationId(null);
        pcsCaseRepository.saveAndFlush(caseEntity);
        readingAsUserFromOrganisation(UNRELATED_FIRM);

        PCSCase pcsCase = readCase(caseEntity);

        assertThat(supportPartyIds(pcsCase)).containsExactly(claimantPartyId(caseEntity).toString());
    }

    private void holdingBothSolicitorRoles() {
        when(securityContextService.getCurrentUserDetails()).thenReturn(
            UserInfo.builder()
                .uid(CLAIMANT_SOLICITOR_USER_ID.toString())
                .roles(List.of("caseworker-pcs-solicitor", "claimant-solicitor", "defendant-solicitor",
                               "[CLAIMANTSOLICITOR]", "[DEFENDANTSOLICITOR]"))
                .build());
    }

    private void readingAsUserFromOrganisation(String organisationId) {
        when(securityContextService.getCurrentUserId()).thenReturn(CLAIMANT_SOLICITOR_USER_ID);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
    }

    private PCSCase readCase(PcsCaseEntity caseEntity) {
        PCSCase pcsCase = PCSCase.builder()
            .parties(caseEntity.getParties().stream()
                         .map(partyEntity -> ListValue.<Party>builder()
                             .value(Party.builder().id(partyEntity.getId().toString()).build())
                             .build())
                         .toList())
            .build();

        underTest.setCaseFields(pcsCase, caseEntity);

        return pcsCase;
    }

    private PcsCaseEntity caseWithSupportOnBothSides() {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(nextCaseReference());

        PartyEntity claimantParty = PartyEntity.builder()
            .orgName("Anytown Housing Association")
            .organisationId(CLAIMANT_FIRM)
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

        representDefendantBy(defendantParty, DEFENDANT_FIRM);

        addSupport(claimantParty, REASONABLE_ADJUSTMENT_CODE, "External");
        addSupport(claimantParty, SPECIAL_MEASURE_CODE, "External");
        addSupport(claimantParty, LANGUAGE_INTERPRETER_CODE, "External");
        addSupport(claimantParty, INTERNAL_FLAG_CODE, "Internal");
        addSupport(defendantParty, REASONABLE_ADJUSTMENT_CODE, "External");

        return pcsCaseRepository.saveAndFlush(caseEntity);
    }

    private void representDefendantBy(PartyEntity defendantParty, String organisationId) {
        OrganisationEntity organisation = OrganisationEntity.builder()
            .organisationId(organisationId)
            .organisationName(organisationId)
            .build();

        ClaimPartyOrganisationEntity representation = ClaimPartyOrganisationEntity.builder()
            .party(defendantParty)
            .organisation(organisation)
            .active(YesOrNo.YES)
            .startDate(Instant.parse("2026-01-01T00:00:00Z"))
            .build();

        organisation.getClaimPartyOrganisationList().add(representation);
        defendantParty.getClaimPartyOrganisationList().add(representation);
    }

    private void endDefendantRepresentation(PcsCaseEntity caseEntity) {
        defendantPartyOf(caseEntity).getClaimPartyOrganisationList()
            .forEach(representation -> representation.setActive(YesOrNo.NO));
        pcsCaseRepository.saveAndFlush(caseEntity);
    }

    private void addSupport(PartyEntity partyEntity, String flagCode, String visibility) {
        FlagRefDataEntity flagRefData = flagRefDataRepository.findByFlagCode(flagCode)
            .orElseGet(() -> flagRefDataRepository.saveAndFlush(FlagRefDataEntity.builder()
                                                                   .flagCode(flagCode)
                                                                   .flagName(flagCode)
                                                                   .visibility(visibility)
                                                                   .hearingRelevant(true)
                                                                   .availableExternally(true)
                                                                   .build()));

        CasePartyFlagEntity supportFlag = new CasePartyFlagEntity();
        supportFlag.setParentEntity(null, partyEntity);
        supportFlag.setFlagRefData(flagRefData);
        supportFlag.setVisibility(visibility);
        supportFlag.setDefaultStatus("Active");
        supportFlag.setDateTimeCreated(LocalDateTime.of(2026, 8, 1, 12, 0));
        supportFlag.setPaths(":Party");

        partyEntity.getDefendantFlags().add(supportFlag);
    }

    private PartyEntity claimantPartyOf(PcsCaseEntity caseEntity) {
        return partyWithRole(caseEntity, PartyRole.CLAIMANT);
    }

    private PartyEntity defendantPartyOf(PcsCaseEntity caseEntity) {
        return partyWithRole(caseEntity, PartyRole.DEFENDANT);
    }

    private UUID claimantPartyId(PcsCaseEntity caseEntity) {
        return claimantPartyOf(caseEntity).getId();
    }

    private UUID defendantPartyId(PcsCaseEntity caseEntity) {
        return defendantPartyOf(caseEntity).getId();
    }

    private PartyEntity partyWithRole(PcsCaseEntity caseEntity, PartyRole partyRole) {
        return caseEntity.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == partyRole)
            .map(ClaimPartyEntity::getParty)
            .findFirst()
            .orElseThrow();
    }

    private Set<String> supportPartyIds(PCSCase pcsCase) {
        return pcsCase.getPartySupport().stream()
            .map(ListValue::getId)
            .collect(toSet());
    }

    private List<String> supportFlagCodes(PCSCase pcsCase) {
        return pcsCase.getPartySupport().stream()
            .map(ListValue::getValue)
            .map(PartySupport::getSupportFlags)
            .flatMap(supportFlags -> supportFlags.getDetails().stream())
            .map(ListValue::getValue)
            .map(FlagDetail::getFlagCode)
            .toList();
    }

    private long nextCaseReference() {
        return 1781000000000000L + Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1000000000L);
    }
}
