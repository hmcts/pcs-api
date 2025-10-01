package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.GeneralApplicationEntity;
import uk.gov.hmcts.reform.pcs.entity.PCSCaseEntity;
import uk.gov.hmcts.reform.pcs.entity.Party;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.service.PartyService;

import java.util.List;
import java.util.UUID;

@Component
public class CreateGeneralApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final PartyService partyService;
    private final PcsCaseRepository pcsCaseRepository;
    private final GeneralApplicationRepository generalApplicationRepository;
    private final PartyRepository partyRepository;

    public CreateGeneralApplication(PartyService partyService,
                                    PcsCaseRepository pcsCaseRepository,
                                    GeneralApplicationRepository generalApplicationRepository,
                                    PartyRepository partyRepository) {

        this.partyService = partyService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.generalApplicationRepository = generalApplicationRepository;
        this.partyRepository = partyRepository;
    }

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.createGeneralApplication.name(), this::submit)
            .forAllStates()
            .name("Create general application")
            .showSummary()
            .aboutToStartCallback(this::start)
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
            .fields()
            .page("applicants")
            .mandatory(PCSCase::getApplicantsToAdd)
            .page("summary")
            .mandatory(PCSCase::getGenAppToAdd)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> start(CaseDetails<PCSCase, State> caseDetails) {
        PCSCase pcsCase = caseDetails.getData();

        List<DynamicListElement> oartiesOptionsList = partyService.getAllPartiesAsOptionsList(caseDetails.getId());

        DynamicMultiSelectList dynamicList = DynamicMultiSelectList.builder()
            .listItems(oartiesOptionsList)
            .build();
        pcsCase.setApplicantsToAdd(dynamicList);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        GeneralApplication genAppToAdd = pcsCase.getGenAppToAdd();

        GeneralApplicationEntity generalApplicationEntity = GeneralApplicationEntity.builder()
            .summary(genAppToAdd.getSummary())
            .build();

        List<UUID> applicantUuids = getSelectedItemCodes(pcsCase.getApplicantsToAdd());

        applicantUuids.forEach(
            applicantUuid -> {
                Party partyReference = partyRepository.getReferenceById(applicantUuid);
                generalApplicationEntity.addParty(partyReference);
            }
        );

        long caseReference = eventPayload.caseReference();
        PCSCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException("Case not found for " + caseReference));

        pcsCaseEntity.addGeneralApplication(generalApplicationEntity);
        generalApplicationRepository.save(generalApplicationEntity);
    }

    @NonNull
    private static List<UUID> getSelectedItemCodes(DynamicMultiSelectList multiSelectList) {
        if (multiSelectList != null) {
            return multiSelectList.getValue().stream()
                .map(DynamicListElement::getCode)
                .toList();
        } else {
            return List.of();
        }
    }

}
