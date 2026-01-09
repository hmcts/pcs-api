package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class VulnerableAdultsChildrenPageTest extends BasePageTest {


    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new VulnerableAdultsChildrenPage(textAreaValidationService));
    }

    @ParameterizedTest
    @MethodSource("characterLimitScenarios")
    void shouldValidateCharacterLimit(
            YesNoNotSure vulnerablePeoplePresent,
            VulnerableCategory vulnerableCategory,
            String vulnerableReasonText,
            boolean expectsError) {
        // Given
        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerableCategory(vulnerableCategory)
                .vulnerableReasonText(vulnerableReasonText)
                .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantDetails(WarrantDetails.builder()
                    .vulnerablePeoplePresent(vulnerablePeoplePresent)
                    .vulnerableAdultsChildren(vulnerableAdultsChildren)
                    .build())
                .build();

        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(enforcementOrder)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (expectsError) {
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors().getFirst())
                    .contains("In ‘How are they vulnerable?’, you have entered more than the maximum number "
                            + "of characters")
                    .contains("6,800");
        } else {
            assertThat(response.getErrors()).isEmpty();
        }
    }

    private static Stream<Arguments> characterLimitScenarios() {
        int limit = TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;
        return Stream.of(
                // Exceeds limit - all categories
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS,
                        "a".repeat(limit + 1),
                        true
                ),
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_CHILDREN,
                        "a".repeat(limit + 1),
                        true
                ),
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS_AND_CHILDREN,
                        "a".repeat(limit + 1),
                        true
                ),
                // Within limit
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_ADULTS,
                        "a".repeat(limit),
                        false
                ),
                // Exactly at limit
                arguments(
                        YesNoNotSure.YES,
                        VulnerableCategory.VULNERABLE_CHILDREN,
                        "a".repeat(limit),
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("dataPreservationScenarios")
    void shouldPreserveCaseDataWhenValidationPasses(
            YesNoNotSure vulnerablePeoplePresent,
            VulnerableCategory vulnerableCategory,
            String vulnerableReasonText) {
        // Given
        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerableCategory(vulnerableCategory)
                .vulnerableReasonText(vulnerableReasonText)
                .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantDetails(WarrantDetails.builder()
                    .vulnerablePeoplePresent(vulnerablePeoplePresent)
                    .vulnerableAdultsChildren(vulnerableAdultsChildren)
                    .build())
                .build();

        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(enforcementOrder)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getEnforcementOrder().getWarrantDetails()
                .getVulnerableAdultsChildren().getVulnerableReasonText())
                .isEqualTo(vulnerableReasonText);
        assertThat(response.getData().getEnforcementOrder().getWarrantDetails()
                .getVulnerablePeoplePresent())
                .isEqualTo(vulnerablePeoplePresent);
        assertThat(response.getData().getEnforcementOrder().getWarrantDetails()
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
                        "a".repeat(TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT)
                )
        );
    }
}
