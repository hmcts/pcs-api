package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativeService;

import java.util.Arrays;
import java.util.List;

@Component("genAppStartEventHandler")
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final OrganisationService organisationService;
    private final LegalRepresentativeService legalRepresentativeService;
    private final FeeApplier feeApplier;
    private final FeatureToggleService featureToggleService;

    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        setRepresentedParties(caseReference, caseData);

        caseData.getXuiGenAppRequest().setApplicationTypeOptionList(buildApplicationTypeList());

        applyApplicationFeeAmounts(caseData);

        caseData.getXuiGenAppRequest().setShowHwfScreens(VerticalYesNo.YES);

        return caseData;
    }

    // Set represented parties if the current user is a legal rep
    private void setRepresentedParties(long caseReference, PCSCase caseData) {
        String organisationIdForCurrentUser = organisationService.getOrganisationIdForCurrentUser();
        DynamicList representedPartyNames = legalRepresentativeService.getRepresentedPartiesDynamicList(
            organisationIdForCurrentUser,
            caseReference
        );
        boolean representingMultipleParties = representedPartyNames.getListItems().size() > 1;
        caseData.setMultipleRepresentedParties(VerticalYesNo.from(representingMultipleParties));
        caseData.setRepresentedPartyNames(representedPartyNames);
        if (representedPartyNames.getListItems().size() == 1) {
            DynamicListElement soleRepresentedParty = representedPartyNames.getListItems().getFirst();
            caseData.setCurrentRepresentedPartyId(soleRepresentedParty.getCode().toString());
            caseData.setCurrentRepresentedPartyName(soleRepresentedParty.getLabel());
        }
    }

    private DynamicStringList buildApplicationTypeList() {
        boolean release1bEnabled = featureToggleService.isEnabled(FeatureFlag.RELEASE_1B);
        List<DynamicStringListElement> listItems = Arrays.stream(GenAppType.values())
            .filter(value -> release1bEnabled || value != GenAppType.SUSPEND)
            .map(value -> DynamicStringListElement.builder()
                .code(value.name())
                .label(value.getLabel())
                .build())
            .toList();

        return DynamicStringList.builder()
            .listItems(listItems)
            .build();
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
        feeApplier.applyFeeAmount(
            caseData,
            FeeType.GEN_APP_SUSPEND_FEE,
            (suppliedCaseData, feeString) -> suppliedCaseData.getXuiGenAppRequest()
                .setSuspendFee(feeString)
        );
    }

}
