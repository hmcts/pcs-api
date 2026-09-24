package uk.gov.hmcts.reform.pcs.ccd.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Legal representative contact details lookup")
class ClaimPartyContactDetailsRepositoryIT extends AbstractPostgresContainerIT {

    private static final String ORGANISATION_ID = "ORG-LOOKUP-IT";

    @Autowired
    private ClaimPartyContactDetailsRepository claimPartyContactDetailsRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("returns the newest row for the organisation on the case when duplicates exist")
    void returnsNewestRowForOrganisationOnCase() {
        PcsCaseEntity thisCase = pcsCaseRepository.save(PcsCaseEntity.builder()
                                                            .caseReference(1234567890123456L).build());
        PcsCaseEntity otherCase = pcsCaseRepository.save(PcsCaseEntity.builder()
                                                             .caseReference(6543210987654321L).build());
        OrganisationEntity organisation = organisation(ORGANISATION_ID);
        organisation.addClaimPartyContactDetails(contactDetails(thisCase, "older@example.com"));
        organisation.addClaimPartyContactDetails(contactDetails(thisCase, "newer@example.com"));
        organisation.addClaimPartyContactDetails(contactDetails(otherCase, "other-case@example.com"));
        entityManager.persist(organisation);
        entityManager.flush();
        entityManager.clear();

        Optional<ClaimPartyContactDetailsEntity> found = claimPartyContactDetailsRepository
            .findFirstByOrganisationOrganisationIdAndPcsCaseCaseReferenceOrderByIdDesc(
                ORGANISATION_ID, thisCase.getCaseReference());

        assertThat(found).isPresent();
        assertThat(found.get().getEmailAddress()).isEqualTo("newer@example.com");
    }

    @Test
    @DisplayName("is empty when the organisation has no contact details on the case")
    void isEmptyWhenOrganisationHasNoRowOnCase() {
        PcsCaseEntity otherCase = pcsCaseRepository.save(PcsCaseEntity.builder()
                                                             .caseReference(4444333322221111L).build());
        final long thisCaseReference = pcsCaseRepository.save(PcsCaseEntity.builder()
                                                                  .caseReference(1111222233334444L).build())
            .getCaseReference();
        OrganisationEntity organisation = organisation(ORGANISATION_ID);
        organisation.addClaimPartyContactDetails(contactDetails(otherCase, "other-case@example.com"));
        entityManager.persist(organisation);
        entityManager.flush();
        entityManager.clear();

        assertThat(claimPartyContactDetailsRepository
                       .findFirstByOrganisationOrganisationIdAndPcsCaseCaseReferenceOrderByIdDesc(
                           ORGANISATION_ID, thisCaseReference)).isEmpty();
        assertThat(claimPartyContactDetailsRepository
                       .findFirstByOrganisationOrganisationIdAndPcsCaseCaseReferenceOrderByIdDesc(
                           "ANOTHER-ORG", otherCase.getCaseReference())).isEmpty();
    }

    private static OrganisationEntity organisation(String organisationId) {
        return OrganisationEntity.builder()
            .organisationId(organisationId)
            .organisationProfileId("SOLICITOR_PROFILE")
            .organisationName("Lookup IT organisation")
            .createdDate(LocalDateTime.of(2026, 9, 2, 12, 0))
            .build();
    }

    private static ClaimPartyContactDetailsEntity contactDetails(PcsCaseEntity pcsCase, String email) {
        return ClaimPartyContactDetailsEntity.builder()
            .pcsCase(pcsCase)
            .emailAddress(email)
            .build();
    }
}
