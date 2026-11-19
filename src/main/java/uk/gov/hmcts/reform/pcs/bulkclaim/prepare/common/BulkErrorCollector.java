package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageClaimSet;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentalClaimSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects validation errors for one claim, addressing each by an XPath-style path such as
 * {@code /RentalClaimSet/RentalClaim[2]/ClaimParticulars/Grounds/Ground[1]/Code}.
 * Paths passed to the collector are relative to the claim element.
 */
public final class BulkErrorCollector {

    public static final String REQUIRED = "REQUIRED";
    public static final String TOO_LONG = "TOO_LONG";
    public static final String INVALID_VALUE = "INVALID_VALUE";
    public static final String NOT_ALLOWED = "NOT_ALLOWED";
    public static final String NOT_IN_PAST = "NOT_IN_PAST";
    public static final String NOT_AGREED = "NOT_AGREED";

    private final String claimPath;
    private final List<BulkValidationError> errors = new ArrayList<>();

    private BulkErrorCollector(String claimPath) {
        this.claimPath = claimPath;
    }

    public static BulkErrorCollector forRentalClaim(RentalClaimSet claimSet, RentalClaimSet.RentalClaim claim) {
        return forClaimPath(buildClaimPath("RentalClaimSet", "RentalClaim", claimSet.getRentalClaim().indexOf(claim)));
    }

    public static BulkErrorCollector forMortgageClaim(MortgageClaimSet claimSet,
                                                      MortgageClaimSet.MortgageClaim claim) {
        int index = claimSet.getMortgageClaim().indexOf(claim);
        return forClaimPath(buildClaimPath("MortgageClaimSet", "MortgageClaim", index));
    }

    private static String buildClaimPath(String root, String element, int index) {
        return "/" + root + "/" + indexed(element, Math.max(index, 0));
    }

    public static BulkErrorCollector forClaimPath(String claimPath) {
        return new BulkErrorCollector(claimPath);
    }

    /**
     * A repeating element step, for example {@code Ground[2]} for zero-based index 1.
     */
    public static String indexed(String element, int zeroBasedIndex) {
        return element + "[" + (zeroBasedIndex + 1) + "]";
    }

    public String claimPath() {
        return claimPath;
    }

    public String absolutePath(String relativePath) {
        return StringUtils.hasLength(relativePath) ? claimPath + "/" + relativePath : claimPath;
    }

    public void add(String relativePath, String code, String message) {
        errors.add(new BulkValidationError(absolutePath(relativePath), code, message));
    }

    public void addAll(String relativePath, String code, List<String> messages) {
        messages.forEach(message -> add(relativePath, code, message));
    }

    /** Records REQUIRED and returns false when the value is null or a blank string. */
    public boolean required(String relativePath, Object value, String label) {
        boolean missing = value == null || (value instanceof String text && text.isBlank());
        if (missing) {
            add(relativePath, REQUIRED, label + " is required");
        }
        return !missing;
    }

    /** Records TOO_LONG and returns false when the text exceeds the limit. */
    public boolean maxLength(String relativePath, String value, int limit, String label) {
        if (value != null && value.length() > limit) {
            add(relativePath, TOO_LONG, label + " must be " + limit + " characters or fewer");
            return false;
        }
        return true;
    }

    public boolean requiredWithMaxLength(String relativePath, String value, int limit, String label) {
        return required(relativePath, value, label) && maxLength(relativePath, value, limit, label);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<BulkValidationError> errors() {
        return List.copyOf(errors);
    }

}
