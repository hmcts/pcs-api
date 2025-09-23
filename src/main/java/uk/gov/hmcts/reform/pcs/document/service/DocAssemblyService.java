package uk.gov.hmcts.reform.pcs.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyRequest;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.docassembly.exception.DocumentGenerationFailedException;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

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

    public String generateDocument(
            FormPayload formPayload,
            String templateId,
            OutputType outputType,
            String outputFilename
    ) {
        try {
            if (formPayload == null) {
                throw new IllegalArgumentException("FormPayload cannot be null");
            }

            String authorization = idamService.getSystemUserAuthorisation();
            String serviceAuthorization = authTokenGenerator.generate();

            // Build DocAssemblyRequest directly
            DocAssemblyRequest assemblyRequest = DocAssemblyRequest.builder()
                .templateId(templateId)
                .outputType(outputType)
                .formPayload(formPayload)
                .outputFilename(outputFilename)
                .caseTypeId(CaseType.getCaseType())
                .jurisdictionId(CaseType.getJurisdictionId())
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

}
