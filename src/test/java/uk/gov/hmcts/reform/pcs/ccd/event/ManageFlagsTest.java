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
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManageFlagsTest extends BaseEventTest {
    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private ManageFlags underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
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
        verify(pcsCaseService).patchCaseFlags(TEST_CASE_REFERENCE, pcsCase, EventFlow.UPDATE.name());
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
