package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionPageConfigurer implements EnforcementPageConfigurer {

    private final PropertyAccessDetailsWarrantOfRestitutionPage propertyAccessDetailsWarrantOfRestitutionPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new PeopleWhoWillBeEvictedWarrantRestitutionPlaceholder())
            .add(propertyAccessDetailsWarrantOfRestitutionPage)
            .add(new AnythingElseToHelpTheEvictionPlaceholder());
    }
}
