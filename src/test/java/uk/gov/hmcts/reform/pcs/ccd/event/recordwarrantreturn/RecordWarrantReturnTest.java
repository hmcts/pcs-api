package uk.gov.hmcts.reform.pcs.ccd.event.recordwarrantreturn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.WarrantReturn;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.event.bailiff.recordwarrantreturn.RecordWarrantReturn;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.WarrantReturnService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RecordWarrantReturnTest extends BaseEventTest {

    @Mock
    WarrantReturnService warrantReturnService;

    @Mock
    PcsCaseService pcsCaseService;

    @Mock
    EnforcementOrderMediator enforcementOrderMediator;

    @InjectMocks
    RecordWarrantReturn underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }

    @Test
    void shouldCallWarrantReturnService() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        WarrantEntity warrantEntity = WarrantEntity.builder().build();
        EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder()
            .warrantDetails(warrantEntity)
            .build();
        when(enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity))
            .thenReturn(Optional.of(enforcementOrderEntity));

        WarrantReturn warrantReturn = WarrantReturn.builder().build();
        PCSCase pcsCase = PCSCase.builder().warrantReturn(warrantReturn).build();

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        verify(warrantReturnService).addWarrantReturn(warrantEntity, warrantReturn);
    }

    @Test
    void shouldReturnErrorWhenNoEnforcementOrderIsPresent() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        WarrantReturn warrantReturn = WarrantReturn.builder().build();
        PCSCase pcsCase = PCSCase.builder().warrantReturn(warrantReturn).build();

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().getFirst()).isEqualTo("There is no warrant on this case");
        verifyNoInteractions(warrantReturnService);
    }

    @Test
    void shouldReturnErrorWhenNoWarrantIsPresent() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder().build();
        when(enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity))
            .thenReturn(Optional.of(enforcementOrderEntity));

        WarrantReturn warrantReturn = WarrantReturn.builder().build();
        PCSCase pcsCase = PCSCase.builder().warrantReturn(warrantReturn).build();

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().getFirst()).isEqualTo("There is no warrant on this case");
        verifyNoInteractions(warrantReturnService);
    }
}
