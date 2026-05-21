package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Component
public class NoticeOfChangeConfig implements CCDConfig<PCSCase, State, UserRole> {

    static final String CHALLENGE_ID = "NoCChallenge";
    static final String CLAIMANT_NAME_QUESTION_ID = "pcsClaimantName";

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.noticeOfChange()
            .challenge(CHALLENGE_ID)
            .question(CLAIMANT_NAME_QUESTION_ID, "Enter the claimant name")
            .answer(UserRole.CLAIMANT_SOLICITOR)
            .field(PCSCase::getNocClaimantName)
            .done();
    }
}
