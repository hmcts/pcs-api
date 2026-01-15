package uk.gov.hmcts.reform.pcs.ccd.service;

import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;

@ExtendWith(MockitoExtension.class)
class PossessionAlternativesServiceTest {

    @Mock(strictness = LENIENT)
    private PCSCase pcsCase;
    private SuspensionOfRightToBuy suspensionOfRightToBuy;
    private DemotionOfTenancy demotionOfTenancy;
    private SuspensionOfRightToBuyDemotionOfTenancy combinedAnswers;

    private PossessionAlternativesService underTest;

    @BeforeEach
    void setUp() {
        // Fill all fields to ensure that irrelevant data is not kept
        suspensionOfRightToBuy = Instancio.create(SuspensionOfRightToBuy.class);
        demotionOfTenancy = Instancio.create(DemotionOfTenancy.class);
        combinedAnswers = Instancio.create(SuspensionOfRightToBuyDemotionOfTenancy.class);

        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(suspensionOfRightToBuy);
        when(pcsCase.getDemotionOfTenancy()).thenReturn(demotionOfTenancy);
        when(pcsCase.getSuspensionOfRightToBuyDemotionOfTenancy()).thenReturn(combinedAnswers);

        underTest = new PossessionAlternativesService();
    }

    @Test
    void shouldReturnNullWhenAlternativesToPossessionIsNull() {
        // Given
        when(pcsCase.getAlternativesToPossession()).thenReturn(null);

        // When
        PossessionAlternativesEntity possessionAlternativesEntity
            = underTest.createPossessionAlternativesEntity(pcsCase);

        // Then
        assertThat(possessionAlternativesEntity).isNull();
    }

    @Test
    void shouldNotSetAnyOptionWhenNoneRequested() {
        // Given
        when(pcsCase.getAlternativesToPossession()).thenReturn(Set.of());

        // When
        PossessionAlternativesEntity possessionAlternativesEntity
            = underTest.createPossessionAlternativesEntity(pcsCase);

        // Then
        assertThat(possessionAlternativesEntity.getSortbRequested()).isEqualTo(YesOrNo.NO);
        assertThat(possessionAlternativesEntity.getSortbHousingActSection()).isNull();
        assertThat(possessionAlternativesEntity.getSortbReason()).isNull();

        assertThat(possessionAlternativesEntity.getDotRequested()).isEqualTo(YesOrNo.NO);
        assertThat(possessionAlternativesEntity.getDotHousingActSection()).isNull();
        assertThat(possessionAlternativesEntity.getDotReason()).isNull();
        assertThat(possessionAlternativesEntity.getDotStatementServed()).isNull();
        assertThat(possessionAlternativesEntity.getDotStatementDetails()).isNull();
    }

    @Test
    void shouldJustSetSuspensionOfRTBWhenNoDemotionOfTenancy() {
        // Given
        when(pcsCase.getAlternativesToPossession()).thenReturn(Set.of(SUSPENSION_OF_RIGHT_TO_BUY));
        suspensionOfRightToBuy.setHousingAct(SuspensionOfRightToBuyHousingAct.SECTION_82A_2);
        suspensionOfRightToBuy.setReason("suspension reason");

        // When
        PossessionAlternativesEntity possessionAlternativesEntity
            = underTest.createPossessionAlternativesEntity(pcsCase);

        // Then
        assertThat(possessionAlternativesEntity.getSortbRequested()).isEqualTo(YesOrNo.YES);
        assertThat(possessionAlternativesEntity.getSortbHousingActSection())
            .isEqualTo(SuspensionOfRightToBuyHousingAct.SECTION_82A_2);
        assertThat(possessionAlternativesEntity.getSortbReason()).isEqualTo("suspension reason");

        assertThat(possessionAlternativesEntity.getDotRequested()).isEqualTo(YesOrNo.NO);
        assertThat(possessionAlternativesEntity.getDotHousingActSection()).isNull();
        assertThat(possessionAlternativesEntity.getDotReason()).isNull();
        assertThat(possessionAlternativesEntity.getDotStatementServed()).isNull();
        assertThat(possessionAlternativesEntity.getDotStatementDetails()).isNull();
    }

