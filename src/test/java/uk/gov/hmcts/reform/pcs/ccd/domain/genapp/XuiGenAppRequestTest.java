package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.AgreementDefendantLegalRep;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class XuiGenAppRequestTest {

    @ParameterizedTest
    @MethodSource("sotAcceptedScenarios")
    void shouldDeriveSotAcceptedFlag(List<AgreementDefendantLegalRep> agreementDefendantLegalRep,
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

    private static Stream<Arguments> sotAcceptedScenarios() {
        return Stream.of(
            Arguments.arguments(null, null),
            Arguments.arguments(List.of(), null),
            Arguments.arguments(List.of(AgreementDefendantLegalRep.AGREED), VerticalYesNo.YES)
        );
    }

}
