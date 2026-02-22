package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;

@Slf4j
@Component
@AllArgsConstructor
public class WarrantOfRestitutionPageConfigurer implements EnforcementPageConfigurer {

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new PeopleWhoWillBeEvictedWarrantRestitutionPage());
    }
}
