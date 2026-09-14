package uk.gov.hmcts.reform.pcs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SupportTabAccessTest extends CftlibTest {

    private static final String OWN_PARTY_SUPPORT = "OWN_PARTY_SUPPORT";
    private static final String OPPOSITE_PARTY_SUPPORT = "OPPOSITE_PARTY_SUPPORT";

    private static final String REASONABLE_ADJUSTMENT_CODE = "RA0042";
    private static final String SPECIAL_MEASURE_CODE = "SM0002";
    private static final String LANGUAGE_INTERPRETER_CODE = "PF0015";

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private IdamClient idamClient;

    @Autowired
    private CaseCreationService caseCreationService;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private FlagRefDataRepository flagRefDataRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String claimantSolicitorToken;

    @BeforeAll
    void setUp() {
        claimantSolicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
    }

    @Test
    void shouldExposeOnlyRepresentedPartySupportToAnExternalProfessional() {
        long caseReference = caseCreationService.createMinimalCase(claimantSolicitorToken);

        PartyIds partyIds = addExternalSupportToBothSides(caseReference);

        CaseDetails retrievedCase = ccdClient.getCaseDetails(caseReference, claimantSolicitorToken);

        List<Map<String, Object>> partySupport = partySupportFrom(retrievedCase);

        assertThat(partySupport)
            .extracting(entry -> entry.get("id"))
            .contains(partyIds.claimantPartyId().toString())
            .doesNotContain(partyIds.defendantPartyId().toString());

        assertThat(supportFlagComments(partySupport))
            .contains(OWN_PARTY_SUPPORT + "-" + REASONABLE_ADJUSTMENT_CODE,
                      OWN_PARTY_SUPPORT + "-" + SPECIAL_MEASURE_CODE,
                      OWN_PARTY_SUPPORT + "-" + LANGUAGE_INTERPRETER_CODE);

        assertThat(supportFlagComments(partySupport))
            .noneMatch(flagComment -> flagComment.startsWith(OPPOSITE_PARTY_SUPPORT));

        assertThat(retrievedCase.getData().toString()).doesNotContain(OPPOSITE_PARTY_SUPPORT);
    }

    private PartyIds addExternalSupportToBothSides(long caseReference) {
        return transactionTemplate.execute(status -> {
            PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();

            PartyEntity claimantParty = partyWithRole(pcsCaseEntity, PartyRole.CLAIMANT);
            PartyEntity defendantParty = partyWithRole(pcsCaseEntity, PartyRole.DEFENDANT);

            addExternalSupport(claimantParty, OWN_PARTY_SUPPORT);
            addExternalSupport(defendantParty, OPPOSITE_PARTY_SUPPORT);

            partyRepository.saveAll(List.of(claimantParty, defendantParty));

            return new PartyIds(claimantParty.getId(), defendantParty.getId());
        });
    }

    private PartyEntity partyWithRole(PcsCaseEntity pcsCaseEntity, PartyRole partyRole) {
        return pcsCaseEntity.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == partyRole)
            .map(ClaimPartyEntity::getParty)
            .findFirst()
            .orElseThrow();
    }

    private void addExternalSupport(PartyEntity partyEntity, String supportOwner) {
        List.of(REASONABLE_ADJUSTMENT_CODE, SPECIAL_MEASURE_CODE, LANGUAGE_INTERPRETER_CODE)
            .forEach(flagCode -> partyEntity.getDefendantFlags()
                .add(externalSupportFlag(partyEntity, flagCode, supportOwner + "-" + flagCode)));
    }

    private CasePartyFlagEntity externalSupportFlag(PartyEntity partyEntity, String flagCode, String flagComment) {
        FlagRefDataEntity flagRefData = flagRefDataRepository.findByFlagCode(flagCode)
            .orElseGet(() -> flagRefDataRepository.save(FlagRefDataEntity.builder()
                                                            .flagCode(flagCode)
                                                            .flagName(flagCode)
                                                            .visibility("External")
                                                            .hearingRelevant(true)
                                                            .availableExternally(true)
                                                            .build()));

        CasePartyFlagEntity supportFlag = new CasePartyFlagEntity();
        supportFlag.setParty(partyEntity);
        supportFlag.setFlagRefData(flagRefData);
        supportFlag.setVisibility("External");
        supportFlag.setDefaultStatus("Active");
        supportFlag.setFlagComment(flagComment);
        supportFlag.setDateTimeCreated(LocalDateTime.now());
        supportFlag.setPaths(partyEntity.getId() + ":Party");

        return supportFlag;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> partySupportFrom(CaseDetails caseDetails) {
        return (List<Map<String, Object>>) caseDetails.getData().get("partySupport");
    }

    @SuppressWarnings("unchecked")
    private List<String> supportFlagComments(List<Map<String, Object>> partySupport) {
        return partySupport.stream()
            .map(entry -> (Map<String, Object>) entry.get("value"))
            .map(value -> (Map<String, Object>) value.get("supportFlags"))
            .filter(supportFlags -> supportFlags.get("details") != null)
            .flatMap(supportFlags -> ((List<Map<String, Object>>) supportFlags.get("details")).stream())
            .map(detail -> (Map<String, Object>) detail.get("value"))
            .map(detail -> (String) detail.get("flagComment"))
            .toList();
    }

    private record PartyIds(UUID claimantPartyId, UUID defendantPartyId) {
    }
}
