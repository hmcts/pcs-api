package uk.gov.hmcts.reform.pcs.ccd.page.deletecase;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

import java.util.List;

@AllArgsConstructor
@Component
public class DeleteThisCasePage implements CcdPageConfiguration, CcdPage {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();

        pageBuilder
            .page(pageKey, this::midEvent)
            .pageLabel("Delete this case")
            .label(pageKey + "-separator", "---")
            .mandatory(PCSCase::getDeleteUnsubmittedClaim);
    }

    // The event carries a TTLIncrement, so submitting it always stamps the retention TTL.
    // Block anything but an explicit Yes from reaching submission.
    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        if (!YesOrNo.YES.equals(details.getData().getDeleteUnsubmittedClaim())) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of("Select Yes to delete this case, or cancel to keep it"))
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

}
