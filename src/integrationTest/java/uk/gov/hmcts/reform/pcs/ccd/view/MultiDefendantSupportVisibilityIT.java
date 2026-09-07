package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Multi-defendant support visibility")
class MultiDefendantSupportVisibilityIT extends AbstractPostgresContainerIT {

    private static final String CLAIMANT_FIRM = "CLAIMANT-FIRM";
    private static final String DEFENDANT_A_FIRM = "DEFENDANT-A-FIRM";
    private static final String DEFENDANT_B_FIRM = "DEFENDANT-B-FIRM";

    private static final String SUPPORT_CODE = "RA0042";

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
    @DisplayName("shows a defendant representative only the defendant they act for")
    void showsOnlyTheRepresentedDefendant() {
        PcsCaseEntity caseEntity = caseWithThreeDefendants();
        readingAsProfessionalFrom(DEFENDANT_A_FIRM);

        assertThat(supportPartyIds(readCase(caseEntity)))
            .containsExactly(partyId(caseEntity, "Ann"));
    }

    @Test
    @DisplayName("shows the other firm's defendant to that firm only")
    void showsTheOtherDefendantToTheOtherFirm() {
        PcsCaseEntity caseEntity = caseWithThreeDefendants();
        readingAsProfessionalFrom(DEFENDANT_B_FIRM);

        assertThat(supportPartyIds(readCase(caseEntity)))
            .containsExactly(partyId(caseEntity, "Bob"));
    }

    @Test
    @DisplayName("keeps every defendant out of the claimant representative's support")
    void showsTheClaimantRepresentativeNoDefendant() {
        PcsCaseEntity caseEntity = caseWithThreeDefendants();
        readingAsProfessionalFrom(CLAIMANT_FIRM);

        assertThat(supportPartyIds(readCase(caseEntity)))
            .containsExactly(partyId(caseEntity, "Anytown Housing Association"));
    }

    @Test
    @DisplayName("leaves an unrepresented defendant out of every professional's support")
    void leavesTheUnrepresentedDefendantOutForEveryone() {
        PcsCaseEntity caseEntity = caseWithThreeDefendants();
        String unrepresented = partyId(caseEntity, "Cal");

        for (String organisationId : List.of(CLAIMANT_FIRM, DEFENDANT_A_FIRM, DEFENDANT_B_FIRM)) {
            readingAsProfessionalFrom(organisationId);

            assertThat(supportPartyIds(readCase(caseEntity)))
                .as("support seen from %s", organisationId)
                .doesNotContain(unrepresented);
        }
    }

    @Test
    @DisplayName("shows no support when the organisation cannot be resolved")
    void showsNoSupportWhenTheOrganisationIsUnknown() {
        PcsCaseEntity caseEntity = caseWithThreeDefendants();
        readingAsProfessionalFrom(null);

        assertThat(readCase(caseEntity).getPartySupport()).isEmpty();
    }

    private void readingAsProfessionalFrom(String organisationId) {
        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
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

    private PcsCaseEntity caseWithThreeDefendants() {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(nextCaseReference());

        PartyEntity claimant = PartyEntity.builder()
            .orgName("Anytown Housing Association")
            .organisationId(CLAIMANT_FIRM)
            .organisationProfileId("LOCALAUTH_PROFILE")
            .claimCreator(true)
            .build();
        PartyEntity defendantA = PartyEntity.builder().firstName("Ann").lastName("Defendant").build();
        PartyEntity defendantB = PartyEntity.builder().firstName("Bob").lastName("Defendant").build();
        PartyEntity defendantC = PartyEntity.builder().firstName("Cal").lastName("Defendant").build();

        caseEntity.addParty(claimant);
        caseEntity.addParty(defendantA);
        caseEntity.addParty(defendantB);
        caseEntity.addParty(defendantC);

        ClaimEntity claim = ClaimEntity.builder().build();
        caseEntity.addClaim(claim);
        claim.addParty(claimant, PartyRole.CLAIMANT);
        claim.addParty(defendantA, PartyRole.DEFENDANT);
        claim.addParty(defendantB, PartyRole.DEFENDANT);
        claim.addParty(defendantC, PartyRole.DEFENDANT);

        representBy(defendantA, DEFENDANT_A_FIRM);
        representBy(defendantB, DEFENDANT_B_FIRM);

        addSupport(claimant);
        addSupport(defendantA);
        addSupport(defendantB);
        addSupport(defendantC);

        return pcsCaseRepository.saveAndFlush(caseEntity);
    }

    private void representBy(PartyEntity party, String organisationId) {
        OrganisationEntity organisation = OrganisationEntity.builder()
            .organisationId(organisationId)
            .organisationName(organisationId)
            .organisationProfileId("SOLICITOR_PROFILE")
            .build();

        ClaimPartyOrganisationEntity representation = ClaimPartyOrganisationEntity.builder()
            .party(party)
            .organisation(organisation)
            .active(YesOrNo.YES)
            .startDate(Instant.parse("2026-01-01T00:00:00Z"))
            .build();

        organisation.getClaimPartyOrganisationList().add(representation);
        party.getClaimPartyOrganisationList().add(representation);
    }

    private void addSupport(PartyEntity partyEntity) {
        FlagRefDataEntity flagRefData = flagRefDataRepository.findByFlagCode(SUPPORT_CODE)
            .orElseGet(() -> flagRefDataRepository.saveAndFlush(FlagRefDataEntity.builder()
                                                                   .flagCode(SUPPORT_CODE)
                                                                   .flagName(SUPPORT_CODE)
                                                                   .visibility("External")
                                                                   .hearingRelevant(true)
                                                                   .availableExternally(true)
                                                                   .build()));

        CasePartyFlagEntity supportFlag = new CasePartyFlagEntity();
        supportFlag.setParentEntity(null, partyEntity);
        supportFlag.setFlagRefData(flagRefData);
        supportFlag.setVisibility("External");
        supportFlag.setDefaultStatus("Active");
        supportFlag.setDateTimeCreated(LocalDateTime.of(2026, 8, 1, 12, 0));
        supportFlag.setPaths(":Party");

        partyEntity.getDefendantFlags().add(supportFlag);
    }

    private String partyId(PcsCaseEntity caseEntity, String name) {
        return caseEntity.getParties().stream()
            .filter(partyEntity -> name.equals(partyEntity.getFirstName())
                || name.equals(partyEntity.getOrgName()))
            .map(partyEntity -> partyEntity.getId().toString())
            .findFirst()
            .orElseThrow();
    }

    private Set<String> supportPartyIds(PCSCase pcsCase) {
        return pcsCase.getPartySupport().stream()
            .map(ListValue::getId)
            .collect(toSet());
    }

    private long nextCaseReference() {
        return 1781000000000000L + Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1000000000L);
    }
}
