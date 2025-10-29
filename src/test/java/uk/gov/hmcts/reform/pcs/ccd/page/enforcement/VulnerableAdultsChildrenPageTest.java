package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableAdultsChildren.VULNERABLE_REASON_TEXT_LIMIT;

class VulnerableAdultsChildrenPageTest extends BasePageTest {

    private VulnerableAdultsChildrenPage page;

    @BeforeEach
    void setUp() {
        page = new VulnerableAdultsChildrenPage();
        setPageUnderTest(page);
    }

    @ParameterizedTest
    @MethodSource("characterLimitScenarios")
    void shouldValidateCharacterLimit(
            YesNoNotSure vulnerablePeopleYesNo,
            VulnerableCategory vulnerableCategory,
            String vulnerableReasonText,
            boolean expectsError) {
        // Given
        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerablePeopleYesNo(vulnerablePeopleYesNo)
                .vulnerableCategory(vulnerableCategory)
                .vulnerableReasonText(vulnerableReasonText)
                .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .vulnerableAdultsChildren(vulnerableAdultsChildren)
                .build();

        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(enforcementOrder)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (expectsError) {
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors().get(0))
                    .contains("In 'How are they vulnerable?', you have entered more than the maximum number of characters")
                    .contains("6,800");
        } else {
            assertThat(response.getErrors()).isEmpty();
        }
    }

    private static Stream<Arguments> characterLimitScenarios() {
        return Stream.of(
                // Exceeds limit - all categories
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS,
                        "a".repeat(VULNERABLE_REASON_TEXT_LIMIT + 1),
                        true
                ),
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_CHILDREN,
                        "a".repeat(VULNERABLE_REASON_TEXT_LIMIT + 1),
                        true
                ),
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS_AND_CHILDREN,
                        "a".repeat(VULNERABLE_REASON_TEXT_LIMIT + 1),
                        true
                ),
                // Within limit
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS,
                        "a".repeat(VULNERABLE_REASON_TEXT_LIMIT),
                        false
                ),
                // Exactly at limit
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_CHILDREN,
                        "a".repeat(VULNERABLE_REASON_TEXT_LIMIT),
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("dataPreservationScenarios")
    void shouldPreserveCaseDataWhenValidationPasses(
            YesNoNotSure vulnerablePeopleYesNo,
            VulnerableCategory vulnerableCategory,
            String vulnerableReasonText) {
        // Given
        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerablePeopleYesNo(vulnerablePeopleYesNo)
                .vulnerableCategory(vulnerableCategory)
                .vulnerableReasonText(vulnerableReasonText)
                .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .vulnerableAdultsChildren(vulnerableAdultsChildren)
                .build();

        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(enforcementOrder)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getEnforcementOrder()
                .getVulnerableAdultsChildren().getVulnerableReasonText())
                .isEqualTo(vulnerableReasonText);
        assertThat(response.getData().getEnforcementOrder()
                .getVulnerableAdultsChildren().getVulnerablePeopleYesNo())
                .isEqualTo(vulnerablePeopleYesNo);
        assertThat(response.getData().getEnforcementOrder()
                .getVulnerableAdultsChildren().getVulnerableCategory())
                .isEqualTo(vulnerableCategory);
    }

    private static Stream<Arguments> dataPreservationScenarios() {
        return Stream.of(
                // Empty string
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS,
                        ""
                ),
                // Very short text (1 character)
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS,
                        "a"
                ),
                // Short valid text
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS,
                        "Short text"
                ),
                // Valid text for vulnerable children
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_CHILDREN,
                        "Valid text"
                ),
                // Valid text for vulnerable adults and children
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS_AND_CHILDREN,
                        "Some text"
                ),
                // Text at exact limit
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS,
                        "a".repeat(VULNERABLE_REASON_TEXT_LIMIT)
                )
        );
    }
}
