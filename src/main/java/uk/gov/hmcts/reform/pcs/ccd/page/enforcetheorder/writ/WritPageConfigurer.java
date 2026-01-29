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
    private final HCEOfficerDetailsPage hceOfficerDetailsPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new NameAndAddressForEvictionWritPage())
            .add(new ChangeNameAddressWritPage())
            .add(new ConfirmHCEOfficerPage())
            .add(hceOfficerDetailsPage)
            .add(new EnforcementOfficerSelectionPage())
            .add(new AmountDefendantOwesPage())
            .add(new LegalCostsWritPage())
            .add(new LandRegistryFeesPage());
    }
}
