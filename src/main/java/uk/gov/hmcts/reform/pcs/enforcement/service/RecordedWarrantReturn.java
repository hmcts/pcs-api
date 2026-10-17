package uk.gov.hmcts.reform.pcs.enforcement.service;

import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;

/**
 * The return a bailiff recorded on a warrant, for its confirmation screen.
 *
 * @param warrantNumber the warrant's number, or null for a warrant approved before warrants were numbered
 */
public record RecordedWarrantReturn(WarrantReturnName returnName, String warrantNumber) {
}
