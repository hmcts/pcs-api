package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WritEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WritRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatementOfTruthService;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.WritDetailsMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritStrategyTest {

    @Mock
    private WritDetailsMapper writDetailsMapper;
    @Mock
    private WritRepository writRepository;
    @Mock
    private EnforcementStatementOfTruthService enforcementStatementOfTruthService;

    @InjectMocks
    private WritStrategy underTest;

    @Captor
    private ArgumentCaptor<WritEntity> writEntityCaptor;

    private EnforcementOrderEntity enforcementOrderEntity;
    private EnforcementOrder enforcementOrder;
    private WritDetails writDetails;
    private WritEntity writEntity;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = mock(EnforcementOrderEntity.class);

        writDetails = WritDetails.builder()
            .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                                           .correctNameAndAddress(VerticalYesNo.YES)
                                           .build())
            .showChangeNameAddressPage(YesOrNo.NO)
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.YES)
            .hceoDetails("Enforcement")
            .hasClaimTransferredToHighCourt(YesOrNo.YES)
            .build();

        enforcementOrder = EnforcementOrder.builder().writDetails(writDetails).build();

        writEntity = new WritEntity();
    }

    @Test
    void shouldProcessWritDetailsSuccessfully() {
        // Given
        when(writDetailsMapper.toEntity(writDetails)).thenReturn(writEntity);
        when(writRepository.save(any(WritEntity.class))).thenReturn(writEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(writDetailsMapper).toEntity(writDetails);
        verify(writRepository).save(writEntityCaptor.capture());

        WritEntity savedEntity = writEntityCaptor.getValue();
        assertThat(savedEntity).isSameAs(writEntity);
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
    }

    @Test
    void shouldPersistTheStatementOfTruthTheClaimantSigned() {
        // Given
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        writDetails.setStatementOfTruth(statementOfTruth);
        when(writDetailsMapper.toEntity(writDetails)).thenReturn(writEntity);
        when(writRepository.save(any(WritEntity.class))).thenReturn(writEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(enforcementStatementOfTruthService).processStatementOfTruth(statementOfTruth, enforcementOrderEntity);
    }

    @Test
    void shouldPersistNoStatementOfTruthWhenTheWritHasNoDetails() {
        // Given
        enforcementOrder.setWritDetails(null);
        when(writDetailsMapper.toEntity(null)).thenReturn(writEntity);
        when(writRepository.save(any(WritEntity.class))).thenReturn(writEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(enforcementStatementOfTruthService).processStatementOfTruth(null, enforcementOrderEntity);
    }

    @Test
    void shouldSetEnforcementOrderOnWritEntity() {
        // Given
        when(writDetailsMapper.toEntity(writDetails)).thenReturn(writEntity);
        when(writRepository.save(any(WritEntity.class))).thenReturn(writEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(writRepository).save(writEntityCaptor.capture());
        WritEntity savedEntity = writEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
    }

    @Test
    void shouldHandleEmptyWritDetails() {
        // Given
        WritDetails emptyWritDetails = WritDetails.builder().build();
        enforcementOrder.setWritDetails(emptyWritDetails);
        WritEntity emptyWritEntity = new WritEntity();

        when(writDetailsMapper.toEntity(emptyWritDetails)).thenReturn(emptyWritEntity);
        when(writRepository.save(any(WritEntity.class))).thenReturn(emptyWritEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(writDetailsMapper).toEntity(emptyWritDetails);
        verify(writRepository).save(writEntityCaptor.capture());

        WritEntity savedEntity = writEntityCaptor.getValue();
        assertThat(savedEntity).isSameAs(emptyWritEntity);
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
    }

    @Test
    void shouldMapAndSaveWritDetailsInCorrectOrder() {
        // Given
        when(writDetailsMapper.toEntity(writDetails)).thenReturn(writEntity);
        when(writRepository.save(any(WritEntity.class))).thenReturn(writEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        var inOrder = org.mockito.Mockito.inOrder(writDetailsMapper, writRepository);
        inOrder.verify(writDetailsMapper).toEntity(writDetails);
        inOrder.verify(writRepository).save(any(WritEntity.class));
    }

    @Test
    void shouldHandleWritDetailsWithMinimalData() {
        // Given
        WritDetails minimalWritDetails = WritDetails.builder()
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.NO)
            .build();
        enforcementOrder.setWritDetails(minimalWritDetails);
        WritEntity minimalWritEntity = new WritEntity();

        when(writDetailsMapper.toEntity(minimalWritDetails)).thenReturn(minimalWritEntity);
        when(writRepository.save(any(WritEntity.class))).thenReturn(minimalWritEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(writDetailsMapper).toEntity(minimalWritDetails);
        verify(writRepository).save(writEntityCaptor.capture());

        WritEntity savedEntity = writEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
    }

    @Test
    void shouldHandleWritDetailsWithAllFieldsPopulated() {
        // Given
        WritDetails fullWritDetails = WritDetails.builder()
            .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                                           .correctNameAndAddress(VerticalYesNo.YES)
                                           .build())
            .showChangeNameAddressPage(YesOrNo.NO)
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.YES)
            .hceoDetails("Complete HCEO Details")
            .hasClaimTransferredToHighCourt(YesOrNo.YES)
            .build();
        enforcementOrder.setWritDetails(fullWritDetails);
        WritEntity fullWritEntity = new WritEntity();

        when(writDetailsMapper.toEntity(fullWritDetails)).thenReturn(fullWritEntity);
        when(writRepository.save(any(WritEntity.class))).thenReturn(fullWritEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(writDetailsMapper).toEntity(fullWritDetails);
        verify(writRepository).save(writEntityCaptor.capture());

        WritEntity savedEntity = writEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
    }

    @Test
    void shouldCallMapperBeforeSaving() {
        // Given
        when(writDetailsMapper.toEntity(writDetails)).thenReturn(writEntity);
        when(writRepository.save(any(WritEntity.class))).thenReturn(writEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(writDetailsMapper).toEntity(writDetails);
        verify(writRepository).save(writEntity);
    }
}
