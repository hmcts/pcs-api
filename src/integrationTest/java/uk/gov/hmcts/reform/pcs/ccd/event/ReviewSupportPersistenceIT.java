package uk.gov.hmcts.reform.pcs.ccd.event;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.SupportReviewService;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("Review Support Request persistence against the database")
class ReviewSupportPersistenceIT extends AbstractPostgresContainerIT {

    private static final String FLAG_ROW_SQL = """
        SELECT f.id, f.status, f.visibility, f.flag_comment, f.flag_update_comment,
               f.date_time_created, f.date_time_modified
        FROM case_party_flag f
        WHERE f.id = CAST(:flagId AS uuid)
        """;

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 8, 28, 14, 57, 52);
    private static final LocalDateTime REVIEWED = LocalDateTime.of(2026, 9, 2, 10, 14, 16);

    @Autowired
    private PcsCaseService pcsCaseService;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private FlagRefDataRepository flagRefDataRepository;

    @Autowired
    private SupportReviewService supportReviewService;

    @Autowired
    private CaseFlagsView caseFlagsView;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<Long> createdCaseReferences = new ArrayList<>();
    private final List<String> createdFlagCodes = new ArrayList<>();

    @AfterEach
    void removeCommittedTestData() {
        transactionTemplate().executeWithoutResult(transactionStatus -> {
            createdCaseReferences.forEach(caseReference ->
                pcsCaseRepository.findByCaseReference(caseReference).ifPresent(pcsCaseRepository::delete));
            pcsCaseRepository.flush();
            createdFlagCodes.forEach(flagCode ->
                flagRefDataRepository.findByFlagCode(flagCode).ifPresent(flagRefDataRepository::delete));
        });
        createdCaseReferences.clear();
        createdFlagCodes.clear();
    }

    @Test
    @DisplayName("persists the edited comment, the status reason and the status when moving to Inactive")
    void persistsTheEditedCommentWhenTheStatusChanges() {
        long caseReference = 1781000000019362L;
        Fixture fixture = storeRequestedSupportFlag(
            caseReference, "RA9001", "Private waiting area",
            "DA persistence proof - original comment");

        Object[] before = readFlagRow(fixture.flagId());
        report("SCENARIO 1 - IDENTIFIERS", "case reference: " + caseReference
            + " | party id: " + fixture.partyId() + " | flag id: " + fixture.flagId());
        report("SCENARIO 1 - BEFORE REVIEW", format(before));

        assertThat(before[1]).isEqualTo("Requested");
        assertThat(before[3]).isEqualTo("DA persistence proof - original comment");
        assertThat(before[4]).isNull();

        reviewSupport(caseReference, fixture.flagId(),
                      "DA persistence proof - UPDATED comment",
                      "Inactive",
                      "DA persistence proof - status reason");

        Object[] after = readFlagRow(fixture.flagId());
        report("SCENARIO 1 - REVIEW INPUT",
               "comments: DA persistence proof - UPDATED comment"
                   + " | status: Inactive"
                   + " | reason: DA persistence proof - status reason");
        report("SCENARIO 1 - AFTER REVIEW", format(after));

        assertThat(after[0]).isEqualTo(before[0]);
        assertThat(after[1]).isEqualTo("Inactive");
        assertThat(after[2]).isEqualTo(before[2]);
        assertThat(after[3]).isEqualTo("DA persistence proof - UPDATED comment");
        assertThat(after[4]).isEqualTo("DA persistence proof - status reason");
        assertThat(after[5]).isEqualTo(before[5]);
        assertThat(after[6]).isNotEqualTo(before[6]);

        FlagDetail projected = readFromCaseFlagsView(caseReference, fixture.partyId());
        report("SCENARIO 1 - CASE FLAGS VIEW",
               "flagComment: " + projected.getFlagComment()
                   + " | flagUpdateComment: " + projected.getFlagUpdateComment()
                   + " | status: " + projected.getStatus());

        assertThat(projected.getFlagComment()).isEqualTo("DA persistence proof - UPDATED comment");
        assertThat(projected.getFlagUpdateComment()).isEqualTo("DA persistence proof - status reason");
        assertThat(projected.getStatus()).isEqualTo("Inactive");
        assertThat(projected.getDateTimeCreated()).isEqualTo(CREATED);
    }

    @Test
    @DisplayName("persists the edited comment when the reviewed status remains Requested")
    void persistsTheEditedCommentWhenTheStatusRemainsRequested() {
        long caseReference = 1781000000019363L;
        Fixture fixture = storeRequestedSupportFlag(
            caseReference, "RA9002", "Sign language interpreter",
            "DA persistence proof - requested original");

        Object[] before = readFlagRow(fixture.flagId());
        report("SCENARIO 2 - IDENTIFIERS", "case reference: " + caseReference
            + " | party id: " + fixture.partyId() + " | flag id: " + fixture.flagId());
        report("SCENARIO 2 - BEFORE REVIEW", format(before));

        assertThat(before[1]).isEqualTo("Requested");
        assertThat(before[3]).isEqualTo("DA persistence proof - requested original");

        reviewSupport(caseReference, fixture.flagId(),
                      "DA persistence proof - requested UPDATED",
                      "Requested",
                      "DA persistence proof - still being considered");

        Object[] after = readFlagRow(fixture.flagId());
        report("SCENARIO 2 - REVIEW INPUT",
               "comments: DA persistence proof - requested UPDATED"
                   + " | status: Requested"
                   + " | reason: DA persistence proof - still being considered");
        report("SCENARIO 2 - AFTER REVIEW", format(after));

        assertThat(after[1]).isEqualTo("Requested");
        assertThat(after[3]).isEqualTo("DA persistence proof - requested UPDATED");
        assertThat(after[4]).isEqualTo("DA persistence proof - still being considered");
        assertThat(after[0]).isEqualTo(before[0]);
        assertThat(after[5]).isEqualTo(before[5]);

        FlagDetail projected = readFromCaseFlagsView(caseReference, fixture.partyId());
        report("SCENARIO 2 - CASE FLAGS VIEW",
               "flagComment: " + projected.getFlagComment()
                   + " | flagUpdateComment: " + projected.getFlagUpdateComment()
                   + " | status: " + projected.getStatus());

        assertThat(projected.getFlagComment()).isEqualTo("DA persistence proof - requested UPDATED");
        assertThat(projected.getStatus()).isEqualTo("Requested");
    }

    @Test
    @DisplayName("persists the edited comment when the reviewed status is Not approved")
    void persistsTheEditedCommentWhenTheStatusIsNotApproved() {
        long caseReference = 1781000000019364L;
        Fixture fixture = storeRequestedSupportFlag(
            caseReference, "RA9003", "Hearing loop", "DA persistence proof - not approved original");

        reviewSupport(caseReference, fixture.flagId(),
                      "DA persistence proof - not approved UPDATED",
                      "Not approved",
                      "DA persistence proof - cannot be accommodated");

        Object[] after = readFlagRow(fixture.flagId());
        report("SCENARIO 3 - AFTER REVIEW (Not approved)", format(after));

        assertThat(after[1]).isEqualTo("Not approved");
        assertThat(after[3]).isEqualTo("DA persistence proof - not approved UPDATED");
        assertThat(after[4]).isEqualTo("DA persistence proof - cannot be accommodated");
    }

    /**
     * Runs the Review Support Request event over the stored case: the start callback offers the
     * requested support the view projects, the reviewer edits the offered flag detail exactly as the
     * flags component does, and the submit callback persists it.
     */
    private void reviewSupport(long caseReference, UUID flagId, String comments, String status,
                               String statusChangeReason) {
        transactionTemplate().executeWithoutResult(transactionStatus -> {
            PCSCase pcsCase = retrieveCase(caseReference);
            List<ListValue<PartySupport>> offered = supportReviewService.buildRequestedSupport(pcsCase);

            ListValue<FlagDetail> reviewed = offered.stream()
                .flatMap(support -> support.getValue().getSupportFlags().getDetails().stream())
                .filter(detail -> flagId.toString().equals(detail.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Review screen did not offer flag " + flagId));

            reviewed.getValue().setFlagComment(comments);
            reviewed.getValue().setStatus(status);
            reviewed.getValue().setFlagUpdateComment(statusChangeReason);
            reviewed.getValue().setDateTimeModified(REVIEWED);

            pcsCaseService.patchReviewedSupportFlags(caseReference, offered);
        });
    }

    private FlagDetail readFromCaseFlagsView(long caseReference, UUID partyId) {
        return transactionTemplate().execute(transactionStatus -> retrieveCase(caseReference).getParties().stream()
            .filter(value -> partyId.toString().equals(value.getId()))
            .findFirst()
            .orElseThrow()
            .getValue()
            .getPartyFlagsExternal()
            .getDetails()
            .getFirst()
            .getValue());
    }

    /**
     * The two steps {@code PCSCaseView} performs for these fields when CCD loads a case: the parties
     * are mapped and wrapped from the stored entities, then the flags view projects the stored flags.
     */
    private PCSCase retrieveCase(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();

        PCSCase pcsCase = PCSCase.builder().build();
        pcsCase.setParties(pcsCaseEntity.getParties().stream()
            .map(entity -> modelMapper.map(entity, Party.class))
            .map(party -> ListValue.<Party>builder().id(party.getId()).value(party).build())
            .toList());
        caseFlagsView.setCaseFields(pcsCase, pcsCaseEntity);

        return pcsCase;
    }

    private Object[] readFlagRow(UUID flagId) {
        return transactionTemplate().execute(transactionStatus -> (Object[]) entityManager
            .createNativeQuery(FLAG_ROW_SQL, Object[].class)
            .setParameter("flagId", flagId.toString())
            .getSingleResult());
    }

    private Fixture storeRequestedSupportFlag(long caseReference, String flagCode, String flagName,
                                              String flagComment) {
        return storeRequestedSupportFlags(caseReference, new FlagSpec(flagCode, flagName, flagComment));
    }

    private Fixture storeRequestedSupportFlags(long caseReference, FlagSpec... flagSpecs) {
        createdCaseReferences.add(caseReference);
        Stream.of(flagSpecs).map(FlagSpec::flagCode).forEach(createdFlagCodes::add);

        return transactionTemplate().execute(transactionStatus -> {
            PcsCaseEntity caseEntity = new PcsCaseEntity();
            caseEntity.setCaseReference(caseReference);

            ClaimEntity claimEntity = ClaimEntity.builder().build();
            caseEntity.addClaim(claimEntity);

            PartyEntity claimant = new PartyEntity();
            claimant.setOrgName("Possession Claims Solicitor Org");
            caseEntity.addParty(claimant);
            claimEntity.addParty(claimant, PartyRole.CLAIMANT);

            PartyEntity defendant = new PartyEntity();
            defendant.setFirstName("Danny");
            defendant.setLastName("Defendant");
            caseEntity.addParty(defendant);
            claimEntity.addParty(defendant, PartyRole.DEFENDANT);

            List<CasePartyFlagEntity> storedFlags = new ArrayList<>();
            for (FlagSpec flagSpec : flagSpecs) {
                CasePartyFlagEntity flag = new CasePartyFlagEntity();
                flag.setFlagRefData(refData(flagSpec));
                flag.setDefaultStatus(SupportReviewService.REQUESTED_STATUS);
                flag.setVisibility(FlagVisibility.EXTERNAL.getValue());
                flag.setFlagComment(flagSpec.flagComment());
                flag.setSubTypeValue(flagSpec.flagName());
                flag.setDateTimeCreated(CREATED);
                flag.setPaths(":Party");
                flag.setParty(defendant);
                defendant.getDefendantFlags().add(flag);
                storedFlags.add(flag);
            }

            pcsCaseRepository.saveAndFlush(caseEntity);

            return new Fixture(defendant.getId(), storedFlags.stream().map(CasePartyFlagEntity::getId).toList());
        });
    }

    private FlagRefDataEntity refData(FlagSpec flagSpec) {
        return flagRefDataRepository.findByFlagCode(flagSpec.flagCode())
            .orElseGet(() -> flagRefDataRepository.save(FlagRefDataEntity.builder()
                .flagCode(flagSpec.flagCode())
                .flagName(flagSpec.flagName())
                .hearingRelevant(true)
                .availableExternally(true)
                .build()));
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private String format(Object[] row) {
        return "id=" + row[0]
            + " | status=" + row[1]
            + " | visibility=" + row[2]
            + " | flag_comment=" + row[3]
            + " | flag_update_comment=" + row[4]
            + " | date_time_created=" + row[5]
            + " | date_time_modified=" + row[6];
    }

    private void report(String heading, String detail) {
        System.out.println("[DA-EVIDENCE] " + heading + " :: " + detail);
    }

    private record Fixture(UUID partyId, List<UUID> flagIds) {

        private UUID flagId() {
            return flagIds.getFirst();
        }
    }

    private record FlagSpec(String flagCode, String flagName, String flagComment) {
    }

    @Test
    @DisplayName("leaves an unrelated requested flag on the same party untouched")
    void leavesUnrelatedFlagsUntouched() {
        long caseReference = 1781000000019365L;
        Fixture fixture = storeRequestedSupportFlags(caseReference,
            new FlagSpec("RA9004", "Large print", "DA persistence proof - reviewed flag"),
            new FlagSpec("RA9005", "Documents in braille", "DA persistence proof - untouched flag"));

        reviewSupport(caseReference, fixture.flagIds().getFirst(),
                      "DA persistence proof - reviewed UPDATED", "Inactive", "Reviewed");

        Object[] reviewedRow = readFlagRow(fixture.flagIds().getFirst());
        Object[] untouchedRow = readFlagRow(fixture.flagIds().getLast());
        report("SCENARIO 4 - REVIEWED FLAG", format(reviewedRow));
        report("SCENARIO 4 - UNRELATED FLAG", format(untouchedRow));

        assertThat(reviewedRow[1]).isEqualTo("Inactive");
        assertThat(reviewedRow[3]).isEqualTo("DA persistence proof - reviewed UPDATED");
        assertThat(untouchedRow[1]).isEqualTo("Requested");
        assertThat(untouchedRow[3]).isEqualTo("DA persistence proof - untouched flag");
        assertThat(untouchedRow[4]).isNull();
        assertThat(untouchedRow[6]).isNull();
    }

}
