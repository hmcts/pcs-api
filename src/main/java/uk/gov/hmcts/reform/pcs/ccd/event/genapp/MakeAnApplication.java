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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.AppliedForHelpWithFees;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.ChooseAnApplication;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.DocumentUploadWanted;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.HearingInNext14Days;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.HelpWithFeesNeeded;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.MustApplyForHelpWithFees;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.OtherPartiesAgreed;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.ReasonsNotToShare;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.SelectParty;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.StartAdjourn;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.StartSetAside;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.StartSomethingElse;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.UploadSupportingDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.WhatOrderWanted;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.WhichLanguage;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
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
    private final FeeApplier feeApplier;
    private final NotificationService notificationService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(makeAnApplication.name(), this::submit, this::start)
            .forAllStates() // TODO: Adjust once target states are known and available
            .name("Make an application")
            .grant(Permission.CRUD, UserRole.DEFENDANT)
            .grant(Permission.CRUD, UserRole.DEFENDANT_SOLICITOR)
            .showSummary();

        new PageBuilder(eventBuilder)
            .add(new ChooseAnApplication())
            .add(new StartAdjourn())
            .add(new StartSetAside())
            .add(new StartSomethingElse())
            .add(new SelectParty())
            .add(new HearingInNext14Days())
            .add(new HelpWithFeesNeeded())
            .add(new AppliedForHelpWithFees())
            .add(new MustApplyForHelpWithFees())
            .add(new OtherPartiesAgreed())
            .add(new ReasonsNotToShare())
            .add(new WhatOrderWanted())
            .add(new DocumentUploadWanted())
            .add(new UploadSupportingDocuments())
            .add(new WhichLanguage())
            .add(new StatementOfTruth());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        setRepresentedParties(caseReference, caseData);

        applyApplicationFeeAmounts(caseData);

        caseData.getXuiGenAppRequest().setShowHwfScreens(VerticalYesNo.YES);

        return caseData;
    }

    // Set represented parties if the current user is a legal rep
    private void setRepresentedParties(long caseReference, PCSCase caseData) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        legalRepresentativeService.getRepresentedPartiesDynamicList(currentUserId, caseReference)
            .ifPresent(representedPartyNames -> {
                boolean representingMultipleParties = representedPartyNames.getListItems().size() > 1;
                caseData.setMultipleRepresentedParties(VerticalYesNo.from(representingMultipleParties));
                caseData.setRepresentedPartyNames(representedPartyNames);
                if (representedPartyNames.getListItems().size() == 1) {
                    UUID soleRepresentedParty = representedPartyNames.getListItems().getFirst().getCode();
                    caseData.setCurrentRepresentedPartyId(soleRepresentedParty.toString());
                }
            });
    }

    private void applyApplicationFeeAmounts(PCSCase caseData) {

        feeApplier.applyFeeAmount(
            caseData,
            FeeType.GEN_APP_STANDARD_FEE,
            (suppliedCaseData, feeString) -> suppliedCaseData.getXuiGenAppRequest().setStandardFee(feeString)
        );

        feeApplier.applyFeeAmount(
            caseData,
            FeeType.GEN_APP_MAX_FEE,
            (suppliedCaseData, feeString) -> suppliedCaseData.getXuiGenAppRequest().setMaxFee(feeString)
        );
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

        createSubmissionDocument(caseReference, createGenAppRequest, genAppEntity, applicantParty);

        sendNotificationEmail(genAppEntity, caseData);

        return SubmitResponse.<State>builder()
            .build();
    }

    private PartyEntity getApplicantParty(long caseReference, PCSCase caseData) {
        if (isSubmittedByLegalRep(caseData)) {
            UUID partyId = UUID.fromString(caseData.getCurrentRepresentedPartyId());
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

        DocumentEntity documentEntity = documentImportService
            .addDocumentToCase(caseReference, documentUrl, CaseFileCategory.APPLICATIONS);

        genAppEntity.setSubmissionDocument(documentEntity);
    }

    private void sendNotificationEmail(GenAppEntity genAppEntity, PCSCase caseData) {
        if (!isSubmittedByLegalRep(caseData)) {
            notificationService.sendGenAppReceivedEmail(genAppEntity);
        }
    }

    private static boolean isSubmittedByLegalRep(PCSCase caseData) {
        return caseData.getCurrentRepresentedPartyId() != null;
    }

    @SuppressWarnings("SameParameterValue")
    private static SubmitResponse<State> errorResponse(String errorMessage) {
        return SubmitResponse.<State>builder()
            .errors(List.of(errorMessage))
            .build();
    }

}
