package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.event.enforcement.EnforcementOrderEvent;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EnforcementApplicationPage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    @Mock
    private SavingPageBuilderFactory savingPageBuilderFactory;

    @BeforeEach
    void setUp() {
        SavingPageBuilder savingPageBuilder = mock(SavingPageBuilder.class);
        when(savingPageBuilderFactory.create(any())).thenReturn(savingPageBuilder);
        when(savingPageBuilder.add(any())).thenReturn(savingPageBuilder);

        EnforcementOrderEvent underTest =
                new EnforcementOrderEvent(savingPageBuilderFactory);

        setEventUnderTest(underTest);
    }
}
