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


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePossessionClaimTest extends BaseEventTest {

    private static final String ORG_ID = "QKLHPMU";
    private static final String ORG_PROFILE_ID = "SOLICITOR_PROFILE";

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
    void shouldCreateCaseWithTheCreatorsOrganisation() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 Test Street").build();
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);
        when(organisationService.getOrgProfileIdForCurrentUser()).thenReturn(ORG_PROFILE_ID);

        // When
        callSubmitHandler(caseData);

        // Then the organisation is carried into the case, so CaseAccessGroups derive on the draft
        verify(pcsCaseService).createCase(TEST_CASE_REFERENCE, propertyAddress,
                                          LegislativeCountry.ENGLAND, ORG_ID, ORG_PROFILE_ID);
    }
}
