package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyAgreementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePossessionGround;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftEventService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PARTIALLY_CREATED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.continueCaseCreation;

@Component
@AllArgsConstructor
@Slf4j
public class ContinueCaseCreation implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseRepository pcsCaseRepository;
    private final ModelMapper modelMapper;
    private final DraftEventService draftEventService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(continueCaseCreation.name(), this::submit)
            .forStateTransition(PARTIALLY_CREATED, AWAITING_SUBMISSION_TO_HMCTS)
            .name("Make a claim")
            .showCondition(NEVER_SHOW)
            .grant(Permission.CRUD, UserRole.HOUSING_PROVIDER)
            .showSummary()
            .endButtonLabel("Submit your claim details")
            .fields()
            .page("resumingADraft", this::loadDraftMidEvent)
                .showCondition(ShowConditions.isYes("controlFlags.draftExists"))
                .pageLabel("Resume a draft")
                .label("draftMessage", "You have previously entered some answers in this section. "
                    + "Would you like to resume where you left off?")
                .mandatory(PCSCase::getContinueWithDraft)
            .optional(PCSCase::getControlFlags, NEVER_SHOW)
            .page("tenancyDetails", this::midEvent)
                .pageLabel("Tenancy Details")
                .mandatory(PCSCase::getTenancyAgreementType)
                .mandatory(PCSCase::getTenancyStartDate)
            .page("groundsForPossession", this::midEvent)
                .pageLabel("Grounds for possession")
                .mandatory(PCSCase::getGroundsForPossession)
            .page("actionAlreadyTaken", this::midEvent)
                .pageLabel("Action you've already taken")
                .mandatory(PCSCase::getMediationAttempted)
                .mandatory(PCSCase::getSettlementAttempted)
            .page("mediationAttempted")
                .showCondition(ShowConditions.isYes("mediationAttempted"))
                .pageLabel("You've attempted mediation")
                .label("mediationAttemptedMessage", "You said that you have attempted mediation.")
            .page("mediationNotAttempted")
                .showCondition(ShowConditions.isNo("mediationAttempted"))
                .pageLabel("You have not attempted mediation")
                .label("mediationNotAttemptedMessage", "You said that you have not attempted mediation.")
            .page("rentAmount", this::midEvent)
                .pageLabel("Rent Amount")
                .mandatory(PCSCase::getRentAmountPence)
                .mandatory(PCSCase::getRentFrequency)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> loadDraftMidEvent(CaseDetails<PCSCase, State> details,
                                                                           CaseDetails<PCSCase, State> detailsBefore) {

        long caseReference = details.getId();
        PCSCase caseData = details.getData();

        if (caseData.getContinueWithDraft() == YesOrNo.YES) {
            boolean draftExists = draftEventService.draftExists(caseReference, continueCaseCreation);
            caseData.getControlFlags().setDraftExists(YesOrNo.from(draftExists));

            draftEventService.getDraftEventData(caseReference, continueCaseCreation, EventData.class)
                .ifPresent(draftEventData -> modelMapper.map(draftEventData, caseData));
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        long caseReference = details.getId();
        PCSCase caseData = details.getData();
        EventData eventData = modelMapper.map(caseData, EventData.class);

        draftEventService.saveDraftEventData(caseReference, continueCaseCreation, eventData);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        Set<CasePossessionGround> casePossessionGrounds = getCasePossessionGrounds(pcsCase);

        pcsCaseEntity.setTenancyAgreementType(pcsCase.getTenancyAgreementType());
        pcsCaseEntity.setTenancyStartDate(pcsCase.getTenancyStartDate());
        pcsCaseEntity.replacePossessionGrounds(casePossessionGrounds);
        pcsCaseEntity.setMediationAttempted(nullSafeToBoolean(pcsCase.getMediationAttempted()));
        pcsCaseEntity.setSettlementAttempted(nullSafeToBoolean(pcsCase.getSettlementAttempted()));
        pcsCaseEntity.setRentAmount(parseMoneyString(pcsCase.getRentAmountPence()));
        pcsCaseEntity.setRentFrequency(pcsCase.getRentFrequency());

        pcsCaseRepository.save(pcsCaseEntity);

        draftEventService.deleteDraftEventData(caseReference, continueCaseCreation);
    }

    private static Boolean nullSafeToBoolean(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }

    private BigDecimal parseMoneyString(String moneyStringPence) {
        if (moneyStringPence != null) {
            return new BigDecimal(moneyStringPence).movePointLeft(2);
        } else {
            return null;
        }
    }

    private static Set<CasePossessionGround> getCasePossessionGrounds(PCSCase pcsCase) {
        return pcsCase.getGroundsForPossession().stream()
            .map(groundForPossession -> {
                CasePossessionGround casePossessionGround = new CasePossessionGround();
                casePossessionGround.setCode(groundForPossession);
                return casePossessionGround;
            })
            .collect(Collectors.toSet());
    }

    @Getter
    @Setter
    private static class EventData {
        private List<PossessionGround> groundsForPossession;

        private TenancyAgreementType tenancyAgreementType;

        private LocalDate tenancyStartDate;

        private YesOrNo mediationAttempted;

        private YesOrNo settlementAttempted;

        private String rentAmountPence;

        private RentFrequency rentFrequency;
    }

}
