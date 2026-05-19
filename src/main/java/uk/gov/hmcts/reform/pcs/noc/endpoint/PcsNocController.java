package uk.gov.hmcts.reform.pcs.noc.endpoint;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.noc.model.NocAnswersRequest;
import uk.gov.hmcts.reform.pcs.noc.model.NocApprovalStatus;
import uk.gov.hmcts.reform.pcs.noc.model.NocQuestionsResponse;
import uk.gov.hmcts.reform.pcs.noc.service.PcsNocService;

@RestController
@AllArgsConstructor
@RequestMapping("/noc")
@Tag(name = "Notice of Change")
public class PcsNocController {

    private final PcsNocService nocService;

    @GetMapping("/noc-questions")
    public NocQuestionsResponse questions(@RequestParam("case_id") String caseId) {
        return nocService.questions(caseId);
    }

    @PostMapping("/verify-noc-answers")
    public NocApprovalStatus verify(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestBody NocAnswersRequest request
    ) {
        return nocService.verify(request, authorisation);
    }

    @PostMapping("/noc-requests")
    public NocApprovalStatus submit(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestBody NocAnswersRequest request
    ) {
        return nocService.submit(request, authorisation);
    }
}
