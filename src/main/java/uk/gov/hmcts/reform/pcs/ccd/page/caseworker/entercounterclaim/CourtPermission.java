package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Component
public class CourtPermission implements CcdPageConfiguration {

    private static final String COURT_PERMISSION_GRANTED_FIELD = "enter_counterclaim_CourtPermissionGranted";

    private static final String COURT_PERMISSION_ANSWERED = ShowConditions.or(
        fieldEquals(COURT_PERMISSION_GRANTED_FIELD, VerticalYesNo.YES),
        fieldEquals(COURT_PERMISSION_GRANTED_FIELD, VerticalYesNo.NO)
    );

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("courtPermission")
            .pageLabel("Court permission")
            .label("courtPermission-lineSeparator", "---")
            .label(
                "courtPermission-warning",
                """
                <div class="govuk-warning-text">
                <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                <strong class="govuk-warning-text__text">
                    <span class="govuk-visually-hidden">Warning</span>
                    You should check whether permission is required. If it is, you must advise the party \
                to get permission from the court to make their counterclaim.
                </strong>
                </div>""")
            .complex(PCSCase::getEnterCounterClaim)
                .mandatory(EnterCounterClaimDetails::getCourtPermissionGranted)
                .mandatory(
                    EnterCounterClaimDetails::getPermissionOrderDate,
                    fieldEquals(COURT_PERMISSION_GRANTED_FIELD, VerticalYesNo.YES))
            .done()
            .mandatory(PCSCase::getCounterClaimSubmittingPartyList, COURT_PERMISSION_ANSWERED)
            .complex(PCSCase::getEnterCounterClaim)
                .mandatory(EnterCounterClaimDetails::getClaimReceivedDate, COURT_PERMISSION_ANSWERED)
            .done();
    }
}
