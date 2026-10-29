package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.StatementOfTruthService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WritDetailsMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritStrategyTest {

    @Mock
    private WritDetailsMapper writDetailsMapper;
    @Mock
    StatementOfTruthService statementOfTruthService;

    @InjectMocks
    private WritStrategy underTest;

    private EnforcementOrder enforcementOrder;
    private EnforcementOrderEntity enforcementOrderEntity;
    private WritEntity writEntity;
    private StatementOfTruthDetailsEnforcement statementOfTruthDetailsEnforcement;

    @BeforeEach
    void setUp() {
        statementOfTruthDetailsEnforcement = new StatementOfTruthDetailsEnforcement();
        WritDetails writDetails = WritDetails.builder()
                .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                        .correctNameAndAddress(VerticalYesNo.YES)
                        .build())
                .showChangeNameAddressPage(YesOrNo.NO)
                .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.YES)
                .hceoDetails("Enforcement")
                .hasClaimTransferredToHighCourt(YesOrNo.YES)
                .statementOfTruth(statementOfTruthDetailsEnforcement)
                .build();
        enforcementOrder = EnforcementOrder.builder().writDetails(writDetails).build();
        enforcementOrderEntity = new EnforcementOrderEntity();
        writEntity = new WritEntity();

        // Given
        when(writDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity)).thenReturn(writEntity);
    }

    @Test
    void shouldProcessWritDetailsSuccessfully() {
        // Given & When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(writDetailsMapper).toEntity(enforcementOrder, enforcementOrderEntity);
        assertThat(enforcementOrderEntity.getWritDetails()).isSameAs(writEntity);
    }

    @Test
    void shouldProcessStatementOfTruth() {
        // Given & When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(writDetailsMapper).toEntity(enforcementOrder, enforcementOrderEntity);
        verify(statementOfTruthService).processStatementOfTruth(statementOfTruthDetailsEnforcement,
                enforcementOrderEntity);
    }
}
