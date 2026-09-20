package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"integration"})
@Transactional
public class DraftResponseDeletionServiceIT extends AbstractPostgresContainerIT {

    private static final String SYSTEM_USER_ID_STUB = "system-user-id-token";
    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    private Long caseReferenceCount = 0L;

    @MockitoBean
    private IdamClient idamClient;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private FeeService feeService;
    @MockitoBean
    private OrganisationService organisationService;
    @MockitoBean
    private NotificationService notificationService;
    @Autowired
    private DraftCaseDataRepository draftCaseDataRepository;
    @Autowired
    private DraftResponseDeletionService underTest;
    @Autowired
    private IdamHelper idamHelper;
    @Autowired
    private DraftCaseDataService draftCaseDataService;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void beforeEach() {
        idamHelper.setUpAuthenticatedUser(authorizedClientManager, SYSTEM_USER_ID_STUB, USER_ID, idamClient);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void deleteRespondPossessionClaimBatch() {
        // Given
        int numberOf = 10;
        saveForLater(numberOf);
        assertThat(numberOf).isEqualTo(draftCaseDataRepository.count());

        // When
        underTest.deleteRespondPossessionClaimBatch(10);

        // Then
        draftCaseDataRepository.saveAllAndFlush(draftCaseDataRepository.findAll());
        assertThat(0).isEqualTo(draftCaseDataRepository.count());
    }

    private void saveForLater(int numberOf) {
        for (int i = 0; i < numberOf; i++) {
            EventPayload<PCSCase, State> eventPayload = buildEventPayload();
            draftCaseDataService.patchUnsubmittedEventData(eventPayload.caseReference(),
                                                           eventPayload.caseData(),
                                                           respondPossessionClaim);
        }
        assertThat(draftCaseDataRepository.count()).isEqualTo(numberOf);
        draftCaseDataRepository.saveAllAndFlush(draftCaseDataRepository.findAll());

        Instant past = Instant.now().minus(Duration.ofDays(40));
        entityManager.createNativeQuery("UPDATE draft.draft_case_data SET created_at = :ts")
            .setParameter("ts", OffsetDateTime.ofInstant(past, ZoneOffset.UTC))
            .executeUpdate();
        entityManager.clear();

        draftCaseDataRepository.findAll().forEach(e ->
            assertThat(e.getCreatedAt()).isBefore(Instant.now().minus(Duration.ofDays(30)))
        );

    }

    private EventPayload<PCSCase, State> buildEventPayload() {
        PCSCase pcsCase = PCSCase.builder().build();
        caseReferenceCount = caseReferenceCount + 1;
        return new EventPayload<>(caseReferenceCount, pcsCase, null);
    }

}
