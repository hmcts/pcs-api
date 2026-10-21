package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.breathingspace;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

@Component
@AllArgsConstructor
public class BreathingSpaceApplicationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("breathingSpaceApplication")
            .label("breathingSpaceApplication-content", "---");
    }

}
