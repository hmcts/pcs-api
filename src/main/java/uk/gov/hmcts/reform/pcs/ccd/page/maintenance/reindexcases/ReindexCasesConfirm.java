package uk.gov.hmcts.reform.pcs.ccd.page.maintenance.reindexcases;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.maintenance.ReindexCasesDetails;

@RequiredArgsConstructor
public class ReindexCasesConfirm implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("reindexCasesConfirm")
            .pageLabel("Confirm reindex")
            .label("reindexCasesConfirm-info", """
                        ---
                        Review the estimated count of affected cases before submitting.
                        """)
            .complex(PCSCase::getReindexCases)
            .readonly(ReindexCasesDetails::getModifiedSince)
            .readonly(ReindexCasesDetails::getMatchingCount)
            .done();
    }

}
