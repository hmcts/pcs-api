package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.manageHearing;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;

@Component
@Slf4j
public class ManageHearing implements CCDConfig<PCSCase, State, UserRole> {

    private final ManageHearingConfigurer manageHearingConfigurer;
    private final AddressFormatter addressFormatter;
    private final HearingService hearingService;
    private final LocationReferenceService locationReferenceService;
    private final PcsCaseService pcsCaseService;

    public ManageHearing(ManageHearingConfigurer manageHearingConfigurer,
                         AddressFormatter addressFormatter,
                         HearingService hearingService,
                         LocationReferenceService locationReferenceService,
                         PcsCaseService pcsCaseService) {
        this.manageHearingConfigurer = manageHearingConfigurer;
        this.addressFormatter = addressFormatter;
        this.hearingService = hearingService;
        this.locationReferenceService = locationReferenceService;
        this.pcsCaseService = pcsCaseService;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder.decentralisedEvent(manageHearing.name(), this::submit, this::start)
                .forStates(State.AWAITING_SUBMISSION_TO_HMCTS, State.PENDING_CASE_ISSUED, State.CASE_ISSUED)
                .name("Manage hearing")
                .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_2, CASEWORKER_EVENTS))
                .grant(Permission.CRUD, CASEWORKER_ROLES)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary()
                .endButtonLabel("Submit");

        manageHearingConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        pcsCase.setPartyMultiSelectionList(hearingService.buildPartyList(pcsCaseEntity));

        List<Integer> baseLocation = List.of(Integer.parseInt(pcsCase.getCaseManagementLocation().getBaseLocation()));

        try {
            List<CourtVenue> courtVenues = locationReferenceService.getCourtVenues(baseLocation);

            if (!CollectionUtils.isEmpty(courtVenues)) {
                CourtVenue courtVenue = courtVenues.getFirst();
                pcsCase.setHearingLocation(courtVenue.courtName());
            } else {
                log.warn("Unable to find hearing location for case {}:", eventPayload.caseReference());
                pcsCase.setHearingLocation("Unable to find hearing location");
            }
        } catch (Exception e) {
            log.warn("Unable to fetch hearing location for case {}:", eventPayload.caseReference(), e);
            pcsCase.setHearingLocation("Unable to find hearing location");
        }

        if (CollectionUtils.isEmpty(pcsCase.getHearingList())) {
            hearingService.clearHearingForm(pcsCase);
            pcsCase.setShowManageHearingPage(VerticalYesNo.NO);
            pcsCase.setManageHearingOption(ManageHearingOption.ADD);
            pcsCase.setSelectedHearingId(null);
        } else {
            pcsCase.setShowManageHearingPage(VerticalYesNo.YES);
        }

        return pcsCase;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseId = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        String address = addressFormatter
            .formatMediumAddress(caseData.getPropertyAddress(), AddressFormatter.COMMA_DELIMITER);

        if (
            caseData.getManageHearingOption() == ManageHearingOption.ADD
                || caseData.getShowManageHearingPage() != VerticalYesNo.YES
        ) {
            hearingService.addHearing(caseId, caseData);
        } else if (caseData.getManageHearingOption() == ManageHearingOption.EDIT) {
            hearingService.updateHearing(caseId, caseData);
        }

        return SubmitResponse.<State>builder()
            .confirmationBody(getConfirmationBody(caseId, address, caseData))
            .build();
    }

    private String getConfirmationBody(Long caseId, String address, PCSCase caseData) {
        if (caseData.getManageHearingOption() == ManageHearingOption.EDIT) {
            return """
                ---
                <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
                <span class="govuk-panel__title govuk-!-font-size-32">Hearing edited</span><br>
                <span class="govuk-panel__body govuk-!-font-size-24">Case number #%s</span><br>
                <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
                <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
                </div>

                <h3>What happens next</h3>

                A hearing notice will be issued if you specified one is needed.
                """.formatted(caseId, address, caseData.getCaseNameHmctsInternal());
        }

        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-32">Hearing added</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">Case number #%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
            </div>

            <h3>What happens next</h3>

            A hearing notice will be issued if you specified one is needed.
            """.formatted(caseId, address, caseData.getCaseNameHmctsInternal());
    }

}
