package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionPageConfigurer implements PageConfigurer {

    private final ExplainHowDefendantsReturnedPage explainHowDefendantsReturnedPage;
    private final VulnerableAdultsChildrenWarrantRestPage vulnerableAdultsChildrenWarrantRestPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new PeopleWhoWillBeEvictedWarrantRestPlaceholder())
            .add(new ShareEvidenceWithJudgePage())
            .add(explainHowDefendantsReturnedPage)
            .add(new DefendantAtPropertyPage())
            .add(vulnerableAdultsChildrenWarrantRestPage)
            .add(new PropertyAccessDetailsWarrantRestPlaceholder());
    }
}
