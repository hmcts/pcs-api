package uk.gov.hmcts.reform.pcs.ccd.view;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

public interface ViewComponent {

    void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity);

}
