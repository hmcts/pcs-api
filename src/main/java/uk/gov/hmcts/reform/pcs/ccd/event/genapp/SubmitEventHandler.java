package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component("genAppSubmitEventHandler")
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final SecurityContextService securityContextService;
    private final GenAppService genAppService;
    private final GenAppRepository genAppRepository;
    private final GenAppDocumentGenerator genAppDocumentGenerator;
    private final DocumentImportService documentImportService;
    private final LegalRepresentativeRepository legalRepresentativeRepository;
    private final ConfirmationScreenFactory confirmationScreenFactory;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
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

        createSubmissionDocument(caseReference, createGenAppRequest, genAppEntity, applicantParty);

        return confirmationScreenFactory.buildConfirmationScreenResponse(createGenAppRequest, caseReference);
    }

    private PartyEntity getApplicantParty(long caseReference, PCSCase caseData) {
        UUID currentUserId = securityContextService.getCurrentUserId();

        if (caseData.getCurrentRepresentedPartyId() != null) {
            UUID representedPartyId = UUID.fromString(caseData.getCurrentRepresentedPartyId());
            validateCurrentUserIsLegalRepForParty(currentUserId, representedPartyId);
            return partyService.getPartyEntityByEntityId(representedPartyId, caseReference);
        } else {
            return partyService.getPartyEntityByIdamId(currentUserId, caseReference);
        }
    }

    private void validateCurrentUserIsLegalRepForParty(UUID currentUserId, UUID representedPartyId) {
        boolean isLegalRepForParty = legalRepresentativeRepository
            .isLegalRepresentativeLinkedToPartyAndActive(currentUserId, representedPartyId);

        if (!isLegalRepForParty) {
            throw new PartyNotFoundException("No matching party found represented by current user");
        }
    }

    // This event can handle requests from ExUI and pcs_frontend, so return the one that
    // was used for the current invocation
    private static GenAppRequest getGenAppRequest(PCSCase caseData) {
        return Optional.ofNullable((GenAppRequest) caseData.getXuiGenAppRequest())
            .filter(xuiGenAppRequest -> xuiGenAppRequest.getApplicationType() != null)
            .orElseGet(caseData::getCitizenGenAppRequest);
    }

    private boolean isDuplicateRequest(GenAppRequest genAppRequest, PcsCaseEntity pcsCaseEntity) {
        String clientReference = genAppRequest.getClientReference();
        return clientReference != null
            && genAppRepository.existsByPcsCaseAndClientReference(pcsCaseEntity, clientReference);
    }

    private void createSubmissionDocument(long caseReference,
                                          GenAppRequest genAppRequest,
                                          GenAppEntity genAppEntity,
                                          PartyEntity applicantParty) {

        String documentUrl = genAppDocumentGenerator.generateSubmissionDocument(
            caseReference,
            genAppRequest,
            genAppEntity,
            applicantParty
        );

        DocumentEntity importedDocumentEntity = documentImportService.addDocumentToCase(
            caseReference,
            documentUrl,
            CaseFileCategory.APPLICATIONS
        );

        importedDocumentEntity.setGeneralApplication(genAppEntity);
        genAppEntity.setSubmissionDocument(importedDocumentEntity);
    }

    @SuppressWarnings("SameParameterValue")
    private static SubmitResponse<State> errorResponse(String errorMessage) {
        return SubmitResponse.<State>builder()
            .errors(List.of(errorMessage))
            .build();
    }

}
