package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.ChooseAnApplication;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.SelectParty;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativeService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.makeAnApplication;

@Slf4j
@Component
@AllArgsConstructor
public class MakeAnApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final SecurityContextService securityContextService;
    private final GenAppService genAppService;
    private final GenAppRepository genAppRepository;
    private final GenAppDocumentGenerator genAppDocumentGenerator;
    private final DocumentImportService documentImportService;
    private final LegalRepresentativeService legalRepresentativeService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(makeAnApplication.name(), this::submit, this::start)
            .forAllStates() // TODO: Adjust once target states are known and available
            .name("Make a general application")
            .grant(Permission.CRUD, UserRole.DEFENDANT)
            .grant(Permission.CRUD, UserRole.DEFENDANT_SOLICITOR)
            .showSummary();

        new PageBuilder(eventBuilder)
            .add(new SelectParty())
            .add(new ChooseAnApplication());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        // Set represented parties if the current user is a legal rep
        UUID currentUserId = securityContextService.getCurrentUserId();
        legalRepresentativeService.getRepresentedPartiesDynamicList(currentUserId, caseReference)
            .ifPresent(caseData::setRepresentedPartyNames);

        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        PartyEntity applicantParty = getApplicantParty(caseReference, caseData);

        GenAppRequest createGenAppRequest = getGenAppRequest(caseData);

        if (isDuplicateRequest(createGenAppRequest, pcsCaseEntity)) {
            return errorResponse("Application already exists for client reference");
        }

        GenAppEntity genAppEntity = genAppService
            .createGenAppEntity(createGenAppRequest, pcsCaseEntity, applicantParty);

        createSubmissionDocument(caseReference, createGenAppRequest, genAppEntity);

        return SubmitResponse.<State>builder()
            .build();
    }

    private PartyEntity getApplicantParty(long caseReference, PCSCase caseData) {
        if (caseData.getRepresentedPartyNames() != null) {
            DynamicListElement selectedPartyElement = caseData.getRepresentedPartyNames().getValue();
            UUID partyId = selectedPartyElement.getCode();
            return partyService.getPartyEntityByEntityId(partyId, caseReference);
        } else {
            UUID partyIdamId = securityContextService.getCurrentUserId();
            return partyService.getPartyEntityByIdamId(partyIdamId, caseReference);
        }
    }

    // This event can handle requests from ExUI and pcs_frontend, so return the one that
    // was used for the current invocation
    private static GenAppRequest getGenAppRequest(PCSCase caseData) {
        return Optional.ofNullable((GenAppRequest) caseData.getXuiGenAppRequest())
            .filter(xuiGenAppRequest -> xuiGenAppRequest.getApplicationType() != null)
            .orElseGet(caseData::getCitizenGenAppRequest);
    }

    private boolean isDuplicateRequest(GenAppRequest createGenAppRequest, PcsCaseEntity pcsCaseEntity) {
        String clientReference = createGenAppRequest.getClientReference();
        return clientReference != null
            && genAppRepository.existsByPcsCaseAndClientReference(pcsCaseEntity, clientReference);
    }

    private void createSubmissionDocument(long caseReference,
                                          GenAppRequest citizenGenAppRequest,
                                          GenAppEntity genAppEntity) {
        String documentUrl = genAppDocumentGenerator.generateSubmissionDocument(
            caseReference,
            citizenGenAppRequest,
            genAppEntity
        );

        documentImportService.addDocumentToCase(caseReference, documentUrl, CaseFileCategory.APPLICATIONS);
    }

    @SuppressWarnings("SameParameterValue")
    private static SubmitResponse<State> errorResponse(String errorMessage) {
        return SubmitResponse.<State>builder()
            .errors(List.of(errorMessage))
            .build();
    }

}
