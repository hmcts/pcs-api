package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.MakeAnApplicationResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppFeeCalculator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.PENDING_GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.GEN_APP_ISSUE;

@Component("genAppSubmitEventHandler")
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final SecurityContextService securityContextService;
    private final GenAppService genAppService;
    private final GenAppRepository genAppRepository;
    private final GenAppDocumentGenerator genAppDocumentGenerator;
    private final GenAppFeeCalculator genAppFeeCalculator;
    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    private final ConfirmationScreenFactory confirmationScreenFactory;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final OrganisationService organisationService;

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

        FeeDetails feeDetails = genAppFeeCalculator.getApplicationFeeDetails(createGenAppRequest)
            .orElse(null);

        boolean paymentRequired = feeDetails != null;

        GenAppState initialState = paymentRequired ? PENDING_GEN_APP_ISSUED : GEN_APP_ISSUED;

        GenAppEntity genAppEntity = genAppService
            .createGenAppEntity(createGenAppRequest, pcsCaseEntity, applicantParty, initialState);

        if (isXuiJourney(createGenAppRequest)) {
            return handleXuiSubmit(caseReference, createGenAppRequest, genAppEntity, feeDetails);
        } else {
            return handleCuiSubmit(paymentRequired, caseReference, genAppEntity, initialState, feeDetails);
        }
    }

    private SubmitResponse<State> handleXuiSubmit(long caseReference,
                                                  GenAppRequest genAppRequest,
                                                  GenAppEntity genAppEntity,
                                                  FeeDetails feeDetails) {

        // TODO: Schedule service request task for ExUI journey (HDPI-6034)
        genAppDocumentGenerator
            .createSubmissionDocument(caseReference, genAppEntity);

        return confirmationScreenFactory
            .buildConfirmationScreenResponse(genAppRequest, caseReference, feeDetails);
    }

    private SubmitResponse<State> handleCuiSubmit(boolean paymentRequired,
                                                  long caseReference,
                                                  GenAppEntity genAppEntity,
                                                  GenAppState initialState,
                                                  FeeDetails feeDetails) {
        if (!paymentRequired) {
            genAppDocumentGenerator
                .createSubmissionDocument(caseReference, genAppEntity);

            notificationService.sendGenAppReceivedEmail(genAppEntity);

            MakeAnApplicationResponse response = MakeAnApplicationResponse.builder()
                .state(initialState)
                .build();

            return SubmitResponse.<State>builder()
                .confirmationBody(toJson(response))
                .build();

        } else {

            String serviceRequestReference = createPaymentServiceRequest(genAppEntity, feeDetails, caseReference);

            MakeAnApplicationResponse response = MakeAnApplicationResponse.builder()
                .state(initialState)
                .serviceRequestReference(serviceRequestReference)
                .feeAmount(feeDetails.getFeeAmount())
                .build();

            return SubmitResponse.<State>builder()
                .confirmationBody(toJson(response))
                .build();
        }
    }

    private String createPaymentServiceRequest(GenAppEntity genAppEntity, FeeDetails feeDetails, long caseReference) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        PartyEntity responsibleParty = partyService.getPartyEntityByIdamId(currentUserId, caseReference);

        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeDetails(feeDetails)
            .ccdCaseNumber(String.valueOf(caseReference))
            .caseReference(caseReference)
            .responsiblePartyId(responsibleParty.getId())
            .paymentCallbackHandlerType(GEN_APP_ISSUE)
            .relatedEntityId(genAppEntity.getId())
            .build();

        PaymentServiceResponse paymentServiceResponse = paymentService.createServiceRequest(taskData);

        return paymentServiceResponse.getServiceRequestReference();
    }

    private PartyEntity getApplicantParty(long caseReference, PCSCase caseData) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        String organisationIdForCurrentUser = organisationService.getOrganisationIdForCurrentUser();

        if (caseData.getCurrentRepresentedPartyId() != null) {
            UUID representedPartyId = UUID.fromString(caseData.getCurrentRepresentedPartyId());
            validateCurrentUserIsLegalRepForParty(organisationIdForCurrentUser, representedPartyId);
            return partyService.getPartyEntityByEntityId(representedPartyId, caseReference);
        } else {
            return partyService.getPartyEntityByIdamId(currentUserId, caseReference);
        }
    }

    private void validateCurrentUserIsLegalRepForParty(String organisationIdForCurrentUser, UUID representedPartyId) {
        boolean isLegalRepForParty = legalRepresentativeOrganisationRepository
            .isRepresentativeOrganisationLinkedToPartyAndActive(organisationIdForCurrentUser, representedPartyId);

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

    @SuppressWarnings("SameParameterValue")
    private static SubmitResponse<State> errorResponse(String errorMessage) {
        return SubmitResponse.<State>builder()
            .errors(List.of(errorMessage))
            .build();
    }

    private static boolean isXuiJourney(GenAppRequest genAppRequest) {
        return genAppRequest instanceof XuiGenAppRequest;
    }

    private String toJson(MakeAnApplicationResponse makeAnApplicationResponse) {
        try {
            return objectMapper.writeValueAsString(makeAnApplicationResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise JSON", e);
        }
    }

}
