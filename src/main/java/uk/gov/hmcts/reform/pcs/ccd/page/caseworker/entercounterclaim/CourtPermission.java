package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;

    public CourtPermission(@Qualifier("ukClock") Clock ukClock, PcsCaseService pcsCaseService,
                           PartyService partyService) {
        this.ukClock = ukClock;
        this.pcsCaseService = pcsCaseService;
        this.partyService = partyService;
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

        caseData.setPartyMultiSelectionList(buildAgainstPartyList(details.getId(), caseData));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", validationErrors))
            .build();
    }

    private DynamicMultiSelectStringList buildAgainstPartyList(long caseReference, PCSCase caseData) {
        ClaimEntity mainClaim = pcsCaseService.loadCase(caseReference).getClaims().getFirst();

        DynamicList submittingPartyList = caseData.getPartyRadioList();
        UUID submittingPartyId = submittingPartyList != null ? submittingPartyList.getValueCode() : null;

        List<DynamicListElement> parties = new ArrayList<>();
        parties.addAll(partyService.buildPartyDynamicList(mainClaim, PartyRole.CLAIMANT).getListItems());
        parties.addAll(partyService.buildPartyDynamicList(mainClaim, PartyRole.DEFENDANT).getListItems());

        List<DynamicStringListElement> listItems = parties.stream()
            .filter(element -> !element.getCode().equals(submittingPartyId))
            .map(element -> DynamicStringListElement.builder()
                .code(element.getCode().toString())
                .label(element.getLabel())
                .build())
            .toList();

        return DynamicMultiSelectStringList.builder()
            .value(new ArrayList<>())
            .listItems(listItems)
            .build();
    }
}
