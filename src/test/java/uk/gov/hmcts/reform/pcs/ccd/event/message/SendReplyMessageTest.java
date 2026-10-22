package uk.gov.hmcts.reform.pcs.ccd.event.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseMessageEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SendReplyMessageTest extends BaseEventTest {

    @InjectMocks
    private SendReplyMessage underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }

    @Test
    void shouldOnlyShowEventWhenMakeOrderIsEnabled() {
        assertConfiguredShowConditions("featureFlags.makeOrderEnabled=\"YES\"");
    }

    @Test
    void shouldShowTheSubjectAsWrittenNotAsMarkdownOrHtml() {
        CaseMessageEntity message = CaseMessageEntity.builder()
            .subject("<img src=x> [GA1](x) *now* #2 O'Neil's").build();

        assertThat(SendReplyMessage.summary(message)).startsWith(
            "### &lt;img src=x&gt; \\[GA1\\](x) \\*now\\* \\#2 O&#39;Neil&#39;s\n\n");
    }
}
