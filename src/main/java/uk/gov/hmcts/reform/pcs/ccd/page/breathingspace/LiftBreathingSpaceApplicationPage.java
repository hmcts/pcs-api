package uk.gov.hmcts.reform.pcs.ccd.page.breathingspace;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;

@Component
public class LiftBreathingSpaceApplicationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("liftBreathingSpaceApplication")
            .label("liftBreathingSpaceApplication-lineSeparator", "---")
            .complex(PCSCase::getBreathingSpaceDetails)
            .readonly(BreathingSpaceDetails::getTypeDisplay)
            .done();
    }

}
