package uk.gov.hmcts.reform.pcs.controllers;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeHashingService;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class CaseCreationHelper {

    static final String ACCESS_CODE = "ABC123XYZ789";

    private final PcsCaseRepository pcsCaseRepository;
    private final PartyAccessCodeRepository partyAccessCodeRepository;
    private final PartyAccessCodeHashingService hashingService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public CaseCreationHelper(
        PcsCaseRepository pcsCaseRepository,
        PartyAccessCodeRepository partyAccessCodeRepository,
        PartyAccessCodeHashingService hashingService,
        JdbcTemplate jdbcTemplate,
        PlatformTransactionManager transactionManager
    ) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.partyAccessCodeRepository = partyAccessCodeRepository;
        this.hashingService = hashingService;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public PcsCaseEntity createTestCaseWithParty(long caseReference, UUID idamUserId, PartyRole partyRole) {
        return createUnauditedCase(caseReference, () -> {
            PcsCaseEntity caseEntity = new PcsCaseEntity();
            caseEntity.setCaseReference(caseReference);

            ClaimEntity claimEntity = ClaimEntity.builder().build();

            caseEntity.addClaim(claimEntity);

            PartyEntity party = new PartyEntity();
            party.setIdamId(idamUserId);
            party.setFirstName("John");
            party.setLastName("Doe");
            if (PartyRole.CLAIMANT == partyRole) {
                party.setOrgName("Test Claimant");
            }
            caseEntity.addParty(party);
            claimEntity.addParty(party, partyRole);

            return pcsCaseRepository.save(caseEntity);
        });
    }

    public PcsCaseEntity createTestCaseWithMultipleDefendants(
        long caseReference, UUID firstIdamUserId, UUID secondIdamUserId) {
        return createUnauditedCase(caseReference, () -> {
            PcsCaseEntity caseEntity = new PcsCaseEntity();
            caseEntity.setCaseReference(caseReference);

            ClaimEntity claimEntity = ClaimEntity.builder().build();

            caseEntity.addClaim(claimEntity);

            PartyEntity defendant1 = new PartyEntity();
            defendant1.setIdamId(firstIdamUserId);
            defendant1.setFirstName("John");
            defendant1.setLastName("Doe");

            PartyEntity defendant2 = new PartyEntity();
            defendant2.setIdamId(secondIdamUserId);
            defendant2.setFirstName("Jane");
            defendant2.setLastName("Smith");

            caseEntity.addParty(defendant1);
            caseEntity.addParty(defendant2);

            claimEntity.addParty(defendant1, PartyRole.DEFENDANT);
            claimEntity.addParty(defendant2, PartyRole.DEFENDANT);

            return pcsCaseRepository.save(caseEntity);
        });
    }

    public String createPartyAccessCode(PcsCaseEntity caseEntity, UUID partyId) {
        return runUnaudited(() -> {
            PartyAccessCodeEntity pac = PartyAccessCodeEntity.builder()
                .partyId(partyId)
                .pcsCase(caseEntity)
                .code(hashingService.encodeForStorage(ACCESS_CODE))
                .role(PartyRole.DEFENDANT)
                .build();

            partyAccessCodeRepository.save(pac);
            return ACCESS_CODE;
        });
    }

    public void runUnaudited(Runnable action) {
        runUnaudited(() -> {
            action.run();
            return null;
        });
    }

    public <T> T runUnaudited(Supplier<T> fixtureSetup) {
        return transactionTemplate.execute(status -> {
            jdbcTemplate.queryForObject(
                "select set_config('ccd.audit_disabled', 'true', true)",
                String.class
            );
            return fixtureSetup.get();
        });
    }

    public <T> T createUnauditedCase(long caseReference, Supplier<T> fixtureSetup) {
        return runUnaudited(() -> {
            resetCase(caseReference);
            return fixtureSetup.get();
        });
    }

    List<PartyEntity> getDefendants(PcsCaseEntity pcsCaseEntity) {
        return transactionTemplate.execute(status -> pcsCaseRepository
            .findByCaseReference(pcsCaseEntity.getCaseReference())
            .orElseThrow()
            .getClaims()
            .getFirst()
            .getClaimParties()
            .stream()
            .filter(claimPartyEntity -> claimPartyEntity.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList());
    }

    private void resetCase(long caseReference) {
        pcsCaseRepository.findByCaseReference(caseReference).ifPresent(pcsCaseRepository::delete);
        pcsCaseRepository.flush();
        jdbcTemplate.update("delete from ccd.case_data where reference = ?", caseReference);
        jdbcTemplate.update("""
            insert into ccd.case_data (
                id, reference, version, security_classification, jurisdiction, case_type_id,
                state, data, supplementary_data, last_modified, last_state_modified_date
            ) values (
                ?, ?, 1, 'PUBLIC', 'PCS', 'PCS', 'AWAITING_SUBMISSION_TO_HMCTS',
                '{}'::jsonb, '{}'::jsonb, now() at time zone 'UTC', now() at time zone 'UTC'
            )
            """, caseReference, caseReference);
    }

}
