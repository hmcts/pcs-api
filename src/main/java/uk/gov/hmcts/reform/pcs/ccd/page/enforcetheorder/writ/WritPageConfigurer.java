package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class WritPageConfigurer implements PageConfigurer {

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new CannotApplyForWritInfoPage())
            .add(new NameAndAddressForEvictionWritPage())
            .add(new ChangeNameAddressWritPage())
            .add(new ConfirmHCEOfficerPage())
            .add(new HCEOfficerDetailsPage())
            .add(new EnforcementOfficerSelectionPage())
            .add(new LanguageUsedWritPage())
            .add(new StatementOfTruthWritPage());
    }
}
