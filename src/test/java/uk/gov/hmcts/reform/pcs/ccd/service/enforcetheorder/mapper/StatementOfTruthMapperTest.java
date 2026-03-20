package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.StatementOfTruthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementOfTruthMapperTest {

    @Mock
    private StatementOfTruthService statementOfTruthService;
    @InjectMocks
    private StatementOfTruthMapper underTest;

    private EnforcementOrderEntity enforcementOrderEntity;
    private StatementOfTruthEntity statementOfTruthEntity;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = new EnforcementOrderEntity();
        statementOfTruthEntity = new StatementOfTruthEntity();
        statementOfTruthEntity.setCompletedBy(StatementOfTruthCompletedBy.CLAIMANT);
        statementOfTruthEntity.setFullName("John Doe");
        statementOfTruthEntity.setPositionHeld("Owner");
    }

    @Test
    void shouldMapStatementOfTruthForWarrantRest() {
        // Given
        StatementOfTruthDetails statementOfTruth = new StatementOfTruthDetails();
        statementOfTruth.setCompletedBy(StatementOfTruthCompletedBy.CLAIMANT);
        statementOfTruth.setFullNameClaimant("John Doe");
        statementOfTruth.setPositionClaimant("Owner");

        RawWarrantRestDetails rawWarrantRestDetails = RawWarrantRestDetails.builder()
                .statementOfTruthWarrantRest(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .rawWarrantRestDetails(rawWarrantRestDetails).build();
        when(statementOfTruthService.createStatementOfTruth(statementOfTruth))
                .thenReturn(statementOfTruthEntity);

        // When
        underTest.mapStatementOfTruthForWarrantRest(enforcementOrder, enforcementOrderEntity);
        StatementOfTruthEntity result = enforcementOrderEntity.getStatementOfTruth();

        // Then
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.CLAIMANT);
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getPositionHeld()).isEqualTo("Owner");
    }

    @Test
    void shouldHandleNullStatementOfTruthForWarrantRest() {
        RawWarrantRestDetails rawWarrantRestDetails = RawWarrantRestDetails.builder().build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .rawWarrantRestDetails(rawWarrantRestDetails).build();

        // When
        underTest.mapStatementOfTruthForWarrantRest(enforcementOrder, enforcementOrderEntity);

        // Then
        verify(statementOfTruthService, never()).createStatementOfTruth(any());
    }

    @Test
    void shouldHandleNullRawWarrantRest() {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();

        // When
        underTest.mapStatementOfTruthForWarrantRest(enforcementOrder, enforcementOrderEntity);

        // Then
        verify(statementOfTruthService, never()).createStatementOfTruth(any());
    }
}