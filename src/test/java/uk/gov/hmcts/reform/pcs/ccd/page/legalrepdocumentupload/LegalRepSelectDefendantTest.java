package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("LegalRepSelectDefendant tests")
class LegalRepSelectDefendantTest extends BasePageTest {

    private static final UUID FIRST_ID = UUID.randomUUID();
    private static final UUID SECOND_ID = UUID.randomUUID();
    private static final DynamicListElement FIRST_PARTY = DynamicListElement.builder()
        .code(FIRST_ID).label("Sam Vimes").build();
    private static final DynamicListElement SECOND_PARTY = DynamicListElement.builder()
        .code(SECOND_ID).label("Granny Weatherwax").build();

    @BeforeEach
    void setUp() {
        setPageUnderTest(new LegalRepSelectDefendant());
    }

    @Nested
    @DisplayName("Mid-event callback tests")
    class MidEventCallbackTests {

        private static Stream<Arguments> partySelectionProvider() {
            return Stream.of(
                Arguments.of(FIRST_PARTY, String.valueOf(FIRST_ID), "Sam Vimes"),
                Arguments.of(SECOND_PARTY, String.valueOf(SECOND_ID), "Granny Weatherwax")
            );
        }

        @ParameterizedTest
        @DisplayName("Selected Party populates case data")
        @MethodSource("partySelectionProvider")
        void populatesCaseData(DynamicListElement selectedParty, String id, String name) {
            // Given
            PCSCase caseData = PCSCase.builder()
                .representedPartyNames(DynamicList.builder()
                                           .listItems(List.of(FIRST_PARTY, SECOND_PARTY))
                                           .value(DynamicListElement.EMPTY)
                                           .build())
                .build();


            caseData.getRepresentedPartyNames().setValue(selectedParty);
            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getCurrentRepresentedPartyId()).isEqualTo(id);
            assertThat(response.getData().getCurrentRepresentedPartyName()).isEqualTo(name);
        }
    }
}

