package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class WarrantPageConfigurer implements PageConfigurer {

    private final DefendantsDOBPage defendantsDOBPage;
    private final WarrantRiskPagesConfigurer warrantRiskPagesConfigurer;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new BreathingSpacePage())
            .add(new MissedPaymentsPage())
            .add(new CannotApplyForWarrantInfoPage())
            .add(new NameAndAddressForEvictionPage())
            .add(new ChangeNameAddressPage())
            .add(new ConfirmIfDOBKnownPage())
            .add(defendantsDOBPage);
        warrantRiskPagesConfigurer.configurePages(pageBuilder);
        pageBuilder
            .add(new LanguageUsedPage())
            .add(new StatementOfTruthPage());
    }
}