    @Test
    void shouldJustSetDemotionOfTenancyWhenNoSuspensionOfRTB() {
        // Given
        when(pcsCase.getAlternativesToPossession()).thenReturn(Set.of(DEMOTION_OF_TENANCY));
        demotionOfTenancy.setHousingAct(DemotionOfTenancyHousingAct.SECTION_6A_2);
        demotionOfTenancy.setReason("demotion reason");

        // When
        PossessionAlternativesEntity possessionAlternativesEntity
            = underTest.createPossessionAlternativesEntity(pcsCase);

        // Then
        assertThat(possessionAlternativesEntity.getSortbRequested()).isEqualTo(YesOrNo.NO);
        assertThat(possessionAlternativesEntity.getSortbHousingActSection()).isNull();
        assertThat(possessionAlternativesEntity.getSortbReason()).isNull();

        assertThat(possessionAlternativesEntity.getDotRequested()).isEqualTo(YesOrNo.YES);
        assertThat(possessionAlternativesEntity.getDotHousingActSection())
            .isEqualTo(DemotionOfTenancyHousingAct.SECTION_6A_2);
        assertThat(possessionAlternativesEntity.getDotReason()).isEqualTo("demotion reason");
    }

    @Test
    void shouldSetSuspensionOfRTBAndDemotionOfTenancyWhenBothRequested() {
        // Given
        when(pcsCase.getAlternativesToPossession()).thenReturn(Set.of(SUSPENSION_OF_RIGHT_TO_BUY, DEMOTION_OF_TENANCY));
        combinedAnswers.setSuspensionOfRightToBuyActs(SuspensionOfRightToBuyHousingAct.SECTION_82A_2);
        combinedAnswers.setSuspensionOrderReason("suspension reason");
        combinedAnswers.setDemotionOfTenancyActs(DemotionOfTenancyHousingAct.SECTION_6A_2);
        combinedAnswers.setDemotionOrderReason("demotion reason");

        // When
        PossessionAlternativesEntity possessionAlternativesEntity
            = underTest.createPossessionAlternativesEntity(pcsCase);

        // Then
        assertThat(possessionAlternativesEntity.getSortbRequested()).isEqualTo(YesOrNo.YES);
        assertThat(possessionAlternativesEntity.getSortbHousingActSection())
            .isEqualTo(SuspensionOfRightToBuyHousingAct.SECTION_82A_2);
        assertThat(possessionAlternativesEntity.getSortbReason()).isEqualTo("suspension reason");

        assertThat(possessionAlternativesEntity.getDotRequested()).isEqualTo(YesOrNo.YES);
        assertThat(possessionAlternativesEntity.getDotHousingActSection())
            .isEqualTo(DemotionOfTenancyHousingAct.SECTION_6A_2);
        assertThat(possessionAlternativesEntity.getDotReason()).isEqualTo("demotion reason");
    }

    @Test
    void shouldSetStatementOfExpressForDemotionOfTenancyWhenServed() {
        // Given
        when(pcsCase.getAlternativesToPossession()).thenReturn(Set.of(DEMOTION_OF_TENANCY));
        demotionOfTenancy.setStatementOfExpressTermsServed(VerticalYesNo.YES);
        demotionOfTenancy.setStatementOfExpressTermsDetails("statement of express details");

        // When
        PossessionAlternativesEntity possessionAlternativesEntity
            = underTest.createPossessionAlternativesEntity(pcsCase);

        // Then
        assertThat(possessionAlternativesEntity.getDotStatementServed()).isEqualTo(YesOrNo.YES);
        assertThat(possessionAlternativesEntity.getDotStatementDetails()).isEqualTo("statement of express details");
    }

    @Test
    void shouldNotSetStatementOfExpressForDemotionOfTenancyWhenNotServed() {
        // Given
        when(pcsCase.getAlternativesToPossession()).thenReturn(Set.of(DEMOTION_OF_TENANCY));
        demotionOfTenancy.setStatementOfExpressTermsServed(VerticalYesNo.NO);

        // When
        PossessionAlternativesEntity possessionAlternativesEntity
            = underTest.createPossessionAlternativesEntity(pcsCase);

        // Then
        assertThat(possessionAlternativesEntity.getDotStatementServed()).isEqualTo(YesOrNo.NO);
        assertThat(possessionAlternativesEntity.getDotStatementDetails()).isNull();
    }

}
