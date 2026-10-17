package uk.gov.hmcts.reform.pcs.enforcement.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationPersonalisationFactory;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnforcementNotificationSenderTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final String TEMPLATE_ID = "a-notify-template-id";

    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationPersonalisationFactory personalisationFactory;
    @Mock
    private PartyService partyService;
    @Mock
    private PcsCaseService pcsCaseService;

    private final EnforcementNotificationTemplates templates = new EnforcementNotificationTemplates();
    private final PcsCaseEntity pcsCase = new PcsCaseEntity();
    private final ClaimEntity claim = new ClaimEntity();
    private final PartyEntity claimant = PartyEntity.builder().emailAddress("claimant@example.com").build();

    EnforcementNotificationSenderTest() {
        templates.setTemplates(new HashMap<>());
        pcsCase.addClaim(claim);
    }

    /**
     * A case whose claimant can be emailed.
     */
    private void givenAnEmailableClaimant() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(partyService.getPrimaryClaimantPartyEntity(pcsCase)).thenReturn(claimant);
        when(partyService.canSendEmailNotification(claimant, PartyRole.CLAIMANT)).thenReturn(true);
        when(personalisationFactory.forClaimant(claim)).thenReturn(BasePersonalisation.builder()
            .firstName("Pat").lastName("Lee").caseNumber("1234-5678-1234-5678")
            .claimantName("PAT LEE").primaryDefendantName("SAM JONES").build());
    }

    @Test
    void shouldScheduleTheEmailAgainstTheConfiguredTemplate() {
        configure(EnforcementEmail.WARRANT_APPROVED, TEMPLATE_ID);
        givenAnEmailableClaimant();

        boolean sent = sender().send(CASE_REFERENCE, EnforcementEmail.WARRANT_APPROVED,
                                     Map.of(EnforcementEmail.ENFORCEMENT_NUMBER, "W123456"));

        assertThat(sent).isTrue();
        ArgumentCaptor<EmailNotificationRequest> request = ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(notificationService).scheduleEmailNotification(request.capture(), eq(pcsCase), eq(claim),
                                                              eq(claimant));
        assertThat(request.getValue().getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(request.getValue().getEmailAddress()).isEqualTo("claimant@example.com");
        assertThat(request.getValue().getClaimType()).isEqualTo(NotificationClaimType.ENFORCEMENT);
        assertThat(request.getValue().getPersonalisation())
            .containsEntry("caseNumber", "1234-5678-1234-5678")
            .containsEntry("firstName", "Pat")
            .containsEntry("claimantName", "PAT LEE")
            .containsEntry("primaryDefendantName", "SAM JONES")
            .containsEntry(EnforcementEmail.ENFORCEMENT_NUMBER, "W123456");
    }

    /**
     * The state every environment is in today: no enforcement template exists in Notify, so every one of
     * these emails has to be skipped without touching Notify and without throwing into the case event.
     */
    @ParameterizedTest
    @EnumSource(EnforcementEmail.class)
    void shouldSendNothingUntilTheTemplateIsConfigured(EnforcementEmail email) {
        assertThat(sender().send(CASE_REFERENCE, email, Map.of())).isFalse();

        verifyNoInteractions(notificationService, pcsCaseService);
    }

    @Test
    void shouldSendNothingForNoEmail() {
        assertThat(sender().send(CASE_REFERENCE, null, Map.of())).isFalse();

        verifyNoInteractions(notificationService, pcsCaseService);
    }

    @Test
    void shouldSendNothingWhenTheClaimantHasNoEmailAddress() {
        configure(EnforcementEmail.EVICTION_SCHEDULED, TEMPLATE_ID);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(partyService.getPrimaryClaimantPartyEntity(pcsCase)).thenReturn(claimant);
        when(partyService.canSendEmailNotification(claimant, PartyRole.CLAIMANT)).thenReturn(false);

        assertThat(sender().send(CASE_REFERENCE, EnforcementEmail.EVICTION_SCHEDULED, Map.of())).isFalse();

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldSendNothingWhenTheCaseHasNoClaimant() {
        configure(EnforcementEmail.EVICTION_SCHEDULED, TEMPLATE_ID);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(partyService.getPrimaryClaimantPartyEntity(pcsCase)).thenReturn(null);

        assertThat(sender().send(CASE_REFERENCE, EnforcementEmail.EVICTION_SCHEDULED, Map.of())).isFalse();

        verifyNoInteractions(notificationService);
    }

    private void configure(EnforcementEmail email, String templateId) {
        templates.getTemplates().put(email.getTemplateKey(), templateId);
    }

    private EnforcementNotificationSender sender() {
        return new EnforcementNotificationSender(notificationService, personalisationFactory, partyService,
                                                  pcsCaseService, templates);
    }
}
