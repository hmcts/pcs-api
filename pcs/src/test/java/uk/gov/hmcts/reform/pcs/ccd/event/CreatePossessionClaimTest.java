package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePossessionClaimTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnterPropertyAddress enterPropertyAddress;
    @Mock
    private CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    @Mock
    private PropertyNotEligible propertyNotEligible;

    @BeforeEach
    void setUp() {
        CreatePossessionClaim underTest = new CreatePossessionClaim(pcsCaseService, enterPropertyAddress,
                                                                    crossBorderPostcodeSelection, propertyNotEligible);

        setEventUnderTest(underTest);
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        // Given
        PCSCase caseData = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);
        LegislativeCountry legislativeCountry = mock(LegislativeCountry.class);

        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(caseData.getLegislativeCountry()).thenReturn(legislativeCountry);

        // When
        callSubmitHandler(caseData);

        // Then
        verify(pcsCaseService).createCase(TEST_CASE_REFERENCE, propertyAddress, legislativeCountry);
    }

}
