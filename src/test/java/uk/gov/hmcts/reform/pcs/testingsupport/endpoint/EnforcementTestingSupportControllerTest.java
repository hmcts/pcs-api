package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementTestingSupportControllerTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String SERVICE_AUTHORIZATION = "Bearer s2s";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnforcementOrderMediator enforcementOrderMediator;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    @InjectMocks
    private EnforcementTestingSupportController underTest;

    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        pcsCase = PcsCaseEntity.builder().build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
    }

    @Test
    void shouldMovePaidWarrantToAwaitingScheduling() {
        // Given
        EnforcementOrderEntity application = application(EnforcementState.CASE_PROGRESSION, true);
        when(enforcementOrderMediator.getEnforcementOrder(pcsCase)).thenReturn(Optional.of(application));

        // When
        ResponseEntity<String> response = underTest.moveWarrantToAwaitingScheduling(SERVICE_AUTHORIZATION,
                                                                                     CASE_REFERENCE);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(application.getId().toString());
        assertThat(application.getState()).isEqualTo(EnforcementState.AWAITING_SCHEDULING);
        verify(enforcementOrderRepository).save(application);
    }

    @Test
    void shouldReturnNotFoundWhenActiveWarrantIsNotPaid() {
        // Given
        EnforcementOrderEntity application = application(EnforcementState.PENDING_CASE_ISSUED, true);
        when(enforcementOrderMediator.getEnforcementOrder(pcsCase)).thenReturn(Optional.of(application));

        // When
        ResponseEntity<String> response = underTest.moveWarrantToAwaitingScheduling(SERVICE_AUTHORIZATION,
                                                                                     CASE_REFERENCE);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(application.getState()).isEqualTo(EnforcementState.PENDING_CASE_ISSUED);
        verifyNoInteractions(enforcementOrderRepository);
    }

    @Test
    void shouldReturnNotFoundWhenActiveEnforcementOrderIsNotAWarrant() {
        // Given
        EnforcementOrderEntity application = application(EnforcementState.CASE_PROGRESSION, false);
        when(enforcementOrderMediator.getEnforcementOrder(pcsCase)).thenReturn(Optional.of(application));

        // When
        ResponseEntity<String> response = underTest.moveWarrantToAwaitingScheduling(SERVICE_AUTHORIZATION,
                                                                                     CASE_REFERENCE);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(enforcementOrderRepository);
    }

    @Test
    void shouldReturnNotFoundWhenNoActiveEnforcementOrder() {
        // Given
        when(enforcementOrderMediator.getEnforcementOrder(pcsCase)).thenReturn(Optional.empty());

        // When
        ResponseEntity<String> response = underTest.moveWarrantToAwaitingScheduling(SERVICE_AUTHORIZATION,
                                                                                     CASE_REFERENCE);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(enforcementOrderRepository);
    }

    private static EnforcementOrderEntity application(EnforcementState state, boolean withWarrant) {
        EnforcementOrderEntity application = EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .state(state)
            .build();
        if (withWarrant) {
            application.setWarrantDetails(WarrantEntity.builder().build());
        }
        return application;
    }
}
