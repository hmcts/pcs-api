package uk.gov.hmcts.reform.pcs.ccd.page.legalrepresentativedetails;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

@Component
public class LegalRepresentativeContactDetailsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("legalRepresentativeDetails", this::midEvent)
            .pageLabel("Update legal representative details")
            .complex(PCSCase::getLegalRepresentativeDetails)
                .mandatory(LegalRepresentativeDetails::getEmailAddress)
                .optional(LegalRepresentativeDetails::getReference)
                .optional(LegalRepresentativeDetails::getProvideContactPhoneNumber)
                .mandatory(LegalRepresentativeDetails::getContactPhoneNumber)
                .label("legalRepresentativeDetails-correspondence", "abc")
                .mandatory(LegalRepresentativeDetails::getDifferentPostalAddress)
                .complex(LegalRepresentativeDetails::getCorrespondenceAddress)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                    .done()
            .done()
            .label("legalRepresentativeDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);;
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
