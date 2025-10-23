package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

@AllArgsConstructor
@Component
public class DefendantsDOBPage implements CcdPageConfiguration {

    private final AddressValidator addressValidator;


    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDOB", this::midEvent)
            .pageLabel("Defendant DOB")
            .label("defendantDOBLabel", "My Defendant DOB label");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                    CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        DefendantDetails defendantDetails = caseData.getDefendant1();

//        if (defendantDetails.getAddressSameAsPossession() == VerticalYesNo.NO
//            && defendantDetails.getAddressKnown() == VerticalYesNo.YES) {
//
//            AddressUK correspondenceAddress = defendantDetails.getCorrespondenceAddress();
//            List<String> validationErrors = addressValidator.validateAddressFields(correspondenceAddress);
//            if (!validationErrors.isEmpty()) {
//                return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
//                    .errors(validationErrors)
//                    .build();
//            }
//        }
//
//        // TODO: Update this once multiple defendant support is implemented.
//        //  Set the text dynamically for one/multiple defendants.
//        caseData.getDefendantCircumstances().setDefendantTermPossessive("defendants'");
//
//        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
//            .data(caseData)
//            .build();
            return null;
    }

}
