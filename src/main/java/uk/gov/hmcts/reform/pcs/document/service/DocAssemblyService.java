package uk.gov.hmcts.reform.pcs.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyRequest;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.docassembly.exception.DocumentGenerationFailedException;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.document.model.FormPayloadObj;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.Map;

@Slf4j
@Service
public class DocAssemblyService {
    private final DocAssemblyClient docAssemblyClient;
    private final IdamService idamService;
    private final AuthTokenGenerator authTokenGenerator;

    public DocAssemblyService(
        DocAssemblyClient docAssemblyClient,
        IdamService idamService,
        AuthTokenGenerator authTokenGenerator
    ) {
        this.docAssemblyClient = docAssemblyClient;
        this.idamService = idamService;
        this.authTokenGenerator = authTokenGenerator;
    }

    public String generateDocument(DocAssemblyRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Request cannot be null");
            }

            String authorization = idamService.getSystemUserAuthorisation();
            String serviceAuthorization = authTokenGenerator.generate();

            // Convert FormPayload to FormPayloadObj
            FormPayloadObj formPayloadObj = convertToFormPayloadObj(request.getFormPayload());

            // docAssemblyRequest with meta
            DocAssemblyRequest assemblyRequest = DocAssemblyRequest.builder()
                .templateId(request.getTemplateId() != null
                    ? request.getTemplateId() : "CV-SPC-CLM-ENG-01356.docx")
                .outputType(request.getOutputType() != null
                    ? request.getOutputType() : OutputType.PDF)
                .formPayload(formPayloadObj)
                .outputFilename(request.getOutputFilename())
                .caseTypeId(request.getCaseTypeId() != null
                    ? request.getCaseTypeId() : CaseType.getCaseType())
                .jurisdictionId(request.getJurisdictionId() != null
                    ? request.getJurisdictionId() : CaseType.getJurisdictionId())
                .secureDocStoreEnabled(true)
                .build();

            DocAssemblyResponse response = docAssemblyClient.generateOrder(
                authorization,
                serviceAuthorization,
                assemblyRequest
            );

            // Extract document URL directly from response object
            String documentUrl = response.getRenditionOutputLocation();
            if (documentUrl == null || documentUrl.isEmpty()) {
                log.error("No or empty renditionOutputLocation found in Doc Assembly response");
                throw new DocAssemblyException("No document URL returned from Doc Assembly service");
            }
            log.info("Document generated successfully. URL: {}", documentUrl);
            return documentUrl;

        } catch (DocumentGenerationFailedException e) {
            // This is the exception thrown by DocAssemblyClient.generateOrder()
            log.error("Document generation failed: {}", e.getMessage(), e);
            throw new DocAssemblyException("Document generation failed", e);
        } catch (DocAssemblyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error occurred during document generation", e);
            throw new DocAssemblyException("Unexpected error occurred during document generation", e);
        }
    }

    /**
     * Converts FormPayload to FormPayloadObj for proper deserialization
     * Handles both Map and FormPayload types
     */
    private FormPayloadObj convertToFormPayloadObj(Object formPayload) {
        if (formPayload == null) {
            return new FormPayloadObj();
        }

        if (formPayload instanceof FormPayloadObj) {
            return (FormPayloadObj) formPayload;
        }

        if (formPayload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = (Map<String, Object>) formPayload;
            FormPayloadObj formPayloadObj = new FormPayloadObj();

            // Map the fields from the Map to FormPayloadObj
            if (payloadMap.containsKey("applicantName")) {
                formPayloadObj.setApplicantName((String) payloadMap.get("applicantName"));
            }
            if (payloadMap.containsKey("caseNumber")) {
                formPayloadObj.setCaseNumber((String) payloadMap.get("caseNumber"));
            }

            return formPayloadObj;
        }

        // For other types, use ObjectMapper to convert
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(formPayload, FormPayloadObj.class);
        } catch (Exception e) {
            log.warn("Failed to convert FormPayload to FormPayloadObj, using empty object", e);
            return new FormPayloadObj();
        }
    }
}
