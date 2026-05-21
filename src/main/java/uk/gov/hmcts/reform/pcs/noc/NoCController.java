package uk.gov.hmcts.reform.pcs.noc;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/noc")
@AllArgsConstructor
public class NoCController {

    private final NoCService noCService;

    @GetMapping("/noc-questions")
    public Map<String, Object> questions(@RequestParam("case_id") String caseId) {
        return noCService.questions(caseId);
    }

    @PostMapping("/verify-noc-answers")
    public NoCResponse verifyAnswers(@RequestBody NoCRequest request) {
        return noCService.verifyAnswers(request);
    }

    @PostMapping("/noc-requests")
    public NoCResponse submitNoticeOfChange(@RequestBody NoCRequest request) {
        return noCService.submitNoticeOfChange(request);
    }

    @GetMapping("/provider/questions")
    public Map<String, Object> providerQuestions(@RequestParam("case_id") String caseId) {
        return noCService.questions(caseId);
    }

    @PostMapping("/provider/verify")
    public Map<String, Object> providerVerify(@RequestBody NoCRequest request) {
        return noCService.providerVerify(request);
    }

    @PostMapping("/provider/prepare-request")
    public Map<String, Object> providerPrepareRequest(@RequestBody NoCRequest request) {
        return noCService.providerPrepareRequest(request);
    }

    @PostMapping("/provider/apply-decision")
    public Map<String, Object> providerApplyDecision(@RequestBody Map<String, Object> request) {
        return noCService.providerApplyDecision(request);
    }

    @PostMapping("/provider/access-delta-applied")
    public Map<String, Object> providerAccessDeltaApplied(@RequestBody Map<String, Object> request) {
        return noCService.providerAccessDeltaApplied(request);
    }
}
