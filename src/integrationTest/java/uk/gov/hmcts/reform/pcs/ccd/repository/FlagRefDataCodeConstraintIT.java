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
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
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

    private FlagRefDataEntity flagRefData(String flagCode, String flagName) {
        return FlagRefDataEntity.builder()
            .flagCode(flagCode)
            .flagName(flagName)
            .hearingRelevant(true)
            .availableExternally(true)
            .build();
    }
}
