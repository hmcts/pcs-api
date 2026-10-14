package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.SelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.StatementOfTruthService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.RiskProfileService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.SelectedDefendantsMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantDetailsMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory.VULNERABLE_CHILDREN;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildEnforcementTypes;

@ExtendWith(MockitoExtension.class)
class WarrantStrategyTest {

    @Mock
    private RiskProfileService riskProfileService;
    @Mock
    private StatementOfTruthService statementOfTruthService;
    @Mock
    private SelectedDefendantsMapper selectedDefendantsMapper;
    @Mock
    private SelectedDefendantRepository selectedDefendantRepository;

    @Mock
    private WarrantDetailsMapper warrantDetailsMapper;

    @InjectMocks
    private WarrantStrategy underTest;

    private EnforcementOrderEntity enforcementOrderEntity;
    private EnforcementOrder enforcementOrder;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = mock(EnforcementOrderEntity.class);

        WarrantDetails warrantDetails = WarrantDetails.builder().anyRiskToBailiff(YesNoNotSure.YES).build();

        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
            .vulnerableCategory(VULNERABLE_CHILDREN).vulnerableReasonText("Young children present").build();

        RawWarrantDetails rawWarrantDetails = RawWarrantDetails.builder()
            .vulnerablePeoplePresent(YesNoNotSure.YES).vulnerableAdultsChildren(vulnerableAdultsChildren).build();

        enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails)
            .rawWarrantDetails(rawWarrantDetails).build();
    }

    @Test
    void shouldProcessWarrantDetailsAndSaveToRepository() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();
        WarrantEntity savedWarrantEntity = new WarrantEntity();
        savedWarrantEntity.setId(1);

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());
        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(warrantDetailsMapper).toEntity(enforcementOrder, enforcementOrderEntity);
        verify(enforcementOrderEntity).setWarrantDetails(warrantEntity);
        verify(riskProfileService).processRisk(enforcementOrder, enforcementOrderEntity);
        verify(statementOfTruthService).processStatementOfTruth(
            enforcementOrder.getWarrantDetails().getStatementOfTruth(),
            enforcementOrderEntity
        );
    }

    @Test
    void shouldSetSavedWarrantDetailsOnEnforcementOrderEntity() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();
        WarrantEntity savedWarrantEntity = new WarrantEntity();
        Integer warrantId = 1;
        savedWarrantEntity.setId(warrantId);

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(enforcementOrderEntity).setWarrantDetails(warrantEntity);
    }

    @Test
    void shouldCallWarrantDetailsMapper() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(warrantDetailsMapper).toEntity(
            argThat(order -> order.equals(enforcementOrder)),
            argThat(entity -> entity.equals(enforcementOrderEntity))
        );
    }

    @Test
    void shouldProcessAllThreeStepsInCorrectOrder() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();
        List<SelectedDefendantEntity> defendants = List.of(new SelectedDefendantEntity());

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity)).thenReturn(defendants);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        assertThat(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .isEqualTo(warrantEntity);
    }

    @Test
    void shouldNotAddAnySelectedDefendantsWhenEvictEveryoneIsYes() {
        // Given
        final EnforcementOrder enfOrder = EnforcementOrder.builder()
                .chooseEnforcementType(buildEnforcementTypes(WARRANT))
            .warrantDetails(WarrantDetails.builder()
                                .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                                                               .correctNameAndAddress(VerticalYesNo.YES)
                                                               .build())
                                .peopleToEvict(PeopleToEvict.builder()
                                                   .evictEveryone(VerticalYesNo.YES)
                                                   .build())
                                .build())
            .rawWarrantDetails(RawWarrantDetails.builder().selectedDefendants(null).build())
            .build();

        // When
        underTest.process(enforcementOrderEntity, enfOrder);

        // Then
        verifyNoInteractions(selectedDefendantRepository);
    }
}
