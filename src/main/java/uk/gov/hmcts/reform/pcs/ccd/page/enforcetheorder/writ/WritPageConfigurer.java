package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;

@Slf4j
@Component
@AllArgsConstructor
public class WritPageConfigurer implements EnforcementPageConfigurer {
    private final LandRegistryFeesWritPage landRegistryFeesWritPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new CannotApplyForWritInfoPage())
            .add(new NameAndAddressForEvictionWritPage())
            .add(new ChangeNameAddressWritPage())
            .add(new ConfirmHCEOfficerPage())
            .add(new HCEOfficerDetailsPage())
            .add(new EnforcementOfficerSelectionPage())
            .add(new MoneyOwedWritPage())
            .add(new LegalCostsWritPage())
            .add(landRegistryFeesWritPage)
            .add(new RepaymentsWritPage())
            .add(new LanguageUsedWritPage())
            .add(new StatementOfTruthPlaceholderWritPage());
    }
}
