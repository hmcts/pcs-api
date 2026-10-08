package uk.gov.hmcts.reform.pcs.idam;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("IDAM id mapping against text columns (after V033)")
class IdamUserIdUuidConverterIT extends AbstractPostgresContainerIT {

    private static final String IDAM_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("reads and writes IDAM ids when columns are text")
    void roundTripsAgainstTextColumns() {
        long caseReference = 9_178_000_003L;

        transactionTemplate.executeWithoutResult(status -> {
            PcsCaseEntity caseEntity = new PcsCaseEntity();
            caseEntity.setCaseReference(caseReference);

            ClaimEntity claimEntity = ClaimEntity.builder().build();
            caseEntity.addClaim(claimEntity);

            PartyEntity party = new PartyEntity();
            party.setIdamId(IDAM_ID);
            party.setFirstName("John");
            party.setLastName("Doe");
            caseEntity.addParty(party);
            claimEntity.addParty(party, PartyRole.DEFENDANT);

            pcsCaseRepository.save(caseEntity);
            entityManager.flush();
            entityManager.clear();

            PartyEntity reloaded = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow()
                .getClaims().getFirst().getClaimParties().stream()
                .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
                .map(claimParty -> claimParty.getParty())
                .findFirst()
                .orElseThrow();

            assertThat(reloaded.getIdamId()).isEqualToIgnoringCase(IDAM_ID);
        });
    }
}
