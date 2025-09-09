package uk.gov.hmcts.reform.pcs.ccd.event;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.uploadDocumentPoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.generatedocument.GenerateDocument;
import uk.gov.hmcts.reform.pcs.ccd.page.uploadsupportingdocs.DocumentUpload;
import uk.gov.hmcts.reform.pcs.ccd.service.DocumentGenerationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.SendLetterService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

@Slf4j
@Component
@AllArgsConstructor
public class UploadDocumentPoc implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final DocumentGenerationService documentGenerationService;
    private final SendLetterService sendLetterService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(uploadDocumentPoc.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Upload Document - POC")
                .description("Create new case and upload documents")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .showSummary();

        new PageBuilder(eventBuilder)
            .add(new ClaimantInformation())
            .add(new DocumentUpload())
            .add(new GenerateDocument());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        String userDetails = securityContextService.getCurrentUserDetails().getSub();

        caseData.setClaimantName(userDetails);

        return caseData;
    }

    private PCSCase submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);

        if (pcsCase.getGeneratedDocuments() == null) {
            pcsCase.setGeneratedDocuments(new ArrayList<>());
        }

        pcsCaseService.createCase(caseReference, pcsCase);

        if (pcsCase.getGeneratedDocuments() != null && !pcsCase.getGeneratedDocuments().isEmpty()) {
            pcsCase.getGeneratedDocuments().forEach(doc ->
                log.info("DEBUG: Generated doc ID: {}, filename: {}",
                         doc.getId(),
                         doc.getValue() != null
                             ? doc.getValue().getFilename() : "null"));
        }

        try {
            log.error(("Binary URL of Event payload: "
                + eventPayload.caseData().getSupportingDocuments().getFirst().getValue().getBinaryUrl()));

            String fullUrl = eventPayload.caseData().getSupportingDocuments().getFirst().getValue().getBinaryUrl();
            String[] urlArray = fullUrl.split("/");
            String documentId = urlArray[urlArray.length - 2];
            log.error("Extracted ID: " + documentId);

            sendLetterService.sendLetterv2(documentId);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return pcsCase;
    }

    private Map<String, Object> extractCaseDataForDocument(PCSCase pcsCase, long caseReference) {
        Map<String, Object> formPayload = new HashMap<>();

        formPayload.put("referenceNumber", String.valueOf(caseReference));

        String claimantName = pcsCase.getClaimantName();
        if (claimantName != null && !claimantName.trim().isEmpty()) {
            formPayload.put("applicantName", claimantName);
        } else {
            // Fallback
            String overriddenName = pcsCase.getOverriddenClaimantName();
            if (overriddenName != null && !overriddenName.trim().isEmpty()) {
                formPayload.put("claimant.partyName", overriddenName);
            } else {
                formPayload.put("claimant.partyName", "Not Specified");
            }
        }

        if (pcsCase.getPropertyAddress() != null) {
            formPayload.put("propertyAddress", pcsCase.getPropertyAddress().getAddressLine1());
            formPayload.put("postCode", pcsCase.getPropertyAddress().getPostCode());
        }

        formPayload.put("documentType", "Case Summary");
        formPayload.put("currentDate", java.time.LocalDate.now().toString());

        return formPayload;
    }
}
