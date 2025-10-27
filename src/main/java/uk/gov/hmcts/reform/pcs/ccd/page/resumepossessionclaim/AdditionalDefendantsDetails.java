package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class AdditionalDefendantsDetails implements CcdPageConfiguration {

    private final AddressValidator addressValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("additionalDefendantsDetails", this::midEvent)
            .pageLabel("Additional Defendants details")
            .showCondition("addAdditionalDefendant=\"Yes\"")
            .mandatoryWithLabel(PCSCase::getDefendants, "Additional Defendants details");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                    CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        List<ListValue<DefendantDetails>> defendants = caseData.getDefendants();

        List<String> addressErrors = validateDefendantAddresses(defendants);
        if (!addressErrors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(addressErrors)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private List<String> validateDefendantAddresses(List<ListValue<DefendantDetails>> defendants) {
        if (defendants == null || defendants.isEmpty()) {
            return List.of();
        }

        List<String> allValidationErrors = new ArrayList<>();

        for (ListValue<DefendantDetails> defendantListValue : defendants) {
            List<String> errors = validateSingleDefendantAddress(defendantListValue.getValue());
            allValidationErrors.addAll(errors);
        }

        return allValidationErrors;
    }

    private List<String> validateSingleDefendantAddress(DefendantDetails defendantDetails) {
        if (defendantDetails == null) {
            return List.of();
        }

        if (!needsAddressValidation(defendantDetails)) {
            return List.of();
        }

        AddressUK correspondenceAddress = defendantDetails.getCorrespondenceAddress();
        if (correspondenceAddress == null) {
            return List.of();
        }

        return addressValidator.validateAddressFields(correspondenceAddress);
    }

    private boolean needsAddressValidation(DefendantDetails defendantDetails) {
        return defendantDetails.getAddressSameAsPossession() == VerticalYesNo.NO
            && defendantDetails.getAddressKnown() == VerticalYesNo.YES;
    }

}
