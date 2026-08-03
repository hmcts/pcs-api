package uk.gov.hmcts.reform.pcs.document.model.claimform;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Party rendered in the claim form: claimant, defendants, and underlessees or mortgagees.
 *
 * <p>"Persons unknown" is a name-only state. The address is always known and populated, even when
 * {@code isPersonsUnknown} is true, so the unnamed-defendant and unnamed-underlessee sections
 * still render the address rows under the "Persons unknown" label.</p>
 *
 * <p>{@code isPersonsUnknown} switches the name block between the named layout and the literal
 * "Persons unknown" text. Defaults to {@code false}; the claimant never sets it true.</p>
 */
@Data
@Builder
public class ClaimFormParty {

    private String firstName;
    private String lastName;
    /**
     * For organisational claimants; takes precedence over firstName/lastName when set.
     */
    private String orgName;
    @Builder.Default
    @JsonProperty("isPersonsUnknown")
    private boolean isPersonsUnknown = false;
    /** Always populated; never null in a well-formed case. See the class javadoc. */
    private ClaimFormAddress address;

}
