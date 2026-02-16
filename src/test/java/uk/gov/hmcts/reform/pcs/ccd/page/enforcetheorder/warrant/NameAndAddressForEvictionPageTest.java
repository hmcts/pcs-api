package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NameAndAddressForEvictionPage tests")
class NameAndAddressForEvictionPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new NameAndAddressForEvictionPage());
    }

    @Nested
    @DisplayName("Mid-event callback tests")
    class MidEventCallbackTests {

        @Test
        @DisplayName("Should set navigation flags when correctNameAndAddress is NO")
        void shouldSetNavigationFlagsWhenCorrectNameAndAddressIsNo() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .warrantDetails(WarrantDetails.builder()
                        .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                            .correctNameAndAddress(VerticalYesNo.NO)
                            .build())
                        .build())
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNullOrEmpty();
            EnforcementOrder enforcementOrder = response.getData().getEnforcementOrder();
            assertThat(enforcementOrder.getWarrantDetails()
                    .getShowChangeNameAddressPage()).isEqualTo(YesOrNo.YES);
            assertThat(enforcementOrder.getWarrantDetails()
                    .getShowPeopleWhoWillBeEvictedPage()).isEqualTo(YesOrNo.NO);
        }

        @Test
        @DisplayName("Should set navigation flags when correctNameAndAddress is YES")
        void shouldSetNavigationFlagsWhenCorrectNameAndAddressIsYes() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .warrantDetails(WarrantDetails.builder()
                        .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                            .correctNameAndAddress(VerticalYesNo.YES)
                            .build())
                        .build())
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNullOrEmpty();
            EnforcementOrder enforcementOrder = response.getData().getEnforcementOrder();
            assertThat(enforcementOrder.getWarrantDetails()
                    .getShowChangeNameAddressPage()).isEqualTo(YesOrNo.NO);
            assertThat(enforcementOrder.getWarrantDetails()
                    .getShowPeopleWhoWillBeEvictedPage()).isEqualTo(YesOrNo.YES);
        }
    }
}


