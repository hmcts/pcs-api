package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableAdultsChildren.VULNERABLE_REASON_TEXT_LIMIT;

class EvictionVulnerableAdultsChildrenPageTest extends BasePageTest {

    private EvictionVulnerableAdultsChildrenPage page;

    @BeforeEach
    void setUp() {
        page = new EvictionVulnerableAdultsChildrenPage();
        setPageUnderTest(page);
    }

    @Test
    void shouldReturnErrorWhenTextExceedsCharacterLimit() {
        // Given
        String longText = "a".repeat(VULNERABLE_REASON_TEXT_LIMIT + 1);
        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerablePeopleYesNo(YesNoNotSure.YES)
                .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                .vulnerableReasonText(longText)
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
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors().get(0))
                .contains("In 'How are they vulnerable?', you have entered more than the maximum number of characters")
                .contains("6800");
    }

    @Test
    void shouldNotReturnErrorWhenTextIsWithinCharacterLimit() {
        // Given
        String validText = "a".repeat(VULNERABLE_REASON_TEXT_LIMIT);
        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerablePeopleYesNo(YesNoNotSure.YES)
                .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                .vulnerableReasonText(validText)
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
    }

    @Test
    void shouldNotReturnErrorWhenNoTextProvided() {
        // Given
        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerablePeopleYesNo(YesNoNotSure.NO)
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
    }
}

