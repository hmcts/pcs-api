package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import static org.assertj.core.api.Assertions.assertThat;

class PostcodeNotAssignedToCourtTest {

    private PostcodeNotAssignedToCourt underTest;

    @BeforeEach
    void setUp() {
        underTest = new PostcodeNotAssignedToCourt();
    }


    private static Stream<Arguments> contentScenarios() {
        return Stream.of(
            arguments("ALL_COUNTRIES", new String[] {
                "For claims in Scotland",
                "For claims in Northern Ireland",
                "local sheriff court",
                "Enforcement of Judgments Office (EJO)"
            }),
            arguments("ENGLAND", new String[] {
                "For rental or mortgage arrears claims",
                "For other types of claims",
                "N5 and the correct particulars of claim form"
            }),
            arguments("WALES", new String[] {
                "Use form N5 Wales",
                "correct particulars of claim form"
            })
        );
    }

    @ParameterizedTest
    @MethodSource("contentScenarios")
    void shouldGenerateAppropriateContent(String view, String[] expectedContents) {
        PCSCase caseData = PCSCase.builder()
            .showPostcodeNotAssignedToCourt(YesOrNo.YES)
            .postcodeNotAssignedView(view)
            .build();

        CaseDetails<PCSCase, State> details = new CaseDetails<>();
        details.setData(caseData);

        AboutToStartOrSubmitResponse<PCSCase, State> response = underTest.midEvent(details, null);

        assertThat(response.getErrors()).containsExactly("You're not eligible for this online service");
        String content = underTest.generateContent(caseData);
        
        for (String expectedContent : expectedContents) {
            assertThat(content).contains(expectedContent);
        }

        // Verify content that should NOT be present based on the view
        if ("ENGLAND".equals(view)) {
            assertThat(content).doesNotContain("For claims in Scotland")
                             .doesNotContain("For claims in Wales")
                             .doesNotContain("For claims in Northern Ireland");
        } else if ("WALES".equals(view)) {
            assertThat(content).doesNotContain("For claims in Scotland")
                             .doesNotContain("For claims in England")
                             .doesNotContain("For claims in Northern Ireland")
                             .doesNotContain("PCOL service");
        }
    }

    @ParameterizedTest
    @MethodSource("linkScenarios")
    void shouldContainRequiredLinks(String view, String expectedLink) {
        PCSCase caseData = PCSCase.builder()
            .showPostcodeNotAssignedToCourt(YesOrNo.YES)
            .postcodeNotAssignedView(view)
            .build();

        String content = underTest.generateContent(caseData);
        assertThat(content).contains(expectedLink);
    }

    private static Stream<Arguments> linkScenarios() {
        return Stream.of(
            arguments("ALL_COUNTRIES", "https://www.gov.uk/possession-claim-online-recover-property"),
            arguments("ALL_COUNTRIES", "https://www.scotcourts.gov.uk/home"),
            arguments("ALL_COUNTRIES", "https://www.nidirect.gov.uk/articles/enforcement-civil-court-orders-northern-ireland"),
            arguments("ENGLAND", "https://www.gov.uk/possession-claim-online-recover-property"),
            arguments("WALES", "https://www.gov.uk/government/collections/property-possession-forms")
        );
    }
}
