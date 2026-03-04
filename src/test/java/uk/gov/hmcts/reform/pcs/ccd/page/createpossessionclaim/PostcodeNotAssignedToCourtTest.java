package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.ShowCondition;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.dto.CreateClaimData;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@DisplayName("PostcodeNotAssignedToCourt Page Tests")
@ExtendWith(MockitoExtension.class)
class PostcodeNotAssignedToCourtTest {

    @Mock
    private EventBuilder<CreateClaimData, UserRole, State> eventBuilder;
    @Mock
    private FieldCollectionBuilder<CreateClaimData, State, EventBuilder<CreateClaimData, UserRole, State>> fieldBuilder;

    private PostcodeNotAssignedToCourt underTest;

    @BeforeEach
    void setUp() {
        when(eventBuilder.fields()).thenReturn(fieldBuilder);
        when(fieldBuilder.page(anyString(), any())).thenReturn(fieldBuilder);
        when(fieldBuilder.pageLabel(anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.showCondition(any(ShowCondition.class))).thenReturn(fieldBuilder);
        when(fieldBuilder.hidden(any())).thenReturn(fieldBuilder);
        when(fieldBuilder.label(anyString(), anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.label(anyString(), anyString(), any(ShowCondition.class))).thenReturn(fieldBuilder);

        underTest = new PostcodeNotAssignedToCourt();
    }

    @Test
    @DisplayName("Should build page configuration successfully")
    void shouldBuildPageConfigurationSuccessfully() {
        underTest.addTo(eventBuilder);

        verify(fieldBuilder).page(eq("postcodeNotAssignedToCourt"), any());
        verify(fieldBuilder).pageLabel(eq("You cannot use this online service"));
    }

    @ParameterizedTest
    @MethodSource("showConditionScenarios")
    @DisplayName("Should apply correct show conditions for different views")
    void shouldApplyCorrectShowConditions(String expectedShowCondition, String expectedLabelId) {
        underTest.addTo(eventBuilder);

        // Verify the main page show condition is set
        verify(fieldBuilder).showCondition(
            org.mockito.ArgumentMatchers.argThat(
                (ShowCondition sc) -> sc.toString().equals("showPostcodeNotAssignedToCourt=\"Yes\"")
            ));

        // Verify specific show conditions for each view are configured
        verify(fieldBuilder).label(eq(expectedLabelId), anyString(),
            org.mockito.ArgumentMatchers.argThat(
                (ShowCondition sc) -> sc.toString().equals(expectedShowCondition)
            ));
    }

    private static Stream<Arguments> showConditionScenarios() {
        return Stream.of(
            arguments(
                // ENGLAND
                "showPostcodeNotAssignedToCourt=\"Yes\" AND postcodeNotAssignedView=\"ENGLAND\"",
                "postcodeNotAssignedToCourt-england"),
            arguments(
                // WALES
                "showPostcodeNotAssignedToCourt=\"Yes\" AND postcodeNotAssignedView=\"WALES\"",
                "postcodeNotAssignedToCourt-wales"),
            arguments(
                // ALL_COUNTRIES
                "showPostcodeNotAssignedToCourt=\"Yes\" AND postcodeNotAssignedView=\"ALL_COUNTRIES\"",
                "postcodeNotAssignedToCourt-all")
        );
    }
}
