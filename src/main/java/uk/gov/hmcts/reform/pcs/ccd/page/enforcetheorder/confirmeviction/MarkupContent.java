package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.confirmeviction;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MarkupContent {

    public static String CONFIRM_EVICTION_SUMMARY_WITH_DATES =
            """
            <h2 class="govuk-heading-m govuk-!-padding-top-1">Confirm the eviction date</h2>
            <p class="govuk-body govuk-!-font-size-19">
             The bailiff has given you an eviction date of %s.
            </p>
            <p class="govuk-body govuk-!-font-size-19 govuk-!-padding-bottom-6">
             They need you to confirm if you are available on this date.
            </p>
            <p class="govuk-body govuk-!-font-size-19">
             You must confirm the eviction details before %s.
             If you try to confirm the eviction after this
             date, the bailiff will cancel your eviction.
            </p>
            <p class="govuk-body govuk-!-font-size-19">
             They will also ask you to confirm if the defendants
             (the person or people being evicted) pose any risk to the
             bailiff.
            </p>
            <p class="govuk-body govuk-!-font-size-19 govuk-!-padding-bottom-6">
             The bailiff needs this information to carry out the eviction
             safely.
            </p>
            <p class="govuk-body govuk-!-font-size-19">
             To confirm the eviction date, select ‘Confirm the eviction
             date’ from the dropdown menu.
            </p>
            """;

    public static String CONFIRM_EVICTION_SUMMARY_NO_DATES =
            """
            <h2 class="govuk-heading-m govuk-!-padding-top-1">You cannot enforce the order at the moment</h2>
            <p class="govuk-body govuk-!-padding-bottom-2">
             You cannot enforce the order at the moment (use a bailiff to evict someone).
            </p>
            <p class="govuk-body govuk-!-font-weight-bold govuk-!-padding-bottom-2"> How to find out why you cannot
             enforce the order
            </p>
            <p class="govuk-body govuk-!-margin-bottom-0">To find out why you cannot enforce the order, you can:</p>
            <ul class="govuk-list govuk-list--bullet">
             <li class="govuk-!-font-size-19">check the tab: ‘Case file view’ (you should see an order from the court,
             explaining why you cannot enforce), or</li>
             <li class="govuk-!-font-size-19">
             <a href="https://www.gov.uk/find-court-tribunal"
                              rel="noreferrer noopener"
                              target="_blank" class="govuk-link">
             contact your local court.</a> You will need to tell them your case number
             (you can find this at the top of this page). If you do not know the name of your local court, select the
             ‘Money’ category and then the ‘Housing’ category to find it.</li>
            </ul>
            """;

}
