package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalTabAccess;

/**
 * Read only access to saved bailiff notes for internal users, bailiffs included. Claimants and defendants
 * get none: bailiff notes are internal. The notes are added through the "Add a bailiff note" event, which
 * grants its own access to the note it captures, so nobody edits a saved note.
 */
public class BailiffNoteReadAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        return new InternalTabAccess().getGrants();
    }
}
