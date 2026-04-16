package uk.gov.hmcts.reform.pcs.noticeofchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.noticeofchange.client.AssignCaseAccessClient;
import uk.gov.hmcts.reform.pcs.noticeofchange.model.AcaRequest;


@Component
@RequiredArgsConstructor
public class NoticeOfChangeApplyService {

    private final IdamService idamService;
    private final AuthTokenGenerator authTokenGenerator;
    private final AssignCaseAccessClient assignCaseAccessClient;
    private final ObjectMapper objectMapper;

    public AboutToStartOrSubmitResponse<PCSCase, State> applyNoticeOfChange(
        CaseDetails<PCSCase, State> details, CaseDetails<PCSCase, State> before) {

        String sysToken = idamService.getSystemUserAuthorisation();
        String s2sToken = authTokenGenerator.generate();

        AcaRequest acaRequest = AcaRequest.builder().caseDetails(details).build();

        AboutToStartOrSubmitCallbackResponse aacResponse =
            assignCaseAccessClient.applyNoticeOfChange(sysToken, s2sToken, acaRequest);

        if (aacResponse.getErrors() != null && !aacResponse.getErrors().isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(aacResponse.getErrors())
                .build();
        }

        // Merge aac's mutated org-policy data into the typed case
        PCSCase updated = objectMapper.convertValue(aacResponse.getData(), PCSCase.class);

        // Optional: any service-specific resets / mirroring
        // e.g. clear stale draft fields the previous solicitor was working on
        // updated.setDraftStatementOfTruth(null);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(updated)
            .build();
    }
}
