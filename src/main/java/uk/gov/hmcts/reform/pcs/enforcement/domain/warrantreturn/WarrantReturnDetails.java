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
 * The return name is three fixed lists picked by show condition rather than one dynamic list: the outcome
 * that narrows it is chosen on the same page, and a dynamic list can only be refilled by a mid-event callback.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class WarrantReturnDetails {

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
