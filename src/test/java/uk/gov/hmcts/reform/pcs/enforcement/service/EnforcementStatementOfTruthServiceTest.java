package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.StatementOfTruthMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementStatementOfTruthServiceTest {

    @InjectMocks
    private EnforcementStatementOfTruthService enforcementStatementOfTruthService;

    @Mock
    private StatementOfTruthRepository statementOfTruthRepository;

    @Mock
    private StatementOfTruthMapper statementOfTruthMapper;

    @Test
    void shouldPersistStatementOfTruthForEnforcement() {
        // Given
        StatementOfTruthDetailsEnforcement statementOfTruthDetailsEnforcement
                = new StatementOfTruthDetailsEnforcement();
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        StatementOfTruthEntity statementOfTruthEntity = mock(StatementOfTruthEntity.class);
        StatementOfTruthEntity savedStatementOfTruthEntity = mock(StatementOfTruthEntity.class);

        when(statementOfTruthMapper.toEntity(statementOfTruthDetailsEnforcement))
                .thenReturn(statementOfTruthEntity);
        when(statementOfTruthRepository.save(statementOfTruthEntity))
                .thenReturn(savedStatementOfTruthEntity);

        // When
        enforcementStatementOfTruthService.processStatementOfTruth(statementOfTruthDetailsEnforcement,
                                                                   enforcementOrderEntity);

        // Then
        assertThat(enforcementOrderEntity.getStatementOfTruth()).isEqualTo(savedStatementOfTruthEntity);
        verify(statementOfTruthRepository).save(statementOfTruthEntity);
    }
}
