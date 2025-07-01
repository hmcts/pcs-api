package uk.gov.hmcts.reform.pcs.testingsupport.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request model for document generation via Doc Assembly API")
public class DocAssemblyRequest {
    
    @Schema(
        description = "Template ID for the document to be generated",
        example = "CV-SPC-CLM-ENG-00001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String templateId;
    
    @Schema(
        description = "Form data payload containing the values to populate the template",
        example = "{\"applicantName\": \"John Doe\", \"caseNumber\": \"1234567890\", \"dateOfBirth\": \"1990-01-01\"}",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Map<String, Object> formPayload;
    
    @Schema(
        description = "Output format for the generated document",
        example = "PDF",
        allowableValues = {"PDF", "DOCX", "DOC"}
    )
    private String outputType;
} 