package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionPageConfigurer implements EnforcementPageConfigurer {

    private final ExplainHowDefendantsReturnedPage explainHowDefendantsReturnedPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new ShareEvidenceWithJudgePage())
            .add(explainHowDefendantsReturnedPage)
            .add(new DefendantAtPropertyPage());
    }
}
