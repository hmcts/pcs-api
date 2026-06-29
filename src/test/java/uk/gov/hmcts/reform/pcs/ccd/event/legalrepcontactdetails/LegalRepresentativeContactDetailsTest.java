package uk.gov.hmcts.reform.pcs.ccd.event.legalrepcontactdetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepresentativedetails.LegalRepresentativeContactDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative.LegalRepresentativePageService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeContactDetailsTest extends BaseEventTest {

    @Mock
    private LegalRepresentativeContactDetailsPage legalRepresentativeContactDetailsPage;

    @Mock
    private OrganisationService organisationService;

    @Mock
    private LegalRepresentativePageService legalRepresentativePageService;

    @BeforeEach
    void setUp() {
        LegalRepresentativeContactDetails legalRepresentativeContactDetails = new LegalRepresentativeContactDetails(
            legalRepresentativeContactDetailsPage,
            organisationService,
            legalRepresentativePageService
        );
        setEventUnderTest(legalRepresentativeContactDetails);
    }

    @Test
    void start_WithLegalRepresentativeEntity() {
        // given
        String organisationId = "org";
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder().build();
        PCSCase caseData = PCSCase.builder()
            .legalRepresentativeDetails(legalRepresentativeDetails)
            .build();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(legalRepresentativePageService.retrieveLegalRepresentativeDetails(organisationId, TEST_CASE_REFERENCE,
                                                                               legalRepresentativeDetails))
            .thenReturn(legalRepresentativeDetails);

        // when
        PCSCase updatedCase = callStartHandler(caseData);

        // then
        LegalRepresentativeDetails actual = updatedCase.getLegalRepresentativeDetails();
        assertThat(actual).isEqualTo(legalRepresentativeDetails);
    }

    @Test
    void submit_SavesLegalRepresentativeDetails() {
        // given
        String organisationId = "org";
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder().build();
        PCSCase caseData = PCSCase.builder()
            .legalRepresentativeDetails(legalRepresentativeDetails)
            .build();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);

        // when
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // then
        verify(legalRepresentativePageService).save(organisationId, TEST_CASE_REFERENCE, legalRepresentativeDetails);
        assertThat(submitResponse.getConfirmationBody())
            .contains("legal representative's information");
    }

}
