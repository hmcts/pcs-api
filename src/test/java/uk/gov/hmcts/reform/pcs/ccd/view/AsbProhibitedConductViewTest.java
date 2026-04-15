package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.NO;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.YES;

@ExtendWith(MockitoExtension.class)
class AsbProhibitedConductViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock(strictness = LENIENT)
    private ClaimEntity mainClaimEntity;
    @Mock
    private AsbProhibitedConductEntity asbProhibitedConductEntity;

    private AsbProhibitedConductView underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getAsbProhibitedConductEntity()).thenReturn(asbProhibitedConductEntity);

        underTest = new AsbProhibitedConductView();
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
    void shouldNotSetAnythingIfAsbProhibitedConductEntity() {
        // Given
        when(mainClaimEntity.getAsbProhibitedConductEntity()).thenReturn(null);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @ParameterizedTest
    @MethodSource("asbQuestionScenarios")
    void shouldSetAsbQuestionDetails(VerticalYesNo antisocialBehaviour,
                                     VerticalYesNo illegalPurposes,
                                     VerticalYesNo otherProhibitedConduct) {
        // Given
        when(asbProhibitedConductEntity.getAntisocialBehaviour()).thenReturn(antisocialBehaviour);
        when(asbProhibitedConductEntity.getAntisocialBehaviourDetails()).thenReturn("antisocial behaviour");
        when(asbProhibitedConductEntity.getIllegalPurposes()).thenReturn(illegalPurposes);
        when(asbProhibitedConductEntity.getIllegalPurposesDetails()).thenReturn("illegal purposes");
        when(asbProhibitedConductEntity.getOtherProhibitedConduct()).thenReturn(otherProhibitedConduct);
        when(asbProhibitedConductEntity.getOtherProhibitedConductDetails()).thenReturn("other prohibited conduct");

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<ASBQuestionsDetailsWales> asbQuestionDetailsCaptor
            = ArgumentCaptor.forClass(ASBQuestionsDetailsWales.class);

        verify(pcsCase).setAsbQuestionsWales(asbQuestionDetailsCaptor.capture());

        ASBQuestionsDetailsWales asbQuestionDetails = asbQuestionDetailsCaptor.getValue();

        assertThat(asbQuestionDetails.getAntisocialBehaviour()).isEqualTo(antisocialBehaviour);
        assertThat(asbQuestionDetails.getAntisocialBehaviourDetails()).isEqualTo("antisocial behaviour");
        assertThat(asbQuestionDetails.getIllegalPurposesUse()).isEqualTo(illegalPurposes);
        assertThat(asbQuestionDetails.getIllegalPurposesUseDetails()).isEqualTo("illegal purposes");
        assertThat(asbQuestionDetails.getOtherProhibitedConduct()).isEqualTo(otherProhibitedConduct);
        assertThat(asbQuestionDetails.getOtherProhibitedConductDetails()).isEqualTo("other prohibited conduct");
    }

    @ParameterizedTest
    @MethodSource("prohibitedConductScenarios")
    void shouldSetProhibitedConductFields(VerticalYesNo claimingStandardContract,
                                          VerticalYesNo periodicContractAgreed) {
        // Given
        when(asbProhibitedConductEntity.getClaimingStandardContract()).thenReturn(claimingStandardContract);
        when(asbProhibitedConductEntity.getClaimingStandardContractDetails()).thenReturn("claim details");
        when(asbProhibitedConductEntity.getPeriodicContractAgreed()).thenReturn(periodicContractAgreed);
        when(asbProhibitedConductEntity.getPeriodicContractDetails()).thenReturn("contract details");

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setProhibitedConductWalesClaim(claimingStandardContract);
        verify(pcsCase).setProhibitedConductWalesClaimDetails("claim details");

        ArgumentCaptor<PeriodicContractTermsWales> periodicContractTermsCaptor
            = ArgumentCaptor.forClass(PeriodicContractTermsWales.class);

        verify(pcsCase).setPeriodicContractTermsWales(periodicContractTermsCaptor.capture());
        PeriodicContractTermsWales periodicContractTerms = periodicContractTermsCaptor.getValue();

        assertThat(periodicContractTerms.getAgreedTermsOfPeriodicContract()).isEqualTo(periodicContractAgreed);
        assertThat(periodicContractTerms.getDetailsOfTerms()).isEqualTo("contract details");
    }

    private static Stream<Arguments> asbQuestionScenarios() {
        return Stream.of(
            // antisocial behaviour Y/N, illegal behaviour Y/N, other prohibited conduct Y/N
            argumentSet("All set to no", NO, NO, NO),
            argumentSet("Just antisocial", YES, NO, NO),
            argumentSet("Just illegal behaviour", NO, YES, NO),
            argumentSet("Just other prohibited conduct", NO, NO, YES),
            argumentSet("All set to yes", YES, YES, YES)
        );
    }

    private static Stream<Arguments> prohibitedConductScenarios() {
        return Stream.of(
            // claiming standard contract Y/N, period contract agreed Y/N
            argumentSet("Both set to no", NO, NO),
            argumentSet("Only standard contract claim", YES, NO),
            argumentSet("Both set to yes", YES, YES)
        );
    }

}
