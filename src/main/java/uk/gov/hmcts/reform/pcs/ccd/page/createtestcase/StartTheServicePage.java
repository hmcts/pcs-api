package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Start the service CCD page configuration.
 * This page serves as a placeholder with minimal configuration.
 */
public class StartTheServicePage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("startingTheService")
            .showCondition("showStartTheServicePage=\"Yes\"")
            .readonly(PCSCase::getShowStartTheServicePage, NEVER_SHOW)
            .label("lineSeparator", "---")
            .label("startingTheService-info", """
                <h2 class="govuk-heading-m">Before you start</h2>
                <p class="govuk-body">
                You can use this online service if you're a registered provider of social housing or a community
                landlord and the property you want to claim possession of is in England or Wales.
                This service is also available in <a href="https://www.gov.uk/cais-trwydded-yrru-dros-dro" class="govuk-link">Welsh (Cymraeg)</a>.
                We'll check your eligibility by asking for the property's address.
                The claim fee is £404.
                </p>

                <h3 class="govuk-heading-s">What you'll need</h3>
                <p class="govuk-body">Before you start, make sure you have the following information:</p>

                <ul class="govuk-list govuk-list--bullet">
                  <li>details of the tenancy, contract, licence or mortgage agreement</li>
                  <li>the defendants' details (the people you're making the claim against)</li>
                  <li>your reasons for making a possession claim</li>
                  <li>copies of any relevant documents. You can either upload documents now or closer to the
                  hearing date.
                  Any documents you upload now will be included in the pack of documents that a judge will receive
                  before the hearing (the bundle)</li>
                </ul>

                <p class="govuk-body">Once you've finished answering the questions, you can either:</p>
                <ul class="govuk-list govuk-list--bullet">
                  <li>Sign, submit and pay for your claim now, or</li>
                  <li>Save it as a draft. You can then sign, submit and pay at a later date</li>
                </ul>
                """);
    }
}
