package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.generatedocument.GenerateDocument;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.service.DocumentGenerationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.generateDocument;

@Slf4j
@Component
@AllArgsConstructor
public class GenerateDocumentPoc implements CCDConfig<PCSCase, State, UserRole> {

    private final DocumentGenerationService documentGenerationService;
    private final PcsCaseService pcsCaseService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(generateDocument.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Generate Document")
                .description("Generate a document using case data and add it to the case file")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .showSummary();

        new PageBuilder(eventBuilder)
            .add(new ClaimantInformation())
            .add(new GenerateDocument());
    }

    PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        log.info("Starting document generation for case: {}", eventPayload.caseReference());


        if (caseData.getClaimantName() == null) {
            caseData.setClaimantName("");
        }

        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Submitting document generation for case: {}", caseReference);

        try {
            // Extract case data for document generation
            Map<String, Object> formPayload = extractCaseDataForDocument(pcsCase, caseReference);

            String templateId = "CV-CMC-ENG-0010.docx";
            String outputType = "PDF";

            log.info("Generating document with template: {} and output: {}", templateId, outputType);

            uk.gov.hmcts.ccd.sdk.type.Document generatedDocument =
                documentGenerationService.generateDocument(templateId, formPayload, outputType);

            pcsCaseService.addGeneratedDocumentToCase(caseReference, generatedDocument);

            log.info("Successfully generated and added document to case: {}", caseReference);

        } catch (Exception e) {
            log.error("Failed to generate document for case: {}", caseReference, e);
            throw new RuntimeException("Document generation failed: " + e.getMessage(), e);
        }
    }


    Map<String, Object> extractCaseDataForDocument(PCSCase pcsCase, long caseReference) {
        Map<String, Object> formPayload = new HashMap<>();

        // Add case reference
        formPayload.put("caseNumber", String.valueOf(caseReference));

        // Add claimant information from user input
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
