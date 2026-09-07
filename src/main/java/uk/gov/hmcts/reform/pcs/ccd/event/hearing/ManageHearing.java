package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.CaseLocation;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingSummaryRenderer;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.manageHearing;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@Component
@Slf4j
public class ManageHearing implements CCDConfig<PCSCase, State, UserRole> {

    private final ManageHearingConfigurer manageHearingConfigurer;
    private final HearingService hearingService;
    private final LocationReferenceService locationReferenceService;
    private final PcsCaseService pcsCaseService;
    private final HearingSummaryRenderer hearingSummaryRenderer;
    private final ConfirmationBodyRenderer confirmationBodyRenderer;

    public ManageHearing(ManageHearingConfigurer manageHearingConfigurer,
                         HearingService hearingService,
                         LocationReferenceService locationReferenceService,
                         PcsCaseService pcsCaseService,
                         HearingSummaryRenderer hearingSummaryRenderer,
                         ConfirmationBodyRenderer confirmationBodyRenderer) {

        this.manageHearingConfigurer = manageHearingConfigurer;
        this.hearingService = hearingService;
        this.locationReferenceService = locationReferenceService;
        this.pcsCaseService = pcsCaseService;
        this.hearingSummaryRenderer = hearingSummaryRenderer;
        this.confirmationBodyRenderer = confirmationBodyRenderer;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder.decentralisedEvent(manageHearing.name(), this::submit, this::start)
                .forStates(State.AWAITING_SUBMISSION_TO_HMCTS, State.PENDING_CASE_ISSUED, State.CASE_ISSUED)
                .name("Manage hearing")
                .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_3, CASEWORKER_EVENTS))
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
        setHearingLocation(eventPayload, pcsCase);

        Optional<HearingEntity> editableHearing = hearingService.findEditableHearing(pcsCaseEntity);

        if (editableHearing.isEmpty()) {
            hearingService.clearHearingForm(pcsCase);
            pcsCase.setShowManageHearingPage(VerticalYesNo.NO);
            pcsCase.setManageHearingOption(ManageHearingOption.ADD);
            pcsCase.setSelectedHearingId(null);
        } else {
            HearingEntity nextHearingEntity = editableHearing.get();
            String hearingLocation = pcsCase.getHearingLocation();
            Hearing hearing = pcsCase.getHearing() == null ? Hearing.builder().build() : pcsCase.getHearing();
            hearing.setHearingId(nextHearingEntity.getId());
            hearing.setHearingSummaryMarkdown(
                hearingSummaryRenderer.renderMarkdown(nextHearingEntity, hearingLocation));
            pcsCase.setHearing(hearing);
            pcsCase.setSelectedHearingId(null);
            pcsCase.setShowManageHearingPage(VerticalYesNo.YES);
        }

        return pcsCase;
    }

    private void setHearingLocation(EventPayload<PCSCase, State> eventPayload, PCSCase pcsCase) {
        CaseLocation caseManagementLocation = pcsCase.getCaseManagementLocation();

        if (caseManagementLocation == null) {
            log.warn("Unable to find hearing location for case {}:", eventPayload.caseReference());
            pcsCase.setHearingLocation("Unable to find hearing location");
            return;
        }

        List<Integer> baseLocation = List.of(Integer.parseInt(caseManagementLocation.getBaseLocation()));

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
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        if (caseData.getShowManageHearingPage() != VerticalYesNo.YES) {
            caseData.setManageHearingOption(ManageHearingOption.ADD);
        }

        String confirmationBody = "";
        switch (caseData.getManageHearingOption()) {
            case ADD: {
                hearingService.addHearing(caseReference, caseData);
                confirmationBody = confirmationBodyRenderer
                    .renderHearingAddedConfirmationBody(caseData, caseReference);
                break;
            }
            case EDIT: {
                hearingService.updateHearing(caseReference, caseData);
                confirmationBody = confirmationBodyRenderer
                    .renderHearingEditedConfirmationBody(caseData, caseReference);
                break;
            }
            case CANCEL: {
                hearingService.cancelHearing(caseData.getHearing());
                confirmationBody = confirmationBodyRenderer
                    .renderHearingCancelledConfirmationBody(caseData, caseReference);
                break;
            }
        }

        return SubmitResponse.<State>builder()
            .confirmationBody(confirmationBody)
            .build();
    }

}
