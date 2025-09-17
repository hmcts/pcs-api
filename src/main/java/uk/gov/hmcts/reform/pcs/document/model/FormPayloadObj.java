package uk.gov.hmcts.reform.pcs.document.model;

import lombok.Getter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;


@Getter
@Component
public class FormPayloadObj implements FormPayload {
    private String applicantName;
    //    private FormPayload formPayload;
    private String caseNumber;

    //    {
    //        "templateId": "CV-SPC-CLM-ENG-00001",
    //        "formPayload": {
    //        "applicantName": "John Doe",
    //            "caseNumber": "1234567890"
    //    },
    //        "outputType": "PDF"
    //    }
}
