package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.allocatebailiff;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.enforcement.domain.BailiffAllocationDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.StaffUser;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffDirectoryService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * "Find the bailiff's name", the first page of "Allocate case to bailiff". The bailiff is chosen with XUI's
 * StaffUser type-ahead, which searches staff reference data for the service's admin staff. Its choice is
 * checked here against AM, and the bailiff's email address is looked up for check your answers.
 */
@RequiredArgsConstructor
public class FindBailiffPage implements CcdPageConfiguration, CcdPage {

    public static final String PAGE_LABEL = "Find the bailiff's name";

    /**
     * The StaffUser type-ahead filter. Bailiffs are assumed to be in the ADMIN role category
     * (OPEN-QUESTIONS 24). XUI ignores REGION- filters, so there is no location filter yet.
     */
    static final String STAFF_USER_SEARCH = "#ARGUMENT(CATEGORY-ADMIN)";

    public static final String CHOOSE_A_BAILIFF_ERROR = "Choose a bailiff";
    public static final String NOT_A_BAILIFF_ERROR = "Choose a user who has the bailiff role";

    private final BailiffDirectoryService bailiffDirectoryService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey, this::midEvent)
            .pageLabel(PAGE_LABEL)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getBailiffAllocationDetails)
            .readonly(BailiffAllocationDetails::getBailiffAllocationCaseName, NEVER_SHOW, true)
            .label(pageKey + "-parties",
                   "<p class=\"govuk-!-font-size-24 govuk-!-margin-top-0\">${bailiffAllocationCaseName}</p>")
            .label(pageKey + "-line-separator", "---")
            .mandatoryWithDisplayContextParameter(BailiffAllocationDetails::getAllocatedBailiff, null,
                                                  STAFF_USER_SEARCH)
            .readonly(BailiffAllocationDetails::getAllocatedBailiffEmail, "allocatedBailiffEmail!=\"\"")
            .done()
            .done();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

    AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                          CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();
        BailiffAllocationDetails allocation = allocationOf(caseData);
        String userId = chosenUserId(allocation);
        if (userId == null) {
            return errors(CHOOSE_A_BAILIFF_ERROR);
        }
        Optional<BailiffUser> bailiff = bailiffDirectoryService.findBailiff(userId);
        if (bailiff.isEmpty()) {
            return errors(NOT_A_BAILIFF_ERROR);
        }
        allocation.setAllocatedBailiffEmail(bailiff.get().email());
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData).build();
    }

    /**
     * The IDAM id of the user chosen in the type-ahead, or null if none has been chosen.
     */
    public static String chosenUserId(BailiffAllocationDetails allocation) {
        StaffUser chosen = allocation == null ? null : allocation.getAllocatedBailiff();
        String userId = chosen == null ? null : chosen.getIdamId();
        return userId == null || userId.isBlank() ? null : userId;
    }

    private static BailiffAllocationDetails allocationOf(PCSCase caseData) {
        if (caseData.getEnforcementOrder() == null) {
            caseData.setEnforcementOrder(new EnforcementOrder());
        }
        EnforcementOrder enforcementOrder = caseData.getEnforcementOrder();
        if (enforcementOrder.getBailiffAllocationDetails() == null) {
            enforcementOrder.setBailiffAllocationDetails(new BailiffAllocationDetails());
        }
        return enforcementOrder.getBailiffAllocationDetails();
    }

    private static AboutToStartOrSubmitResponse<PCSCase, State> errors(String error) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().errors(List.of(error)).build();
    }
}
