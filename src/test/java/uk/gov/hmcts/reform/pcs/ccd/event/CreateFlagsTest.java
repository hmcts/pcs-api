package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@ExtendWith(MockitoExtension.class)
class CreateFlagsTest extends  BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private FeatureToggleService featureToggleService;


    @InjectMocks
    private CreateFlags underTest;

    @BeforeEach
    void setUp() {
        when(featureToggleService.isEnabled(RELEASE_1_DOT_3)).thenReturn(true);
        setEventUnderTest(underTest);
    }

    @Test
    void shouldBeConfiguredForEventStates() {
        assertConfiguredForStates(EventStates.createFlags());
    }

    @Test
    void shouldBeConfiguredForPreRelease1dot3EventStatesWhenFeatureFlagDisabled() {
        when(featureToggleService.isEnabled(RELEASE_1_DOT_3)).thenReturn(false);
        setEventUnderTest(underTest);

        assertConfiguredForStates(State.PENDING_CASE_ISSUED);
    }

    @Test
    void shouldCreateFlagsInSubmitCallback() {
        // Given
        List<ListValue<FlagDetail>> flagDetails = createFlagDetails();
        Flags flags = Flags.builder()
            .details(flagDetails)
            .build();

        PCSCase pcsCase = PCSCase.builder().caseFlags(flags).build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseService).patchCaseFlags(TEST_CASE_REFERENCE, pcsCase);
    }

    private List<ListValue<FlagDetail>> createFlagDetails() {

        return List.of(
            ListValue.<FlagDetail>builder()
                .id(UUID.randomUUID().toString())
                .value(FlagDetail.builder()
                           .flagCode("CF0002")
                           .name("Complex Case")
                           .build())
                .build());
    }
}
