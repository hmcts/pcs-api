package uk.gov.hmcts.reform.pcs.ccd.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import static org.assertj.core.api.Assertions.assertThat;

@PcsCcdEventTest
@Transactional
class GenAppDraftOrderWordingIT extends AbstractPostgresContainerIT {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private GenAppRepository genAppRepository;

    @Test
    void shouldReloadFormattedDraftWordingLongerThanTheExplanationLimit() {
        String draft = "<ol><li>" + "Proposed order wording. ".repeat(400)
            + "</li><li><strong>Costs reserved.</strong></li></ol>";
        GenAppEntity application = saveApplication(draft);

        entityManager.clear();

        GenAppEntity reloaded = genAppRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getDraftOrderWording()).isEqualTo(draft);
        assertThat(reloaded.getWhatOrderWanted()).isEqualTo("Please adjourn the hearing");
    }

    @Test
    void shouldReloadAnApplicationWithoutDraftWording() {
        GenAppEntity application = saveApplication(null);

        entityManager.clear();

        assertThat(genAppRepository.findById(application.getId()).orElseThrow().getDraftOrderWording()).isNull();
    }

    private GenAppEntity saveApplication(String draft) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
        entityManager.persist(pcsCase);
        PartyEntity party = PartyEntity.builder().pcsCase(pcsCase).orgName("Claimant").build();
        entityManager.persist(party);
        GenAppEntity application = GenAppEntity.builder()
            .pcsCase(pcsCase)
            .party(party)
            .rank(1)
            .type(GenAppType.ADJOURN)
            .state(GenAppState.PENDING_GEN_APP_ISSUED)
            .whatOrderWanted("Please adjourn the hearing")
            .draftOrderWording(draft)
            .build();
        return genAppRepository.saveAndFlush(application);
    }
}
