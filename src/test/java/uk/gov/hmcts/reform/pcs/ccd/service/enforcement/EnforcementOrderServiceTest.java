package uk.gov.hmcts.reform.pcs.ccd.service.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcement.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.EnforcementOrderNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private EnforcementOrderService enforcementOrderService;

    @Captor
    private ArgumentCaptor<EnforcementOrderEntity> enforcementOrderEntityCaptor;

    private final UUID enforcementOrderId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

    private final UUID claimId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");

    private final UUID pcsCaseId = UUID.fromString("7b9e0f1a-2b3c-4d5e-6f7a-8b9c0d1e2f3a");

    private static final long CASE_REFERENCE = 1234L;

    @BeforeEach
    void setUp() {
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        lenient().when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
    }

    @Test
    void shouldThrowExceptionWhenNoEnforcementOrder() {
        // Given
        when(enforcementOrderRepository.findById(enforcementOrderId)).thenReturn(Optional.empty());

        // When &
        // Then
        assertThatThrownBy(() -> enforcementOrderService.loadEnforcementOrder(enforcementOrderId))
                .isInstanceOf(EnforcementOrderNotFoundException.class)
                .hasMessageContaining("No enforcement order found for case reference");
    }

    @Test
    void shouldReturnEnforcementOrderWhenFound() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final ClaimEntity claimEntity = pcsCaseEntity.getClaims().iterator().next();
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();
        final EnforcementOrderEntity enforcementOrderEntity =
                EnforcementDataUtil.buildEnforcementOrderEntity(enforcementOrderId, claimEntity, enforcementOrder);
        when(enforcementOrderRepository.findById(enforcementOrderId)).thenReturn(Optional.of(enforcementOrderEntity));

        // When
        EnforcementOrderEntity retrievedEnforcementOrderEntity =
                enforcementOrderService.loadEnforcementOrder(enforcementOrderId);

        // Then
        assertThat(retrievedEnforcementOrderEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
        assertThat(retrievedEnforcementOrderEntity.getId()).isEqualTo(enforcementOrderId);
        assertThat(retrievedEnforcementOrderEntity.getClaim().getId()).isEqualTo(claimId);
    }

    @Test
    void shouldSaveNewSubmittedEnforcementData() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enforcementOrderRepository).save(enforcementOrderEntityCaptor.capture());
        EnforcementOrderEntity savedEntity = enforcementOrderEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
    }

    @Test
    void shouldThrowExceptionWhenNoClaimFound() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        pcsCaseEntity.setClaims(null);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When &
        // Then
        assertThatThrownBy(() ->
                enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder))
                .isInstanceOf(ClaimNotFoundException.class)
                .hasMessageContaining("No claim found for case reference");
        verifyNoInteractions(draftCaseDataService);
    }

    @Test
    void shouldDeleteDraftEnforcementDataWhenSubmittedSuccessfully() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, EventId.enforceTheOrder, USER_ID);
    }
}