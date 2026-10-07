package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Component
public class CourtPermission implements CcdPageConfiguration {

    private static final String COURT_PERMISSION_GRANTED_FIELD = "enter_cc_CourtPermissionGranted";

    private static final String COURT_PERMISSION_ANSWERED = ShowConditions.or(
        fieldEquals(COURT_PERMISSION_GRANTED_FIELD, VerticalYesNo.YES),
        fieldEquals(COURT_PERMISSION_GRANTED_FIELD, VerticalYesNo.NO)
    );

    private static final String PERMISSION_ORDER_DATE_ERROR =
        "Date the order was made must not be in the future";
    private static final String CLAIM_RECEIVED_DATE_ERROR =
        "Date the counterclaim was received must not be in the future";

    private static final String PARTY_LIST_LABEL = "Which party submitted the counterclaim?";

    private final Clock ukClock;

    public CourtPermission(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("courtPermission", this::midEvent)
            .pageLabel("Court permission")
            .label("courtPermission-lineSeparator", "---")
            .label(
                "courtPermission-warning",
                """
                <div class="govuk-warning-text">
                <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                    You should check whether permission is required. If it is, you must advise the party to get
                    permission from the court to make their counterclaim.
                </strong>
                </div>""",
                COURT_PERMISSION_ANSWERED)
            .complex(PCSCase::getEnterCounterClaim)
                .mandatory(EnterCounterClaimDetails::getCourtPermissionGranted)
                .mandatory(
                    EnterCounterClaimDetails::getPermissionOrderDate,
                    fieldEquals(COURT_PERMISSION_GRANTED_FIELD, VerticalYesNo.YES))
            .done()
            .mandatory(PCSCase::getPartyRadioList, COURT_PERMISSION_ANSWERED, null,PARTY_LIST_LABEL)
            .complex(PCSCase::getEnterCounterClaim)
                .mandatory(EnterCounterClaimDetails::getClaimReceivedDate, COURT_PERMISSION_ANSWERED)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        EnterCounterClaimDetails counterClaimDetails = caseData.getEnterCounterClaim();

        List<String> validationErrors = new ArrayList<>();
        LocalDate currentDate = LocalDate.now(ukClock);

        if (counterClaimDetails != null) {
            LocalDate permissionOrderDate = counterClaimDetails.getPermissionOrderDate();
            boolean permissionGranted = counterClaimDetails.getCourtPermissionGranted() == VerticalYesNo.YES;
            if (permissionGranted && permissionOrderDate != null && permissionOrderDate.isAfter(currentDate)) {
                validationErrors.add(PERMISSION_ORDER_DATE_ERROR);
            }

            LocalDate claimReceivedDate = counterClaimDetails.getClaimReceivedDate();
            if (claimReceivedDate != null && claimReceivedDate.isAfter(currentDate)) {
                validationErrors.add(CLAIM_RECEIVED_DATE_ERROR);
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", validationErrors))
            .build();
    }
}
