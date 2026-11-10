package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.NameAndAddressForEvictionPageHelper;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
@DisplayName("NameAndAddressForEvictionPage tests")
class NameAndAddressForEvictionPageTest extends BasePageTest {

    @Mock
    private NameAndAddressForEvictionPageHelper nameAndAddressForEvictionPageHelper;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new NameAndAddressForEvictionWritPage(nameAndAddressForEvictionPageHelper));
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
                    .writDetails(WritDetails.builder()
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
            assertThat(enforcementOrder.getWritDetails()
                    .getShowChangeNameAddressPage()).isEqualTo(YesOrNo.YES);
        }

        @Test
        @DisplayName("Should set navigation flags when correctNameAndAddress is YES")
        void shouldSetNavigationFlagsWhenCorrectNameAndAddressIsYes() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .writDetails(WritDetails.builder()
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
            assertThat(enforcementOrder.getWritDetails()
                    .getShowChangeNameAddressPage()).isEqualTo(YesOrNo.NO);
        }
    }
}


