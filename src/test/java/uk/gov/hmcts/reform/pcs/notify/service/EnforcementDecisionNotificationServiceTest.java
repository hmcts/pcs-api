package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RejectionReason;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementApplicationDecisionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationRecipient;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.enforcement.EnforcementDecisionPersonalisation;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementDecisionNotificationServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String MANAGE_CASE_URL = "http://manage-case.test";

    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationPersonalisationFactory notificationPersonalisationFactory;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Captor
    private ArgumentCaptor<EnforcementDecisionPersonalisation> personalisationCaptor;

    private EnforcementDecisionNotificationService underTest;

    @BeforeEach
    void setUp() {
        when(claimEntity.getPcsCase()).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getCaseReference()).thenReturn(CASE_REFERENCE);

        underTest = new EnforcementDecisionNotificationService(
            notificationService,
            notificationPersonalisationFactory,
            MANAGE_CASE_URL
        );
    }

    @Nested
    @DisplayName("Warrant emails")
    class WarrantEmails {

        @Test
        void shouldSendWarrantApprovedEmail() {
            // Given
            NotificationRecipient claimantRecipient = mock(NotificationRecipient.class);
            PartyEntity partyEntity = mock(PartyEntity.class);
            when(notificationService.claimantRecipient(claimEntity)).thenReturn(claimantRecipient);
            when(claimantRecipient.party()).thenReturn(partyEntity);

            BasePersonalisation basePersonalisation = mock(BasePersonalisation.class);
            when(notificationPersonalisationFactory.forParty(partyEntity, pcsCaseEntity))
                .thenReturn(basePersonalisation);

            // When
            underTest.sendWarrantApprovedEmail(claimEntity);

            // Then
            verify(notificationService).sendEmail(
                eq(claimantRecipient),
                eq(EmailTemplate.WARRANT_APP_APPROVED),
                eq(NotificationClaimType.ENFORCEMENT_APP_DECISION),
                personalisationCaptor.capture()
            );

            EnforcementDecisionPersonalisation personalisation = personalisationCaptor.getValue();
            assertThat(personalisation.getBase()).isEqualTo(basePersonalisation);
            assertThat(personalisation.getCaseOverviewUrl())
                .isEqualTo("%s/cases/case-details/PCS/PCS/%d", MANAGE_CASE_URL, CASE_REFERENCE);

        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("warrantRejectionScenarios")
        void shouldSendWarrantRejectedEmail(RejectionReason rejectionReason, EmailTemplate expectedEmailTemplate) {
            // Given
            NotificationRecipient claimantRecipient = mock(NotificationRecipient.class);
            PartyEntity partyEntity = mock(PartyEntity.class);
            when(notificationService.claimantRecipient(claimEntity)).thenReturn(claimantRecipient);
            when(claimantRecipient.party()).thenReturn(partyEntity);

            BasePersonalisation basePersonalisation = mock(BasePersonalisation.class);
            when(notificationPersonalisationFactory.forParty(partyEntity, pcsCaseEntity))
                .thenReturn(basePersonalisation);

            String rejectionDetails = "rejection details";
            EnforcementApplicationDecisionEntity decisionEntity = EnforcementApplicationDecisionEntity.builder()
                .rejectionReason(rejectionReason)
                .rejectionReasonAdditionalDetails(rejectionDetails)
                .build();

            // When
            underTest.sendWarrantRejectedEmail(claimEntity, decisionEntity);

            // Then
            verify(notificationService).sendEmail(
                eq(claimantRecipient),
                eq(expectedEmailTemplate),
                eq(NotificationClaimType.ENFORCEMENT_APP_DECISION),
                personalisationCaptor.capture()
            );

            EnforcementDecisionPersonalisation personalisation = personalisationCaptor.getValue();
            assertThat(personalisation.getBase()).isEqualTo(basePersonalisation);
            assertThat(personalisation.getCaseOverviewUrl())
                .isEqualTo("%s/cases/case-details/PCS/PCS/%d", MANAGE_CASE_URL, CASE_REFERENCE);
            assertThat(personalisation.getRejectionDetails()).isEqualTo(rejectionDetails);
        }

        private static Stream<Arguments> warrantRejectionScenarios() {
            return Stream.of(
                arguments(
                    RejectionReason.CHANGE_IN_PARTY_CIRCUMSTANCES,
                    EmailTemplate.WARRANT_APP_REFUSED_PARTY_CIRCUMSTANCES
                ),
                arguments(
                    RejectionReason.APPLYING_OUT_OF_TIME,
                    EmailTemplate.WARRANT_APP_REFUSED_ENFORCE_OUT_OF_TIME
                ),
                arguments(
                    RejectionReason.ORDER_NOT_ENFORCEABLE,
                    EmailTemplate.WARRANT_APP_REFUSED_NOT_ENFORCEABLE
                ),
                arguments(
                    RejectionReason.ORDER_SET_ASIDE_OR_SUSPENDED,
                    EmailTemplate.WARRANT_APP_REFUSED_SET_ASIDE_OR_SUSPENDED
                ),
                arguments(
                    RejectionReason.APPLYING_BEFORE_POSSESSION_DATE,
                    EmailTemplate.WARRANT_APP_REFUSED_BEFORE_POSSESSION_DATE
                ),
                arguments(
                    RejectionReason.ANOTHER_ENFORCEMENT_ONGOING,
                    EmailTemplate.WARRANT_APP_REFUSED_ANOTHER_ENFORCEMENT_ONGOING
                ),
                arguments(
                    RejectionReason.DEFENDANTS_IN_BREATHING_SPACE,
                    EmailTemplate.WARRANT_APP_REFUSED_BREATHING_SPACE
                ),
                arguments(
                    RejectionReason.OTHER,
                    EmailTemplate.WARRANT_APP_REFUSED_OTHER_REASON
                )
            );
        }
    }

    @Nested
    @DisplayName("Writ emails")
    class WritEmails {

        @Test
        void shouldSendWritApprovedEmail() {
            // Given
            NotificationRecipient claimantRecipient = mock(NotificationRecipient.class);
            PartyEntity partyEntity = mock(PartyEntity.class);
            when(notificationService.claimantRecipient(claimEntity)).thenReturn(claimantRecipient);
            when(claimantRecipient.party()).thenReturn(partyEntity);

            BasePersonalisation basePersonalisation = mock(BasePersonalisation.class);
            when(notificationPersonalisationFactory.forParty(partyEntity, pcsCaseEntity))
                .thenReturn(basePersonalisation);

            // When
            underTest.sendWritApprovedEmail(claimEntity);

            // Then
            verify(notificationService).sendEmail(
                eq(claimantRecipient),
                eq(EmailTemplate.WRIT_APP_APPROVED),
                eq(NotificationClaimType.ENFORCEMENT_APP_DECISION),
                personalisationCaptor.capture()
            );

            EnforcementDecisionPersonalisation personalisation = personalisationCaptor.getValue();
            assertThat(personalisation.getBase()).isEqualTo(basePersonalisation);
            assertThat(personalisation.getCaseOverviewUrl())
                .isEqualTo("%s/cases/case-details/PCS/PCS/%d", MANAGE_CASE_URL, CASE_REFERENCE);

        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("writRejectionScenarios")
        void shouldSendWritRejectedEmail(RejectionReason rejectionReason, EmailTemplate expectedEmailTemplate) {
            // Given
            NotificationRecipient claimantRecipient = mock(NotificationRecipient.class);
            PartyEntity partyEntity = mock(PartyEntity.class);
            when(notificationService.claimantRecipient(claimEntity)).thenReturn(claimantRecipient);
            when(claimantRecipient.party()).thenReturn(partyEntity);

            BasePersonalisation basePersonalisation = mock(BasePersonalisation.class);
            when(notificationPersonalisationFactory.forParty(partyEntity, pcsCaseEntity))
                .thenReturn(basePersonalisation);

            String rejectionDetails = "rejection details";
            EnforcementApplicationDecisionEntity decisionEntity = EnforcementApplicationDecisionEntity.builder()
                .rejectionReason(rejectionReason)
                .rejectionReasonAdditionalDetails(rejectionDetails)
                .build();

            // When
            underTest.sendWritRejectedEmail(claimEntity, decisionEntity);

            // Then
            verify(notificationService).sendEmail(
                eq(claimantRecipient),
                eq(expectedEmailTemplate),
                eq(NotificationClaimType.ENFORCEMENT_APP_DECISION),
                personalisationCaptor.capture()
            );

            EnforcementDecisionPersonalisation personalisation = personalisationCaptor.getValue();
            assertThat(personalisation.getBase()).isEqualTo(basePersonalisation);
            assertThat(personalisation.getCaseOverviewUrl())
                .isEqualTo("%s/cases/case-details/PCS/PCS/%d", MANAGE_CASE_URL, CASE_REFERENCE);
            assertThat(personalisation.getRejectionDetails()).isEqualTo(rejectionDetails);
        }

        private static Stream<Arguments> writRejectionScenarios() {
            return Stream.of(
                arguments(
                    RejectionReason.CHANGE_IN_PARTY_CIRCUMSTANCES,
                    EmailTemplate.WRIT_APP_REFUSED_PARTY_CIRCUMSTANCES
                ),
                arguments(
                    RejectionReason.APPLYING_OUT_OF_TIME,
                    EmailTemplate.WRIT_APP_REFUSED_ENFORCE_OUT_OF_TIME
                ),
                arguments(
                    RejectionReason.ORDER_NOT_ENFORCEABLE,
                    EmailTemplate.WRIT_APP_REFUSED_NOT_ENFORCEABLE
                ),
                arguments(
                    RejectionReason.ORDER_SET_ASIDE_OR_SUSPENDED,
                    EmailTemplate.WRIT_APP_REFUSED_SET_ASIDE_OR_SUSPENDED
                ),
                arguments(
                    RejectionReason.CASE_NOT_TRANSFERRED_TO_HIGH_COURT,
                    EmailTemplate.WRIT_APP_REFUSED_NOT_TFR_TO_HIGH_COURT
                ),
                arguments(
                    RejectionReason.APPLYING_BEFORE_POSSESSION_DATE,
                    EmailTemplate.WRIT_APP_REFUSED_BEFORE_POSSESSION_DATE
                ),
                arguments(
                    RejectionReason.ANOTHER_ENFORCEMENT_ONGOING,
                    EmailTemplate.WRIT_APP_REFUSED_ANOTHER_ENFORCEMENT_ONGOING
                ),
                arguments(
                    RejectionReason.DEFENDANTS_IN_BREATHING_SPACE,
                    EmailTemplate.WRIT_APP_REFUSED_BREATHING_SPACE
                ),
                arguments(
                    RejectionReason.OTHER,
                    EmailTemplate.WRIT_APP_REFUSED_OTHER_REASON
                )
            );
        }
    }

}
