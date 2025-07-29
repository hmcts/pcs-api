package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * CCD page configuration for starting the housing possession claim service.
 */
@Component
public class StartingTheService implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("startingTheService")
            .pageLabel("Start your possession claim")
            .showCondition("eligibleForClaim=\"Yes\"")
            .label("lineSeparator", "---")
            .label("startingTheService-info", """
                # Before you start

                You can use this online service if you're a registered provider of social housing or a community landlord and the property you want to claim possession of is in England or Wales.

                This service is also available in [Welsh (Cymraeg)](https://www.gov.uk/cais-trwydded-yrru-dros-dro).

                We'll check your eligibility by asking for the property's address.

                The claim fee is £404.

                ## What you'll need

                Before you start, make sure you have the following information:

                - details of the tenancy, contract, licence or mortgage agreement
                - the defendants' details (the people you're making the claim against)
                - your reasons for making a possession claim
                - copies of any relevant documents. You can either upload documents now or closer to the hearing date. Any documents you upload now will be included in the pack of documents that a judge will receive before the hearing (the bundle)

                Once you've finished answering the questions, you can either:

                - Sign, submit and pay for your claim now, or
                - Save it as a draft. You can then sign, submit and pay at a later date
                """);
    }
}
