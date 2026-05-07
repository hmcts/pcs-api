package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;
import java.util.UUID;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class CreateCaseLinkTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;


    @InjectMocks
    private CreateCaseLink underTest;


    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }


    @Test
    void shouldCreateCaseLinksInSubmitCallback() {
        // Given
        CaseLink caseLink = CaseLink.builder().build();
        List<ListValue<CaseLink>> caseLists = List.of(
            ListValue.<CaseLink>builder()
                .id(UUID.randomUUID().toString())
                .value(caseLink)
                .build());

        PCSCase pcsCase = PCSCase.builder().caseLinks(caseLists).build();
        doNothing().when(pcsCaseService).patchCaseLinks(TEST_CASE_REFERENCE, pcsCase);

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseService).patchCaseLinks(TEST_CASE_REFERENCE, pcsCase);
    }
}
