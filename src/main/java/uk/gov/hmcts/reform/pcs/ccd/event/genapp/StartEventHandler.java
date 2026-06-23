package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.config.RequestInterceptor;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativeService;

import java.util.UUID;

@Component("genAppStartEventHandler")
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final SecurityContextService securityContextService;
    private final LegalRepresentativeService legalRepresentativeService;
    private final FeeApplier feeApplier;

    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        MDC.put(RequestInterceptor.CASE_ID, String.valueOf(eventPayload.caseReference()));
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        setRepresentedParties(caseReference, caseData);

        applyApplicationFeeAmounts(caseData);

        caseData.getXuiGenAppRequest().setShowHwfScreens(VerticalYesNo.YES);

        return caseData;
    }

    // Set represented parties if the current user is a legal rep
    private void setRepresentedParties(long caseReference, PCSCase caseData) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        legalRepresentativeService.getRepresentedPartiesDynamicList(currentUserId, caseReference)
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
