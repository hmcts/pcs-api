package uk.gov.hmcts.reform.pcs.noc;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@RequestMapping("/noc")
@RequiredArgsConstructor
public class NocController {

    private final NocService nocService;

    @GetMapping("/noc-questions")
    public ResponseEntity<NocQuestionsResponse> getQuestions(@RequestParam("case_id") long caseId) {
        return ResponseEntity.ok(nocService.getQuestions(caseId));
    }

    @PostMapping("/verify-noc-answers")
    public ResponseEntity<Boolean> verifyAnswers(@RequestBody NocAnswersRequest request) {
        return ResponseEntity.ok(nocService.verifyAnswers(request));
    }

    @PostMapping("/noc-requests")
    public ResponseEntity<NocSubmissionResponse> submit(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestBody NocAnswersRequest request
    ) {
        return ResponseEntity.ok(nocService.submit(authorisation, request));
    }
}
