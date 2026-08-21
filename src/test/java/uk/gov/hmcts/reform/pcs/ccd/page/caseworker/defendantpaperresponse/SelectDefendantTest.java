package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SelectDefendantTest extends BasePageTest {

    @Mock
    private DefendantResponseRepository defendantResponseRepository;

    @InjectMocks
    private SelectDefendant selectDefendant;

    @BeforeEach
    void setUp() {
        setPageUnderTest(selectDefendant);
    }

    @Test
    void shouldNotReturnErrorWhenSelectedDefendantHasNotResponded() {
        // Given
        UUID partyId = UUID.randomUUID();
        DynamicList dynamicList = DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantRadioList(dynamicList)
            .build();

        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(TEST_CASE_REFERENCE, partyId))
            .thenReturn(false);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isNull();
    }

    @Test
    void shouldReturnErrorWhenSelectedDefendantHasResponded() {
        // Given
        UUID partyId = UUID.randomUUID();
        DynamicList dynamicList = DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantRadioList(dynamicList)
            .build();

        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(TEST_CASE_REFERENCE, partyId))
            .thenReturn(true);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride())
            .isEqualTo("This defendant has already submitted a response."
                           + " If the have filed a further response please use the upload document function.");
    }
}
