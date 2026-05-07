package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AsbProhibitedConductServiceTest {

    private AsbProhibitedConductService underTest;

    @BeforeEach
    void setUp() {
        underTest = new AsbProhibitedConductService();
    }

    @Test
    void shouldReturnNullWhenNoAsbQuestionsOrProhibitedConductDetails() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        AsbProhibitedConductEntity asbProhibitedConductEntity = underTest.createAsbProhibitedConductEntity(pcsCase);

        // Then
        assertThat(asbProhibitedConductEntity).isNull();
    }

    @Test
    void shouldReturnNullWhenEmptyAsbQuestionsAndNoProhibitedConductDetails() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .asbQuestionsWales(ASBQuestionsDetailsWales.builder().build())
            .build();

        // When
        AsbProhibitedConductEntity asbProhibitedConductEntity = underTest.createAsbProhibitedConductEntity(pcsCase);

        // Then
        assertThat(asbProhibitedConductEntity).isNull();
    }

    @ParameterizedTest
    @MethodSource("asbQuestionScenarios")
    void shouldSetAntisocialDetails(ASBQuestionsDetailsWales asbQuestionDetails,
                                    AsbProhibitedConductEntity expectedEntity) {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .asbQuestionsWales(asbQuestionDetails)
            .build();

        // When
        AsbProhibitedConductEntity asbProhibitedConductEntity = underTest.createAsbProhibitedConductEntity(pcsCase);

        // Then
        assertThat(asbProhibitedConductEntity)
            .usingRecursiveComparison()
            .isEqualTo(expectedEntity);
    }

    @ParameterizedTest
    @MethodSource("prohibitedConductScenarios")
    void shouldSetProhibitedConductStandardContract(PCSCase pcsCase,
                                                    AsbProhibitedConductEntity expectedEntity) {
        // When
        AsbProhibitedConductEntity asbProhibitedConductEntity = underTest.createAsbProhibitedConductEntity(pcsCase);

        // Then
        assertThat(asbProhibitedConductEntity)
            .usingRecursiveComparison()
            .isEqualTo(expectedEntity);
    }

    private static Stream<Arguments> asbQuestionScenarios() {
        return Stream.of(
            Arguments.argumentSet(
                "All set to No",
                ASBQuestionsDetailsWales.builder() // Provided by claimant
                    .antisocialBehaviour(VerticalYesNo.NO)
                    .antisocialBehaviourDetails("Should be ignored")
                    .illegalPurposesUse(VerticalYesNo.NO)
                    .illegalPurposesUseDetails("Should be ignored")
                    .otherProhibitedConduct(VerticalYesNo.NO)
                    .otherProhibitedConductDetails("Should be ignored")
                    .build(),
                AsbProhibitedConductEntity.builder() // Expected entity
                    .antisocialBehaviour(VerticalYesNo.NO)
                    .illegalPurposes(VerticalYesNo.NO)
                    .otherProhibitedConduct(VerticalYesNo.NO)
                    .build()
            ),
            Arguments.argumentSet(
                "Antisocial behaviour",
                ASBQuestionsDetailsWales.builder()
                    .antisocialBehaviour(VerticalYesNo.YES)
                    .antisocialBehaviourDetails("Antisocial behaviour details")
                    .illegalPurposesUse(VerticalYesNo.NO)
                    .illegalPurposesUseDetails("Should be ignored")
                    .otherProhibitedConduct(VerticalYesNo.NO)
                    .otherProhibitedConductDetails("Should be ignored")
                    .build(),
                AsbProhibitedConductEntity.builder()
                    .antisocialBehaviour(VerticalYesNo.YES)
                    .antisocialBehaviourDetails("Antisocial behaviour details")
                    .illegalPurposes(VerticalYesNo.NO)
                    .otherProhibitedConduct(VerticalYesNo.NO)
                    .build()
            ),
            Arguments.argumentSet(
                "Illegal purposes",
                ASBQuestionsDetailsWales.builder()
                    .antisocialBehaviour(VerticalYesNo.NO)
                    .antisocialBehaviourDetails("Should be ignored")
                    .illegalPurposesUse(VerticalYesNo.YES)
                    .illegalPurposesUseDetails("Illegal purposes details")
                    .otherProhibitedConduct(VerticalYesNo.NO)
                    .otherProhibitedConductDetails("Should be ignored")
                    .build(),
                AsbProhibitedConductEntity.builder()
                    .antisocialBehaviour(VerticalYesNo.NO)
                    .illegalPurposes(VerticalYesNo.YES)
                    .illegalPurposesDetails("Illegal purposes details")
                    .otherProhibitedConduct(VerticalYesNo.NO)
                    .build()
            ),
            Arguments.argumentSet(
                "Other prohibited conduct",
                ASBQuestionsDetailsWales.builder()
                    .antisocialBehaviour(VerticalYesNo.NO)
                    .antisocialBehaviourDetails("Should be ignored")
                    .illegalPurposesUse(VerticalYesNo.NO)
                    .illegalPurposesUseDetails("Should be ignored")
                    .otherProhibitedConduct(VerticalYesNo.YES)
                    .otherProhibitedConductDetails("Other prohibited conduct details")
                    .build(),
                AsbProhibitedConductEntity.builder()
                    .antisocialBehaviour(VerticalYesNo.NO)
                    .illegalPurposes(VerticalYesNo.NO)
                    .otherProhibitedConduct(VerticalYesNo.YES)
                    .otherProhibitedConductDetails("Other prohibited conduct details")
                    .build()
            ),
            Arguments.argumentSet(
                "All set to Yes",
                ASBQuestionsDetailsWales.builder()
                    .antisocialBehaviour(VerticalYesNo.YES)
                    .antisocialBehaviourDetails("Antisocial behaviour details")
                    .illegalPurposesUse(VerticalYesNo.YES)
                    .illegalPurposesUseDetails("Illegal purposes details")
                    .otherProhibitedConduct(VerticalYesNo.YES)
                    .otherProhibitedConductDetails("Other prohibited conduct details")
                    .build(),
                AsbProhibitedConductEntity.builder()
                    .antisocialBehaviour(VerticalYesNo.YES)
                    .antisocialBehaviourDetails("Antisocial behaviour details")
                    .illegalPurposes(VerticalYesNo.YES)
                    .illegalPurposesDetails("Illegal purposes details")
                    .otherProhibitedConduct(VerticalYesNo.YES)
                    .otherProhibitedConductDetails("Other prohibited conduct details")
                    .build()
            )
        );
    }

    private static Stream<Arguments> prohibitedConductScenarios() {
        return Stream.of(
            Arguments.argumentSet(
                "No claim for prohibited conduct contract",
                PCSCase.builder() // Provided by claimant
                    .prohibitedConductWalesClaim(VerticalYesNo.NO)
                    .prohibitedConductWalesClaimDetails("Should be ignored")
                    .periodicContractTermsWales(PeriodicContractTermsWales.builder()
                                                    .agreedTermsOfPeriodicContract(VerticalYesNo.YES)
                                                    .detailsOfTerms("Should be ignored")
                                                    .build())
                    .build(),
                AsbProhibitedConductEntity.builder() // Expected entity
                    .claimingStandardContract(VerticalYesNo.NO)
                    .build()
            ),
            Arguments.argumentSet(
                "Claim for prohibited conduct contract",
                PCSCase.builder() // Provided by claimant
                    .prohibitedConductWalesClaim(VerticalYesNo.YES)
                    .prohibitedConductWalesClaimDetails("Claim details")
                    .periodicContractTermsWales(PeriodicContractTermsWales.builder()
                                                    .agreedTermsOfPeriodicContract(VerticalYesNo.NO)
                                                    .detailsOfTerms("Should be ignored")
                                                    .build())
                    .build(),
                AsbProhibitedConductEntity.builder() // Expected entity
                    .claimingStandardContract(VerticalYesNo.YES)
                    .claimingStandardContractDetails("Claim details")
                    .periodicContractAgreed(VerticalYesNo.NO)
                    .build()
            ),
            Arguments.argumentSet(
                "Claim for prohibited conduct contract with periodic contract agreed",
                PCSCase.builder() // Provided by claimant
                    .prohibitedConductWalesClaim(VerticalYesNo.YES)
                    .prohibitedConductWalesClaimDetails("Claim details")
                    .periodicContractTermsWales(PeriodicContractTermsWales.builder()
                                                    .agreedTermsOfPeriodicContract(VerticalYesNo.YES)
                                                    .detailsOfTerms("Periodic contract details")
                                                    .build())
                    .build(),
                AsbProhibitedConductEntity.builder() // Expected entity
                    .claimingStandardContract(VerticalYesNo.YES)
                    .claimingStandardContractDetails("Claim details")
                    .periodicContractAgreed(VerticalYesNo.YES)
                    .periodicContractDetails("Periodic contract details")
                    .build()
            )
        );
    }

}
