package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.time.LocalDateTime;
import java.util.Optional;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

/**
 * The answers to "Record warrant return" (HDPI-7347). They are saved to the enforcement's return history on
 * submit, not to the case.
 *
 * <p>The return names offered depend on the return and the outcome. They are three fixed lists shown by show
 * condition rather than one dynamic list, because the outcome is chosen on the same screen: a dynamic list
 * can only be refilled by a mid-event callback, which runs when the user leaves the page.</p>
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
// Unwrapped with a prefix, which Jackson prepends as is: the CCD field ids are prefix + UpperCamelCase.
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class WarrantReturnDetails {

    /**
     * The claimant type, claimant name and defendant names, set when the event starts.
     */
    @CCD(searchable = false)
    private String evictionDetailsMarkup;

    @CCD(label = "Return", typeOverride = FixedList, typeParameterOverride = "WarrantReturnType",
        searchable = false)
    private WarrantReturnType type;

    @CCD(label = "Outcome", typeOverride = FixedList, typeParameterOverride = "WarrantReturnOutcome",
        searchable = false)
    private WarrantReturnOutcome outcome;

    @CCD(label = "Return name", typeOverride = FixedList, typeParameterOverride = "SuccessfulWarrantReturnName",
        searchable = false)
    private SuccessfulWarrantReturnName successfulReturnName;

    @CCD(label = "Return name", typeOverride = FixedList, typeParameterOverride = "UnsuccessfulWarrantReturnName",
        searchable = false)
    private UnsuccessfulWarrantReturnName unsuccessfulReturnName;

    @CCD(label = "Return name", typeOverride = FixedList, typeParameterOverride = "InterimWarrantReturnName",
        searchable = false)
    private InterimWarrantReturnName interimReturnName;

    @CCD(label = "Eviction date and time", hint = "For example, 16 4 2021, 11 15", searchable = false)
    private LocalDateTime evictionDateTime;

    @CCD(label = "Additional details", typeOverride = TextArea, max = RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT,
        searchable = false)
    private String additionalDetails;

    /**
     * The return name from the drop down that the chosen return and outcome show. Answers left in the other
     * drop downs, from before the user changed the return or outcome, are ignored.
     */
    public static Optional<WarrantReturnName> selectedReturnName(WarrantReturnDetails details) {
        if (details == null || details.getType() == null) {
            return Optional.empty();
        }
        if (details.getType() == WarrantReturnType.INTERIM) {
            return Optional.ofNullable(details.getInterimReturnName()).map(InterimWarrantReturnName::getReturnName);
        }
        if (details.getOutcome() == null) {
            return Optional.empty();
        }
        return switch (details.getOutcome()) {
            case EVICTION_SUCCESSFUL -> Optional.ofNullable(details.getSuccessfulReturnName())
                .map(SuccessfulWarrantReturnName::getReturnName);
            case EVICTION_UNSUCCESSFUL, EVICTION_CANCELLED -> Optional.ofNullable(details.getUnsuccessfulReturnName())
                .map(UnsuccessfulWarrantReturnName::getReturnName);
        };
    }
}
