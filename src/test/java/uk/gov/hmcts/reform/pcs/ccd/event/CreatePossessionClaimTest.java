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
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreatePossessionClaimTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private OrganisationService organisationService;
    @Mock
    private FeeApplier feeApplier;
    @Mock
    private EnterPropertyAddress enterPropertyAddress;
    @Mock
    private CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    @Mock
    private PropertyNotEligible propertyNotEligible;

    @BeforeEach
    void setUp() {
        CreatePossessionClaim underTest = new CreatePossessionClaim(
            pcsCaseService, organisationService, feeApplier, enterPropertyAddress,
            crossBorderPostcodeSelection, propertyNotEligible
        );

        setEventUnderTest(underTest);
    }

    @Test
    void shouldCreateCaseOnSubmit() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 Test Street").build();
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        // When
        callSubmitHandler(caseData);

        // Then
        verify(pcsCaseService).createCase(eq(TEST_CASE_REFERENCE), eq(propertyAddress),
            eq(LegislativeCountry.ENGLAND), any());
    }
}
