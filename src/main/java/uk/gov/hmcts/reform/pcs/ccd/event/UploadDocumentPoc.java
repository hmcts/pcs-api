package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.uploadsupportingdocs.DocumentUpload;
import uk.gov.hmcts.reform.pcs.ccd.page.generatedocument.GenerateDocument;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.DocumentGenerationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;

import uk.gov.hmcts.ccd.sdk.type.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.uploadDocumentPoc;

@Slf4j
@Component
@AllArgsConstructor
public class UploadDocumentPoc implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final DocumentGenerationService documentGenerationService;

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

        // Debug: Check what documents we have before
        log.info("DEBUG: Before processing - supportingDocuments: {}",
                 pcsCase.getSupportingDocuments() != null ? pcsCase.getSupportingDocuments().size() : "null");
        log.info("DEBUG: Before processing - generatedDocuments: {}",
                 pcsCase.getGeneratedDocuments() != null ? pcsCase.getGeneratedDocuments().size() : "null");

        if (pcsCase.getGeneratedDocuments() == null) {
            pcsCase.setGeneratedDocuments(new ArrayList<>());
        }

        try {
            Map<String, Object> formPayload = extractCaseDataForDocument(pcsCase, caseReference);

            String templateId = "CV-CMC-ENG-0010.docx";
            String outputType = "PDF";

            Document generatedDocument =
                documentGenerationService.generateDocument(templateId, formPayload, outputType);

            // Add to generatedDocuments (not supportingDocuments)
            pcsCase.getGeneratedDocuments().add(
                documentGenerationService.createDocumentListValue(generatedDocument)
            );

            // Debug: Check what documents we have after
            log.info("DEBUG: After adding generated - supportingDocuments: {}",
                     pcsCase.getSupportingDocuments() != null ? pcsCase.getSupportingDocuments().size() : "null");
            log.info("DEBUG: After adding generated - generatedDocuments: {}",
                     pcsCase.getGeneratedDocuments() != null ? pcsCase.getGeneratedDocuments().size() : "null");

        } catch (Exception e) {
            log.error("Failed to generate document for case: {}", caseReference, e);
        }

        PcsCaseEntity pcsCaseEntity = pcsCaseService.createCase(caseReference, pcsCase);

        log.info("DEBUG: Final check before return - generatedDocuments: {}",
                 pcsCase.getGeneratedDocuments() != null ? pcsCase.getGeneratedDocuments().size() : "null");
        if (pcsCase.getGeneratedDocuments() != null && !pcsCase.getGeneratedDocuments().isEmpty()) {
            pcsCase.getGeneratedDocuments().forEach(doc ->
                                                        log.info("DEBUG: Generated doc ID: {}, filename: {}",
                                                                 doc.getId(),
                                                                 doc.getValue() != null ? doc.getValue().getFilename() : "null"));
        }

        return pcsCase;
    }

    private Map<String, Object> extractCaseDataForDocument(PCSCase pcsCase, long caseReference) {
        Map<String, Object> formPayload = new HashMap<>();

        formPayload.put("caseNumber", String.valueOf(caseReference));

        String claimantName = pcsCase.getClaimantName();
        if (claimantName != null && !claimantName.trim().isEmpty()) {
            formPayload.put("applicantName", claimantName);
        } else {
            // Fallback
            String overriddenName = pcsCase.getOverriddenClaimantName();
            if (overriddenName != null && !overriddenName.trim().isEmpty()) {
                formPayload.put("applicantName", overriddenName);
            } else {
                formPayload.put("applicantName", "Not Specified");
            }
        }

        if (pcsCase.getPropertyAddress() != null) {
            formPayload.put("propertyAddress", pcsCase.getPropertyAddress().getAddressLine1());
            formPayload.put("postCode", pcsCase.getPropertyAddress().getPostCode());
        }

        formPayload.put("dateOfBirth", "1990-01-01");

        formPayload.put("documentType", "Case Summary");
        formPayload.put("generatedAt", java.time.LocalDate.now().toString());

        return formPayload;
    }
}
