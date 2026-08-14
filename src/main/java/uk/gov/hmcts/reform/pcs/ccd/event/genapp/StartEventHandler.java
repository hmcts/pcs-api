package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativeService;

@Component("genAppStartEventHandler")
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final OrganisationService organisationService;
    private final LegalRepresentativeService legalRepresentativeService;
    private final FeeApplier feeApplier;

    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        setRepresentedParties(caseReference, caseData);

        applyApplicationFeeAmounts(caseData);

        caseData.getXuiGenAppRequest().setShowHwfScreens(VerticalYesNo.YES);

        return caseData;
    }

    // Set represented parties if the current user is a legal rep
    private void setRepresentedParties(long caseReference, PCSCase caseData) {
        String organisationIdForCurrentUser = organisationService.getOrganisationIdForCurrentUser();
        legalRepresentativeService.getRepresentedPartiesDynamicList(organisationIdForCurrentUser, caseReference)
            .ifPresent(representedPartyNames -> {
                boolean representingMultipleParties = representedPartyNames.getListItems().size() > 1;
                caseData.setMultipleRepresentedParties(VerticalYesNo.from(representingMultipleParties));
                caseData.setRepresentedPartyNames(representedPartyNames);
                if (representedPartyNames.getListItems().size() == 1) {
                    DynamicListElement soleRepresentedParty = representedPartyNames.getListItems().getFirst();
                    caseData.setCurrentRepresentedPartyId(soleRepresentedParty.getCode().toString());
                    caseData.setCurrentRepresentedPartyName(soleRepresentedParty.getLabel());
                }
            });
    }

    private void applyApplicationFeeAmounts(PCSCase caseData) {
        feeApplier.applyFeeAmount(
            caseData,
            FeeType.GEN_APP_STANDARD_FEE,
            (suppliedCaseData, feeString) -> suppliedCaseData.getXuiGenAppRequest().setStandardFee(feeString)
        );

        feeApplier.applyFeeAmount(
            caseData,
            FeeType.GEN_APP_MAX_FEE,
            (suppliedCaseData, feeString) -> suppliedCaseData.getXuiGenAppRequest().setMaxFee(feeString)
        );
    }

}
