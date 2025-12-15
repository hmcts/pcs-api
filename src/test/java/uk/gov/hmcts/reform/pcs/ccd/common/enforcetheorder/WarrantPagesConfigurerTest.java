package uk.gov.hmcts.reform.pcs.ccd.common.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantPagesConfigurerTest {

    @Mock
    private SavingPageBuilderFactory savingPageBuilderFactory;

    @InjectMocks
    private WarrantPagesConfigurer warrantPagesConfigurer;

    @SuppressWarnings("unchecked")
    @Test
    void shouldConfigureAllPages() {
        // Given
        SavingPageBuilder savingPageBuilder = mock(SavingPageBuilder.class);
        when(savingPageBuilder.add(any())).thenReturn(savingPageBuilder);
        when(savingPageBuilderFactory.create(any(Event.EventBuilder.class), eq(EventId.enforceTheOrder)))
                .thenReturn(savingPageBuilder);
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = mock(Event.EventBuilder.class);

        // When
        warrantPagesConfigurer.configurePages(eventBuilder);

        // Then
        verify(savingPageBuilder, times(24)).add(any());
    }
}