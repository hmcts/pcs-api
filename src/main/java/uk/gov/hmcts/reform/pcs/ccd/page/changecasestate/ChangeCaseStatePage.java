package uk.gov.hmcts.reform.pcs.ccd.page.changecasestate;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;


@AllArgsConstructor
@Component
public class ChangeCaseStatePage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("changeCaseStatePage")
            .pageLabel("Case state to change to")
            .label("changeCaseStatePage-lineSeparator", "---")
            .label("changeCaseState-currentState", "Current state: ${[STATE]}")
            .mandatory(PCSCase::getTargetState);
    }

}
