package uk.gov.hmcts.reform.pcs.ccd.event.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;

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
}
