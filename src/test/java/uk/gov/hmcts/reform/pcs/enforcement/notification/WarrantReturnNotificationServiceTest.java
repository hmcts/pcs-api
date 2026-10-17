package uk.gov.hmcts.reform.pcs.enforcement.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationPersonalisationFactory;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantReturnNotificationServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final String TEMPLATE_ID = "warrant-return-template";

    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationPersonalisationFactory personalisationFactory;
    @Mock
    private PartyService partyService;
    @Mock
    private PcsCaseService pcsCaseService;

    @Test
    void shouldEmailTheClaimantTheBodyForTheReturnName() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        PartyEntity claimant = PartyEntity.builder().emailAddress("claimant@example.com").build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(partyService.getPrimaryClaimantPartyEntity(pcsCase)).thenReturn(claimant);
        when(partyService.canSendEmailNotification(claimant, PartyRole.CLAIMANT)).thenReturn(true);
        when(personalisationFactory.forClaimant(claim)).thenReturn(BasePersonalisation.builder()
            .firstName("Pat").lastName("Lee").caseNumber("1234-5678-1234-5678")
            .claimantName("PAT LEE").primaryDefendantName("SAM JONES").build());

        boolean sent = service(TEMPLATE_ID).notifyClaimant(
            CASE_REFERENCE, WarrantReturnName.OTHER_INTERIM_RETURN, "Occupant presented a court order");

        assertThat(sent).isTrue();
        ArgumentCaptor<EmailNotificationRequest> request = ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(notificationService).scheduleEmailNotification(request.capture(), eq(pcsCase), eq(claim),
                                                              eq(claimant));
        assertThat(request.getValue().getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(request.getValue().getEmailAddress()).isEqualTo("claimant@example.com");
        assertThat(request.getValue().getClaimType()).isEqualTo(NotificationClaimType.POSSESSION_CLAIM);
        assertThat(request.getValue().getPersonalisation())
            .containsEntry("caseNumber", "1234-5678-1234-5678")
            .containsEntry("firstName", "Pat")
            .containsEntry("returnName", "Other interim return")
            .hasEntrySatisfying("warrantReturnBody", body -> assertThat((String) body)
                .contains("The reason provided by the bailiff is: Occupant presented a court order"));
    }

    @ParameterizedTest
    @EnumSource(value = WarrantReturnName.class, names = {
        "EVICTION_SUCCESSFUL", "WARRANT_SUSPENDED_BY_ORDER", "POSSESSION_ORDER_SET_ASIDE"})
    void shouldNotEmailForAReturnNameThatDoesNotNotify(WarrantReturnName returnName) {
        assertThat(service(TEMPLATE_ID).notifyClaimant(CASE_REFERENCE, returnName, "details")).isFalse();

        verifyNoInteractions(notificationService, pcsCaseService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldNotEmailUntilTheTemplateIsConfigured(String templateId) {
        assertThat(service(templateId).notifyClaimant(CASE_REFERENCE, WarrantReturnName.WARRANT_WITHDRAWN, "x"))
            .isFalse();

        verifyNoInteractions(notificationService, pcsCaseService);
    }

    @Test
    void shouldNotEmailAClaimantWithoutAnEmailAddress() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.addClaim(new ClaimEntity());
        PartyEntity claimant = PartyEntity.builder().build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(partyService.getPrimaryClaimantPartyEntity(pcsCase)).thenReturn(claimant);
        when(partyService.canSendEmailNotification(claimant, PartyRole.CLAIMANT)).thenReturn(false);

        assertThat(service(TEMPLATE_ID).notifyClaimant(CASE_REFERENCE, WarrantReturnName.WARRANT_WITHDRAWN, "x"))
            .isFalse();

        verify(notificationService, org.mockito.Mockito.never()).scheduleEmailNotification(any(), any(), any(), any());
    }

    private WarrantReturnNotificationService service(String templateId) {
        return new WarrantReturnNotificationService(notificationService, personalisationFactory, partyService,
                                                    pcsCaseService, templateId);
    }
}
