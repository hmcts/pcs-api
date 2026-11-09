package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

public class BreathingSpaceNotSureContent {
    private BreathingSpaceNotSureContent() {
    }

    public static final String HEADING =
        "You will not be able to submit this application if the "
            + "defendants are using the Breathing Space scheme";

    public static final String CANNOT_PROCEED_MESSAGE =
        "You cannot continue with this application while the "
            + "defendants are using the Breathing Space scheme.";

    public static String information(boolean isWrit) {
        String applicationType = isWrit ? "writ" : "warrant";

        return """
            <p class="govuk-body">
                You will not be able to submit this application if the
                defendants are using the Breathing Space scheme.
            </p>

            <p class="govuk-body">
                Check if the defendants are in Breathing Space before you continue.
            </p>

            <h2 class="govuk-heading-s govuk-!-font-size-19">
                What you should do next
            </h2>

            <p class="govuk-body">
                The Insolvency Service can tell you if the defendants are
                using the Breathing Space scheme.
                <a class="govuk-link"
                   href="https://www.gov.uk/government/organisations/insolvency-service"
                   target="_blank"
                   rel="noopener noreferrer">
                   Contact the Insolvency Service (opens in new tab)</a> to confirm.
            </p>

            <h2 class="govuk-heading-s govuk-!-font-size-19">
                If the Insolvency Service confirms that the defendants
                are using the Breathing Space scheme
            </h2>

            <p class="govuk-body">
                You must contact the court and tell them that the defendants
                are using the Breathing Space scheme.
            </p>

            <h2 class="govuk-heading-s govuk-!-font-size-19">
                Contact the court by email
            </h2>

            <p class="govuk-body">
                Use the
                <a class="govuk-link"
                   href="https://www.find-court-tribunal.service.gov.uk/"
                   target="_blank"
                   rel="noopener noreferrer">
                   find a court or tribunal service (opens in new tab)</a> to get your local court’s email address.
            </p>

            <p class="govuk-body">
                If you’re applying for a warrant and you do not know the name
                of your local court, select the ‘Money’ category and then
                the ‘Housing’ category to find it.
            </p>

            <p class="govuk-body">
                If you’re applying for a writ and you do not know the name
                of your local court, select the ‘High Court district
                registries’ to find it.
            </p>

            <h2 class="govuk-heading-s govuk-!-font-size-19">
                Contact the court by telephone
            </h2>

            <p class="govuk-body">
                0300 123 7050
                <br/>
                Monday to Friday, 8.30am to 5pm.
            </p>

            <p class="govuk-body">
                <a class="govuk-link"
                   href="https://www.gov.uk/call-charges"
                   target="_blank"
                   rel="noopener noreferrer">
                    Find out about call charges (opens in new tab)
                </a>.
            </p>

            <h2 class="govuk-heading-s govuk-!-font-size-19">
                If the Insolvency Service confirms that the defendants
                are not using the Breathing Space scheme
            </h2>

            <p class="govuk-body">
                You can return to this service, and continue applying
                for a %s.
            </p>
            """.formatted(applicationType);
    }
}
