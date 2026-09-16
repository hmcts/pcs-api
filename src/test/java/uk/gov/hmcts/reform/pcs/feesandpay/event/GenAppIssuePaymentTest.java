package uk.gov.hmcts.reform.pcs.feesandpay.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.event.genapp.GenAppWaTaskService;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.exception.GenAppNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CTSC_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.LEADERSHIP_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.WLU_TEAM_LEADER;

@ExtendWith(MockitoExtension.class)
class GenAppIssuePaymentTest extends BaseEventTest {

    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private GenAppDocumentGenerator genAppDocumentGenerator;
    @Mock
    private GenAppWaTaskService genAppWaTaskService;

    @InjectMocks
    private GenAppIssuePayment paymentEvent;

    @BeforeEach
    void setUp() {
        setEventUnderTest(paymentEvent);
    }

    @Test
    void shouldIssueGenAppWhenPendingGenAppIssued() {
        UUID genAppId = UUID.randomUUID();
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getState()).thenReturn(GenAppState.PENDING_GEN_APP_ISSUED);
        when(genAppRepository.findById(genAppId)).thenReturn(Optional.of(genAppEntity));

        callSubmitHandler(PCSCase.builder().pendingGenAppPaymentId(genAppId.toString()).build());

        verify(genAppEntity).setState(GenAppState.GEN_APP_ISSUED);
        verify(genAppDocumentGenerator).createSubmissionDocument(TEST_CASE_REFERENCE, genAppEntity);
        verify(genAppWaTaskService).createReviewGenAppTask(TEST_CASE_REFERENCE, genAppEntity);
        verify(genAppWaTaskService).createTranslationTaskForGenApp(genAppEntity);
    }

    @Test
    void shouldNotIssueGenAppWhenAlreadyIssued() {
        UUID genAppId = UUID.randomUUID();
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getState()).thenReturn(GenAppState.GEN_APP_ISSUED);
        when(genAppRepository.findById(genAppId)).thenReturn(Optional.of(genAppEntity));

        callSubmitHandler(PCSCase.builder().pendingGenAppPaymentId(genAppId.toString()).build());

        verify(genAppEntity, never()).setState(GenAppState.GEN_APP_ISSUED);
        verifyNoInteractions(genAppDocumentGenerator, genAppWaTaskService);
    }

    @Test
    void shouldThrowExceptionForUnknownGenAppEntityId() {
        UUID unknownGenAppId = UUID.randomUUID();
        when(genAppRepository.findById(unknownGenAppId)).thenReturn(Optional.empty());

        Throwable throwable = catchThrowable(() -> callSubmitHandler(
            PCSCase.builder().pendingGenAppPaymentId(unknownGenAppId.toString()).build()));

        assertThat(throwable).isInstanceOf(GenAppNotFoundException.class);
    }

    @Test
    void shouldGrantReadAccessToInternalServiceRequestRoles() {
        assertThat(configuredEvent.getGrants().get(CTSC_ADMIN)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(CTSC_TEAM_LEADER)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(HEARING_CENTRE_ADMIN)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(HEARING_CENTRE_TEAM_LEADER)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(JUDGE)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(LEADERSHIP_JUDGE)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(WLU_ADMIN)).contains(Permission.R);
        assertThat(configuredEvent.getGrants().get(WLU_TEAM_LEADER)).contains(Permission.R);
    }

}
