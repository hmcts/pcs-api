package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderStepRepository;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdersViewTest {

    @Mock
    private OrderStepRepository orderStepRepository;
    @Mock
    private FeatureToggleService featureToggleService;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void onlyIncludesOrdersMarkdownWhenMakeOrderIsEnabled(boolean enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)).thenReturn(enabled);
        PCSCase pcsCase = PCSCase.builder().ordersMarkdown("Previously rendered orders").build();

        new OrdersView(orderStepRepository, featureToggleService).setCaseFields(pcsCase, new PcsCaseEntity());

        var json = new JacksonConfiguration().getMapper().valueToTree(pcsCase);
        if (enabled) {
            assertThat(json.path("ordersMarkdown").asText()).isEqualTo("There are no orders on this case.");
        } else {
            assertThat(json.has("ordersMarkdown")).isFalse();
            verifyNoInteractions(orderStepRepository);
        }
    }
}
