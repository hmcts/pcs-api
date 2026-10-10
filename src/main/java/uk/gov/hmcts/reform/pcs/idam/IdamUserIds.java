package uk.gov.hmcts.reform.pcs.idam;

import java.util.Locale;

public final class IdamUserIds {

    private IdamUserIds() {
    }

    // IDAM uids are opaque; legacy accounts have numeric ones. Lower-cased to match ids stored as uuid.
    public static String normalise(String uid) {
        return uid != null ? uid.toLowerCase(Locale.ROOT) : null;
    }
}
