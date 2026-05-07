package uk.gov.hmcts.reform.pcs.ccd.page;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonPageContent {

    private static final String CPR_URL = "https://www.justice.gov.uk/courts/procedure-rules/civil/rules/part45-"
        + "fixed-costs/practice-direction-45-fixed-costs";

    public static final String SAVE_AND_RETURN = """
        <details class="govuk-details">
            <summary class="govuk-details__summary">
                <span class="govuk-details__summary-text">
                    I want to save this application and return to it later
                </span>
            </summary>
            <div class="govuk-details__text">
                If you want to save your application and return to it later:
                <ol class="govuk-list govuk-list--number">
                    <li class="govuk-!-static-margin-0">Choose ‘Continue’.</li>
                    <li>On the next page choose ‘Cancel’.</li>
                </ol>
                <p>This will save your progress and take you to the case overview.</p>
            </div>
        </details>
        """;

    public static final String LEGAL_COSTS_HELP = """
        <details class="govuk-details">
            <summary class="govuk-details__summary">
                <span class="govuk-details__summary-text">
                    I do not know if I need to reclaim any legal costs
                </span>
            </summary>
            <div class="govuk-details__text">
                <p>
                    Legal costs are the costs you incur when a lawyer, legal representative, or
                    someone working in a legal department applies for a writ or a warrant on your behalf.
                </p>
                <p>
                    They will invoice these costs to you, and you can reclaim them from the defendant.
                </p>
                <p>
                    <div class="govuk-!-font-weight-bold">
                        If you are not sure how much you can reclaim
                    </div>
                </p>
                <p>
                    The amount you can reclaim from the defendant is usually fixed.
                </p>
                <p class="govuk-body govuk-!-margin-bottom-1">
                    You can either:
                </p>
                <ul>
                    <li class="govuk-list govuk-!-font-size-19">ask your lawyer or legal representative how much you can
                     reclaim, or</li>
                    <li class="govuk-list govuk-!-font-size-19">
                        <a href=""" + CPR_URL + """
                            target="_blank">
                            check the Civil Procedure Rules (Justice.gov website, opens in a new tab)
                        </a>
                    </li>
                </ul>
            </div>
        </details>
        """;

    public static final String WARRANT_OF_RESTITUTION_INFO_TEXT =
        """
        <p class="govuk-body govuk-!-font-weight-bold">If you choose a warrant of restitution</p>
        <p class="govuk-body govuk-!-margin-bottom-1">It is free to apply for a warrant of
        restitution, but:</p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">you’ll need a warrant of possession before you can apply
          </li>
          <li class="govuk-!-font-size-19">you can only use it if you have already tried to evict
          someone, but they returned to the property after the eviction. For example, if they
          unlawfully returned after the bailiffs left.</li>
        </ul>
        <p class="govuk-body govuk-!-margin-bottom-1">In a warrant of restitution, the judge will:
        </p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">review evidence that the defendants returned to the
          property after the eviction</li>
          <li class="govuk-!-font-size-19">(in most cases) make a decision without a hearing</li>
        </ul>
        """;
}
