package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.AgreementClaimantLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.AgreementDefendantLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class XuiGenAppRequestTest {

    @ParameterizedTest
    @MethodSource("claimantSoTScenarios")
    void shouldDeriveSotAcceptedFlagForClaimant(List<StatementOfTruthAgreementClaimant> agreementClaimant,
                                                   VerticalYesNo expectedSotAccepted) {
        // Given
        XuiGenAppRequest xuiGenAppRequest = XuiGenAppRequest.builder()
            .agreementClaimant(agreementClaimant)
            .build();

        // When
        VerticalYesNo actualSotAccepted = xuiGenAppRequest.getSotAccepted();

        // Then
        assertThat(actualSotAccepted).isEqualTo(expectedSotAccepted);
    }

    private static Stream<Arguments> claimantSoTScenarios() {
        return Stream.of(
            Arguments.arguments(null, null),
            Arguments.arguments(List.of(), null),
            Arguments.arguments(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE), VerticalYesNo.YES)
        );
    }

    @ParameterizedTest
    @MethodSource("claimantLegalRepSoTScenarios")
    void shouldDeriveSotAcceptedFlagForClaimantLR(List<AgreementClaimantLegalRep> agreementClaimantLegalRep,
                                                  VerticalYesNo expectedSotAccepted) {
        // Given
        XuiGenAppRequest xuiGenAppRequest = XuiGenAppRequest.builder()
            .agreementClaimantLegalRep(agreementClaimantLegalRep)
            .build();

        // When
        VerticalYesNo actualSotAccepted = xuiGenAppRequest.getSotAccepted();

        // Then
        assertThat(actualSotAccepted).isEqualTo(expectedSotAccepted);
    }

    private static Stream<Arguments> claimantLegalRepSoTScenarios() {
        return Stream.of(
            Arguments.arguments(null, null),
            Arguments.arguments(List.of(), null),
            Arguments.arguments(List.of(AgreementClaimantLegalRep.AGREED), VerticalYesNo.YES)
        );
    }

    @ParameterizedTest
    @MethodSource("defendantLegalRepSoTScenarios")
    void shouldDeriveSotAcceptedFlagForDefendantLR(List<AgreementDefendantLegalRep> agreementDefendantLegalRep,
                                                   VerticalYesNo expectedSotAccepted) {
        // Given
        XuiGenAppRequest xuiGenAppRequest = XuiGenAppRequest.builder()
            .agreementDefendantLegalRep(agreementDefendantLegalRep)
            .build();

        // When
        VerticalYesNo actualSotAccepted = xuiGenAppRequest.getSotAccepted();

        // Then
        assertThat(actualSotAccepted).isEqualTo(expectedSotAccepted);
    }

    private static Stream<Arguments> defendantLegalRepSoTScenarios() {
        return Stream.of(
            Arguments.arguments(null, null),
            Arguments.arguments(List.of(), null),
            Arguments.arguments(List.of(AgreementDefendantLegalRep.AGREED), VerticalYesNo.YES)
        );
    }

}
