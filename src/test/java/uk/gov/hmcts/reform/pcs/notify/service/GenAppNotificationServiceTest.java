package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GenAppNotificationServiceTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private GenAppNotificationService underTest;

    private final UUID genAppId = UUID.randomUUID();

    @Test
    void sendsApplicantAndRespondentNotificationsWhenWithNotice() {
        given(featureToggleService.isEnabled(FeatureFlag.GEN_APP_NOTIFICATIONS)).willReturn(true);
        GenAppEntity genApp = mock(VerticalYesNo.NO);
        given(genAppRepository.findById(genAppId)).willReturn(Optional.of(genApp));

        underTest.sendGenAppIssuedNotifications(genAppId);

        verify(notificationService).sendGenAppApplicantNotification(genApp);
        verify(notificationService).sendGenAppRespondentNotifications(genApp);
    }

    @Test
    void skipsRespondentNotificationsWhenWithoutNotice() {
        given(featureToggleService.isEnabled(FeatureFlag.GEN_APP_NOTIFICATIONS)).willReturn(true);
        GenAppEntity genApp = mock(VerticalYesNo.YES);
        given(genAppRepository.findById(genAppId)).willReturn(Optional.of(genApp));

        underTest.sendGenAppIssuedNotifications(genAppId);

        verify(notificationService).sendGenAppApplicantNotification(genApp);
        verify(notificationService, never()).sendGenAppRespondentNotifications(genApp);
    }

    @Test
    void doesNothingWhenFeatureFlagDisabled() {
        given(featureToggleService.isEnabled(FeatureFlag.GEN_APP_NOTIFICATIONS)).willReturn(false);

        underTest.sendGenAppIssuedNotifications(genAppId);

        verifyNoInteractions(notificationService, genAppRepository);
    }

    private GenAppEntity mock(VerticalYesNo withoutNotice) {
        GenAppEntity genApp = org.mockito.Mockito.mock(GenAppEntity.class);
        given(genApp.getWithoutNotice()).willReturn(withoutNotice);
        return genApp;
    }
}
