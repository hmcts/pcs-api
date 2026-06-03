package uk.gov.hmcts.reform.pcs.document.model.claimpack;

import lombok.Builder;
import lombok.Data;

/**
 * Party rendered in the claim pack — covers claimant, defendants, and underlessees/mortgagees.
 *
 * <p><strong>Name vs. address:</strong> "persons unknown" is a <em>name</em>-only state. Address is
 * always known and always populated, even when {@code isPersonsUnknown} is true. The template
 * sections for unnamed defendants (§6.3.4) and unnamed underlessees (§6.3.6) still render the
 * address rows underneath the "Persons unknown" label.</p>
 *
 * <p>{@code isPersonsUnknown} drives the {@code R-DEF1-UNKNOWN} / {@code R-ULN-UNKNOWN} family of
 * template gates (see plan §6.5) — they switch the name block between the named layout and
 * the literal "Persons unknown" text. Defaults to {@code false}; the claimant never sets it true.</p>
 */
@Data
@Builder
public class ClaimPackParty {

    private String firstName;
    private String lastName;
    /**
     * For organisational claimants; takes precedence over firstName/lastName when set.
     */
    private String orgName;
    @Builder.Default
    private boolean isPersonsUnknown = false;
    /**
     * Always populated — see class-level javadoc. Never null in a well-formed case.
     */
    private ClaimPackAddress address;

}
