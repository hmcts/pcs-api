package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlternativesToPossessionViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity mainClaimEntity;
    @Mock
    private PossessionAlternativesEntity possessionAlternativesEntity;

    private AlternativesToPossessionView underTest;

    @BeforeEach
    void setUp() {
        underTest = new AlternativesToPossessionView();
    }

    @Test
    void shouldNotSetAnythingIfNoMainClaim() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldNotSetAnythingIfNoPossessionsAlternative() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getPossessionAlternativesEntity()).thenReturn(null);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldSetSuspensionOfRightToBuyFields() {
        // Given
        SuspensionOfRightToBuyHousingAct expectedHousingAct = SuspensionOfRightToBuyHousingAct.SECTION_6A_2;
        String expectedReason = "suspension reason";

        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getPossessionAlternativesEntity()).thenReturn(possessionAlternativesEntity);
        when(possessionAlternativesEntity.getSuspensionOfRTB()).thenReturn(YesOrNo.YES);
        when(possessionAlternativesEntity.getSuspensionOfRTBHousingActSection()).thenReturn(expectedHousingAct);
        when(possessionAlternativesEntity.getSuspensionOfRTBReason()).thenReturn(expectedReason);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<SuspensionOfRightToBuy> suspensionOfRightToBuyCaptor
            = ArgumentCaptor.forClass(SuspensionOfRightToBuy.class);

        verify(pcsCase).setSuspensionOfRightToBuy(suspensionOfRightToBuyCaptor.capture());

        SuspensionOfRightToBuy suspensionOfRightToBuy = suspensionOfRightToBuyCaptor.getValue();
        assertThat(suspensionOfRightToBuy.getHousingAct()).isEqualTo(expectedHousingAct);
        assertThat(suspensionOfRightToBuy.getReason()).isEqualTo(expectedReason);
    }

    @Test
    void shouldSetDemotionOfTenancyFields() {
        // Given
        DemotionOfTenancyHousingAct expectedHousingAct = DemotionOfTenancyHousingAct.SECTION_82A_2;
        String expectedReason = "demotion reason";

        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getPossessionAlternativesEntity()).thenReturn(possessionAlternativesEntity);
        when(possessionAlternativesEntity.getDotRequested()).thenReturn(YesOrNo.YES);
        when(possessionAlternativesEntity.getDotHousingActSection()).thenReturn(expectedHousingAct);
        when(possessionAlternativesEntity.getDotReason()).thenReturn(expectedReason);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<DemotionOfTenancy> demotionOfTenancyCaptor
            = ArgumentCaptor.forClass(DemotionOfTenancy.class);

        verify(pcsCase).setDemotionOfTenancy(demotionOfTenancyCaptor.capture());

        DemotionOfTenancy demotionOfTenancy = demotionOfTenancyCaptor.getValue();
        assertThat(demotionOfTenancy.getHousingAct()).isEqualTo(expectedHousingAct);
        assertThat(demotionOfTenancy.getReason()).isEqualTo(expectedReason);
    }

}
