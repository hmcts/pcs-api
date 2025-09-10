package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class DefendantsDetails implements CcdPageConfiguration {

    private final PostcodeValidator postcodeValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDetails", this::validatePostcode)
            .pageLabel("Defendant 1 details")
            .mandatory(PCSCase::getDefendant1);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> validatePostcode(CaseDetails<PCSCase, State> details,
                                                                          CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();

        // Validate defendant correspondence address postcode if present
        if (caseData.getDefendant1() != null && caseData.getDefendant1().getCorrespondenceAddress() != null) {
            List<String> addressErrors = postcodeValidator.getValidationErrors(
                caseData.getDefendant1().getCorrespondenceAddress(), 
                "defendant1.correspondenceAddress"
            );
            errors.addAll(addressErrors);
        }

        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(errors)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
