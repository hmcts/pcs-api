package uk.gov.hmcts.reform.pcs.ccd.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Case flags persistence")
class FlagRefDataCodeConstraintIT extends AbstractPostgresContainerIT {

    private static final LocalDateTime FLAG_CREATED = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Autowired
    private FlagRefDataRepository flagRefDataRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private PcsCaseService pcsCaseService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("rejects a second row for a flag code already held")
    void rejectsDuplicateFlagCode() {
        flagRefDataRepository.saveAndFlush(flagRefData("RA0035", "Video hearing"));

        assertThatThrownBy(() ->
            flagRefDataRepository.saveAndFlush(flagRefData("RA0035", "Video hearing (duplicate)")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("allows a row for each distinct flag code")
    void allowsDistinctFlagCodes() {
        flagRefDataRepository.saveAndFlush(flagRefData("RA0012", "Braille documents"));
        flagRefDataRepository.saveAndFlush(flagRefData("PF0015", "Language Interpreter"));

        assertThat(flagRefDataRepository.findByFlagCode("RA0012")).isPresent();
        assertThat(flagRefDataRepository.findByFlagCode("PF0015")).isPresent();
    }

    @Test
    @DisplayName("updates the stored case flag row in place instead of replacing it")
    void updatesStoredCaseFlagRowInPlace() {
        long caseReference = 1781000000009362L;
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(caseReference);

        CaseFlagEntity storedFlag = new CaseFlagEntity();
        storedFlag.setFlagRefData(flagRefDataRepository.saveAndFlush(flagRefData("OT0001", "Other")));
        storedFlag.setVisibility("Internal");
        storedFlag.setDefaultStatus("Active");
        storedFlag.setFlagComment("Stored comment");
        storedFlag.setOtherDescription("Retired judge on case");
        storedFlag.setOtherDescriptionWelsh("Barnwr wedi ymddeol ar yr achos");
        storedFlag.setSubTypeValue("Stored sub type");
        storedFlag.setPaths(":Case");
        storedFlag.setDateTimeCreated(FLAG_CREATED);
        storedFlag.setParentEntity(caseEntity, null);
        caseEntity.getCaseFlags().add(storedFlag);

        pcsCaseRepository.saveAndFlush(caseEntity);
        UUID storedFlagId = storedFlag.getId();
        assertThat(storedFlagId).isNotNull();

        // A v2.1 update payload: it echoes the stored flag id and edits the status and comment, and does
        // not repeat the other-description, sub type, path or creation fields.
        PCSCase pcsCase = PCSCase.builder()
            .caseFlags(Flags.builder()
                           .visibility(FlagVisibility.INTERNAL)
                           .details(List.of(ListValue.<FlagDetail>builder()
                               .id(storedFlagId.toString())
                               .value(FlagDetail.builder()
                                   .flagCode("OT0001")
                                   .name("Other")
                                   .status("Inactive")
                                   .flagComment("Updated comment")
                                   .flagUpdateComment("No longer needed")
                                   .hearingRelevant(YesOrNo.YES)
                                   .availableExternally(YesOrNo.NO)
                                   .build())
                               .build()))
                           .build())
            .build();

        pcsCaseService.patchCaseFlags(caseReference, pcsCase);

        entityManager.flush();
        entityManager.clear();

        PcsCaseEntity reloaded = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();
        assertThat(reloaded.getCaseFlags()).hasSize(1);

        CaseFlagEntity reloadedFlag = reloaded.getCaseFlags().getFirst();
        assertThat(reloadedFlag.getId()).isEqualTo(storedFlagId);
        assertThat(reloadedFlag.getOtherDescription()).isEqualTo("Retired judge on case");
        assertThat(reloadedFlag.getOtherDescriptionWelsh()).isEqualTo("Barnwr wedi ymddeol ar yr achos");
        assertThat(reloadedFlag.getSubTypeValue()).isEqualTo("Stored sub type");
        assertThat(reloadedFlag.getPaths()).isEqualTo(":Case");
        assertThat(reloadedFlag.getDateTimeCreated()).isEqualTo(FLAG_CREATED);
        assertThat(reloadedFlag.getDefaultStatus()).isEqualTo("Inactive");
        assertThat(reloadedFlag.getFlagComment()).isEqualTo("Updated comment");
        assertThat(reloadedFlag.getFlagUpdateComment()).isEqualTo("No longer needed");

        Long rowsForCase = (Long) entityManager
            .createQuery("select count(f) from CaseFlagEntity f where f.pcsCase.caseReference = :ref")
            .setParameter("ref", caseReference)
            .getSingleResult();
        assertThat(rowsForCase).isEqualTo(1L);
    }

    @Test
    @DisplayName("stores the other descriptions a reviewer adds to a requested support flag")
    void storesOtherDescriptionsAddedWhileReviewingSupport() {
        long caseReference = 1781000000009366L;
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(caseReference);

        PartyEntity partyEntity = new PartyEntity();
        partyEntity.setFirstName("Defendant");
        partyEntity.setLastName("Two");
        caseEntity.addParty(partyEntity);

        CasePartyFlagEntity requestedFlag = new CasePartyFlagEntity();
        requestedFlag.setFlagRefData(flagRefDataRepository.saveAndFlush(flagRefData("RA0034", "In person hearing")));
        requestedFlag.setVisibility(FlagVisibility.EXTERNAL.getValue());
        requestedFlag.setDefaultStatus("Requested");
        requestedFlag.setFlagComment("Original support comment");
        requestedFlag.setPaths(":Party");
        requestedFlag.setDateTimeCreated(FLAG_CREATED);
        requestedFlag.setParentEntity(null, partyEntity);
        partyEntity.getDefendantFlags().add(requestedFlag);

        pcsCaseRepository.saveAndFlush(caseEntity);
        UUID partyId = partyEntity.getId();
        UUID requestedFlagId = requestedFlag.getId();

        // The reviewer approves the request and adds a translation, which carries the other descriptions.
        pcsCaseService.patchReviewedSupportFlags(caseReference, List.of(
            ListValue.<PartySupport>builder()
                .id(partyId.toString())
                .value(PartySupport.builder()
                           .supportFlags(Flags.builder()
                                             .visibility(FlagVisibility.INTERNAL)
                                             .groupId(partyId)
                                             .details(List.of(ListValue.<FlagDetail>builder()
                                                 .id(requestedFlagId.toString())
                                                 .value(FlagDetail.builder()
                                                     .flagCode("RA0034")
                                                     .name("In person hearing")
                                                     .status("Active")
                                                     .flagComment("Reviewed support comment")
                                                     .flagCommentCy("Sylw cymorth wedi ei adolygu")
                                                     .otherDescription("Hearing room on the ground floor")
                                                     .otherDescriptionCy("Ystafell wrandawiad ar y llawr gwaelod")
                                                     .flagUpdateComment("Approved")
                                                     .build())
                                                 .build()))
                                             .build())
                           .build())
                .build()));

        entityManager.flush();
        entityManager.clear();

        List<CasePartyFlagEntity> reloadedFlags = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow().getParties().iterator().next().getDefendantFlags();
        assertThat(reloadedFlags).hasSize(1);

        CasePartyFlagEntity reloadedFlag = reloadedFlags.getFirst();
        assertThat(reloadedFlag.getId()).isEqualTo(requestedFlagId);
        assertThat(reloadedFlag.getOtherDescription()).isEqualTo("Hearing room on the ground floor");
        assertThat(reloadedFlag.getOtherDescriptionWelsh()).isEqualTo("Ystafell wrandawiad ar y llawr gwaelod");
        assertThat(reloadedFlag.getFlagComment()).isEqualTo("Reviewed support comment");
        assertThat(reloadedFlag.getFlagCommentWelsh()).isEqualTo("Sylw cymorth wedi ei adolygu");
        assertThat(reloadedFlag.getDefaultStatus()).isEqualTo("Active");
        assertThat(reloadedFlag.getVisibility()).isEqualTo(FlagVisibility.EXTERNAL.getValue());
        assertThat(reloadedFlag.getDateTimeCreated()).isEqualTo(FLAG_CREATED);
    }

    @Test
    @DisplayName("stores the other descriptions a caseworker adds through manage case flags")
    void storesOtherDescriptionsAddedWhileManagingCaseFlags() {
        long caseReference = 1781000000009367L;
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(caseReference);

        CaseFlagEntity storedFlag = new CaseFlagEntity();
        storedFlag.setFlagRefData(flagRefDataRepository.saveAndFlush(flagRefData("CF0004", "Gender recognition")));
        storedFlag.setVisibility(FlagVisibility.INTERNAL.getValue());
        storedFlag.setDefaultStatus("Active");
        storedFlag.setPaths(":Case");
        storedFlag.setDateTimeCreated(FLAG_CREATED);
        storedFlag.setParentEntity(caseEntity, null);
        caseEntity.getCaseFlags().add(storedFlag);

        pcsCaseRepository.saveAndFlush(caseEntity);
        UUID storedFlagId = storedFlag.getId();

        PCSCase pcsCase = PCSCase.builder()
            .caseFlags(Flags.builder()
                           .visibility(FlagVisibility.INTERNAL)
                           .details(List.of(ListValue.<FlagDetail>builder()
                               .id(storedFlagId.toString())
                               .value(FlagDetail.builder()
                                   .flagCode("CF0004")
                                   .name("Gender recognition")
                                   .status("Active")
                                   .flagComment("Managed comment")
                                   .flagCommentCy("Sylw wedi ei reoli")
                                   .otherDescription("Preferred title and pronouns")
                                   .otherDescriptionCy("Teitl a rhagenwau dewisol")
                                   .hearingRelevant(YesOrNo.YES)
                                   .availableExternally(YesOrNo.NO)
                                   .build())
                               .build()))
                           .build())
            .build();

        pcsCaseService.patchCaseFlags(caseReference, pcsCase);

        entityManager.flush();
        entityManager.clear();

        CaseFlagEntity reloadedFlag = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow().getCaseFlags().getFirst();
        assertThat(reloadedFlag.getId()).isEqualTo(storedFlagId);
        assertThat(reloadedFlag.getOtherDescription()).isEqualTo("Preferred title and pronouns");
        assertThat(reloadedFlag.getOtherDescriptionWelsh()).isEqualTo("Teitl a rhagenwau dewisol");
        assertThat(reloadedFlag.getFlagComment()).isEqualTo("Managed comment");
        assertThat(reloadedFlag.getFlagCommentWelsh()).isEqualTo("Sylw wedi ei reoli");
    }

    private FlagRefDataEntity flagRefData(String flagCode, String flagName) {
        return FlagRefDataEntity.builder()
            .flagCode(flagCode)
            .flagName(flagName)
            .hearingRelevant(true)
            .availableExternally(true)
            .build();
    }
}
