package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.StatementOfTruthService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.RiskProfileService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.SelectedDefendantsMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantDetailsMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantStrategyTest {

    @Mock
    private RiskProfileService riskProfileService;
    @Mock
    private StatementOfTruthService statementOfTruthService;
    @Mock
    private SelectedDefendantsMapper selectedDefendantsMapper;
    @Mock
    private WarrantDetailsMapper warrantDetailsMapper;

    @InjectMocks
    private WarrantStrategy underTest;

    private EnforcementOrderEntity enforcementOrderEntity;
    private EnforcementOrder enforcementOrder;
    WarrantEntity warrantEntity;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrder = EnforcementOrder.builder()
                .warrantDetails(WarrantDetails.builder().build())
                .build();
        warrantEntity = new WarrantEntity();

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
                .thenReturn(warrantEntity);
    }

    @Test
    void shouldProcessWarrantDetails() {
        // Given
        List<SelectedDefendantEntity> selectedDefendantEntities =
                Collections.singletonList(mock(SelectedDefendantEntity.class));

        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
                .thenReturn(selectedDefendantEntities);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(warrantDetailsMapper).toEntity(enforcementOrder, enforcementOrderEntity);
        verify(riskProfileService).processRisk(enforcementOrder, enforcementOrderEntity);
        verify(statementOfTruthService).processStatementOfTruth(
                enforcementOrder.getWarrantDetails().getStatementOfTruth(), enforcementOrderEntity);
        verify(selectedDefendantsMapper).mapToEntities(enforcementOrderEntity);
        assertThat(enforcementOrderEntity.getSelectedDefendants()).containsExactlyElementsOf(selectedDefendantEntities);
    }

    @Test
    void shouldNotAddEmptySelectedDefendants() {
        // Given
        List<SelectedDefendantEntity> selectedDefendantEntities = Collections.emptyList();

        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
                .thenReturn(selectedDefendantEntities);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(selectedDefendantsMapper).mapToEntities(enforcementOrderEntity);
        assertThat(enforcementOrderEntity.getSelectedDefendants()).isEmpty();
    }
}
