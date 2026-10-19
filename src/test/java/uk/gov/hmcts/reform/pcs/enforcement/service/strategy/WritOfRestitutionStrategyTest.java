package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WritOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WritOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatementOfTruthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WritOfRestitutionStrategyTest {

    @InjectMocks
    private WritOfRestitutionStrategy underTest;
    @Mock
    private WritOfRestitutionRepository writOfRestitutionRepository;
    @Mock
    private EnforcementStatementOfTruthService enforcementStatementOfTruthService;

    @Test
    void shouldProcess() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        EnforcementOrder enforcementOrder = new EnforcementOrder();

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);
        ArgumentCaptor<WritOfRestitutionEntity> captor =
            ArgumentCaptor.forClass(WritOfRestitutionEntity.class);

        // Then
        verify(writOfRestitutionRepository).save(captor.capture());
        WritOfRestitutionEntity saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getEnforcementOrder()).isSameAs(enforcementOrderEntity);
        assertThat(saved.getLanguageUsed()).isNull();
        verify(enforcementStatementOfTruthService).processStatementOfTruth(null, enforcementOrderEntity);
    }

    /**
     * The journey answers the writ journey's language and statement of truth pages, so both arrive on the writ
     * details of a writ of restitution application.
     */
    @Test
    void shouldStoreTheLanguageAndStatementOfTruthGivenOnTheWritPages() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .writDetails(WritDetails.builder()
                             .languageUsed(LanguageUsed.WELSH)
                             .statementOfTruth(statementOfTruth)
                             .build())
            .build();

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        ArgumentCaptor<WritOfRestitutionEntity> captor =
            ArgumentCaptor.forClass(WritOfRestitutionEntity.class);
        verify(writOfRestitutionRepository).save(captor.capture());
        assertThat(captor.getValue().getLanguageUsed()).isEqualTo(LanguageUsed.WELSH);
        verify(enforcementStatementOfTruthService).processStatementOfTruth(statementOfTruth, enforcementOrderEntity);
    }

}
