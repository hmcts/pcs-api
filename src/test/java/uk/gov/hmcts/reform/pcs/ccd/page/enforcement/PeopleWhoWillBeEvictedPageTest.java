package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PeopleWhoWillBeEvictedPage tests")
class PeopleWhoWillBeEvictedPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new PeopleWhoWillBeEvictedPage());
    }

    @Nested
    @DisplayName("Mid-event callback tests")
    class MidEventCallbackTests {

        @Test
        @DisplayName("Should return error when no selection is made")
        void shouldReturnErrorWhenNoSelection() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .peopleToEvict(PeopleToEvict.builder().build())
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors().get(0))
                .contains("Please select whether you want to evict everyone or specific people");
        }

        @Test
        @DisplayName("Should set showPeopleYouWantToEvictPage to NO when evictEveryone is YES")
        void shouldSetShowPeopleYouWantToEvictPageToNoWhenEvictEveryoneIsYes() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .peopleToEvict(PeopleToEvict.builder()
                        .evictEveryone(VerticalYesNo.YES)
                        .build())
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNullOrEmpty();
            EnforcementOrder enforcementOrder = response.getData().getEnforcementOrder();
            assertThat(enforcementOrder.getShowPeopleYouWantToEvictPage()).isEqualTo(VerticalYesNo.NO);
        }

        @Test
        @DisplayName("Should set showPeopleYouWantToEvictPage to YES when evictEveryone is NO")
        void shouldSetShowPeopleYouWantToEvictPageToYesWhenEvictEveryoneIsNo() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .peopleToEvict(PeopleToEvict.builder()
                        .evictEveryone(VerticalYesNo.NO)
                        .build())
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNullOrEmpty();
            EnforcementOrder enforcementOrder = response.getData().getEnforcementOrder();
            assertThat(enforcementOrder.getShowPeopleYouWantToEvictPage()).isEqualTo(VerticalYesNo.YES);
        }
    }
}

