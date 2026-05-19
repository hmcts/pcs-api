package uk.gov.hmcts.reform.pcs.ccd.event.legalrepcontactdetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepresentativedetails.LegalRepresentativeContactDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative.LegalRepresentativeService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeContactDetailsTest extends BaseEventTest {

    @Mock
    private LegalRepresentativeContactDetailsPage legalRepresentativeContactDetailsPage;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private OrganisationService organisationService;

    @Mock
    private LegalRepresentativeService legalRepresentativeService;

    @Mock
    private AddressFormatter addressFormatter;

    @Mock
    private UserInfo userDetails;

    @BeforeEach
    void setUp() {
        LegalRepresentativeContactDetails legalRepresentativeContactDetails = new LegalRepresentativeContactDetails(
            legalRepresentativeContactDetailsPage,
            securityContextService,
            organisationService,
            legalRepresentativeService,
            addressFormatter
        );
        setEventUnderTest(legalRepresentativeContactDetails);
    }

    @Test
    void start_SetsAddress() {
        // given
        String userEmail = "email";
        String formatedAddress = "address";
        AddressUK orgAddress = AddressUK.builder().build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        when(organisationService.getOrganisationAddressForCurrentUser()).thenReturn(orgAddress);
        when(userDetails.getSub()).thenReturn(userEmail);
        when(addressFormatter.formatMediumAddress(orgAddress, AddressFormatter.BR_DELIMITER))
            .thenReturn(formatedAddress);
        PCSCase caseData = PCSCase.builder().build();

        // when
        PCSCase updatedCaseData = callStartHandler(caseData);

        // then
        LegalRepresentativeDetails legalRepresentativeContactDetails = updatedCaseData
            .getLegalRepresentativeDetails();

        assertThat(legalRepresentativeContactDetails.getLegalRepresentativeOrganisationAddress())
            .isEqualTo(orgAddress);
        assertThat(legalRepresentativeContactDetails.getOrganisationAddressFound()).isEqualTo(YesOrNo.YES);
        assertThat(legalRepresentativeContactDetails.getOriginalEmailAddress()).isEqualTo(userEmail);
        assertThat(legalRepresentativeContactDetails.getFormattedContactAddress())
            .isEqualTo(formatedAddress);
    }

    @Test
    void start_WithNoOrganisationAddress() {
        // given
        String userEmail = "email";
        String formatedAddress = "address";
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        when(userDetails.getSub()).thenReturn(userEmail);
        when(addressFormatter.formatMediumAddress(null, AddressFormatter.BR_DELIMITER))
            .thenReturn(formatedAddress);
        PCSCase caseData = PCSCase.builder().build();

        // when
        PCSCase updatedCaseData = callStartHandler(caseData);

        // then
        LegalRepresentativeDetails legalRepresentativeContactDetails = updatedCaseData
            .getLegalRepresentativeDetails();

        assertThat(legalRepresentativeContactDetails.getLegalRepresentativeOrganisationAddress())
            .isNull();
        assertThat(legalRepresentativeContactDetails.getOrganisationAddressFound()).isEqualTo(YesOrNo.NO);
        assertThat(legalRepresentativeContactDetails.getOriginalEmailAddress()).isEqualTo(userEmail);
        assertThat(legalRepresentativeContactDetails.getFormattedContactAddress())
            .isEqualTo(formatedAddress);
    }

    @Test
    void start_WithLegalRepresentativeEntity() {
        // given
        String userEmail = "email";
        String formatedAddress = "address";
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        when(userDetails.getSub()).thenReturn(userEmail);
        when(addressFormatter.formatMediumAddress(null, AddressFormatter.BR_DELIMITER))
            .thenReturn(formatedAddress);
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder().build();
        PCSCase caseData = PCSCase.builder()
            .legalRepresentativeDetails(legalRepresentativeDetails)
            .build();

        // when
        PCSCase updatedCase = callStartHandler(caseData);

        // then
        LegalRepresentativeDetails actual = updatedCase.getLegalRepresentativeDetails();
        assertThat(actual.getLegalRepresentativeOrganisationAddress()).isNull();
        assertThat(actual.getOrganisationAddressFound()).isEqualTo(YesOrNo.NO);
        assertThat(actual.getOriginalEmailAddress()).isEqualTo(userEmail);
        assertThat(actual.getFormattedContactAddress()).isEqualTo(formatedAddress);
    }

    @Test
    void submit_SavesLegalRepresentativeDetails() {
        // given
        UUID userId = UUID.randomUUID();
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder().build();
        PCSCase caseData = PCSCase.builder()
            .legalRepresentativeDetails(legalRepresentativeDetails)
            .build();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);

        // when
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // then
        verify(legalRepresentativeService).save(userId, legalRepresentativeDetails);
        assertThat(submitResponse.getConfirmationBody())
            .contains("legal representative's information");
    }

}
