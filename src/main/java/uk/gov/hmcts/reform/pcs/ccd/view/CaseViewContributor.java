package uk.gov.hmcts.reform.pcs.ccd.view;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

/**
 * Extension point for modules that add their own fields to the PCS case view.
 * Contributors run after the core case fields and markdown have been set, and before
 * the search fields and case access groups are derived.
 */
public interface CaseViewContributor {

    void contribute(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference, State state);

}
