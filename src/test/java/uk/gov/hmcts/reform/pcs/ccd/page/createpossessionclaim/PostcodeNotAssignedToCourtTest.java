package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PostcodeNotAssignedToCourt Page Tests")
class PostcodeNotAssignedToCourtTest extends BasePageTest {

    private EventBuilder<PCSCase, UserRole, State> eventBuilder;
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> fieldBuilder;
    private PageBuilder pageBuilder;
    private PostcodeNotAssignedToCourt underTest;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        eventBuilder = mock(EventBuilder.class);
        fieldBuilder = mock(FieldCollectionBuilder.class);

        when(eventBuilder.fields()).thenReturn(fieldBuilder);
        when(fieldBuilder.page(anyString(), any())).thenReturn(fieldBuilder);
        when(fieldBuilder.pageLabel(anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.showCondition(anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.readonly(any(), anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.label(anyString(), anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.label(anyString(), anyString(), anyString())).thenReturn(fieldBuilder);

        pageBuilder = new PageBuilder(eventBuilder);
        underTest = new PostcodeNotAssignedToCourt();
    }

    @Test
    @DisplayName("Should build page configuration successfully")
    void shouldBuildPageConfigurationSuccessfully() {
        underTest.addTo(pageBuilder);

        verify(fieldBuilder).page(eq("postcodeNotAssignedToCourt"), any());
        verify(fieldBuilder).pageLabel(eq("You cannot use this online service"));
    }

    @ParameterizedTest
    @MethodSource("showConditionScenarios")
    @DisplayName("Should apply correct show conditions for different views")
    void shouldApplyCorrectShowConditions(String view, String expectedShowCondition, String expectedLabelId) {
        underTest.addTo(pageBuilder);

        // Verify the main show condition is set
        verify(fieldBuilder).showCondition(eq("showPostcodeNotAssignedToCourt=\"Yes\""));
        
        // Verify specific show conditions for each view are configured
        verify(fieldBuilder).label(eq(expectedLabelId), anyString(), eq(expectedShowCondition));
    }

    private static Stream<Arguments> showConditionScenarios() {
        return Stream.of(
            arguments("ENGLAND", 
                "showPostcodeNotAssignedToCourt=\"Yes\" AND postcodeNotAssignedView=\"ENGLAND\"", 
                "postcodeNotAssignedToCourt-england"),
            arguments("WALES", 
                "showPostcodeNotAssignedToCourt=\"Yes\" AND postcodeNotAssignedView=\"WALES\"", 
                "postcodeNotAssignedToCourt-wales"),
            arguments("ALL_COUNTRIES", 
                "showPostcodeNotAssignedToCourt=\"Yes\" AND postcodeNotAssignedView=\"ALL_COUNTRIES\"", 
                "postcodeNotAssignedToCourt-all")
        );
    }
}
