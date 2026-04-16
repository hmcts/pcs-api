package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantCircumstances {

    @CCD(label = """
        ---
        <p class="govuk-body" tabindex="0">
         You can use this section to provide any information about the defendants’ circumstances.
        </p>
        <p class="govuk-body">
        You must give details if any of the following apply
        <ul class="govuk-list govuk-list--bullet">
            <li class="govuk-!-font-size-19">
            the defendant receives any social security benefits</li>
            <li class="govuk-!-font-size-19">
            payments are made on their behalf directly to you, under the Social Security Contributions
            and Benefits Act 1992</li>
        </ul>
        </p>
        """,typeOverride = FieldType.Label
    )
    private String defendantCircumstancesLabel;

    @CCD(
        label = "Is there any information you’re required to provide, or you want to provide, "
            + "about the defendants’ circumstances?",
        hint = "This can be any known details or any attempts made to obtain details"
    )
    private VerticalYesNo hasDefendantCircumstancesInfo;

    @CCD(
        label = "Give details about the defendants’ circumstances",
        hint = "You can enter up to 950 characters",
        typeOverride = TextArea
    )
    private String defendantCircumstancesInfo;

    private String defendantTermPossessive;
}
