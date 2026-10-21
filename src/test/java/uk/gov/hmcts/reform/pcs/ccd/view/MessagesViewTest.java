package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.order.OrderTasks;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.config.PebbleConfiguration;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagesViewTest {

    @Mock
    private FeatureToggleService featureToggleService;

    @Mock
    private OrderTasks orderTasks;

    @Mock
    private UserRoleService userRoleService;

    private MessagesView underTest;

    @BeforeEach
    void setUp() {
        underTest = new MessagesView(new PebbleConfiguration().pebbleEngine(), "http://frontend",
            featureToggleService, orderTasks, userRoleService);
    }

    @Test
    void shouldRenderMessagesTabWhenMakeOrderIsEnabled() {
        when(featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)).thenReturn(true);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, PcsCaseEntity.builder().build());

        assertThat(pcsCase.getMessagesMarkdown()).contains("There are no open messages.");
    }

    @Test
    void shouldNotRenderMessagesTabWhenMakeOrderIsDisabled() {
        when(featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)).thenReturn(false);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, PcsCaseEntity.builder().build());

        assertThat(pcsCase.getMessagesMarkdown()).isNull();
    }
}
