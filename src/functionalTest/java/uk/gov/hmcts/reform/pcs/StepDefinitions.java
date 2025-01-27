package uk.gov.hmcts.reform.pcs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;

import org.junit.platform.suite.api.ConfigurationParameter;
import static io.cucumber.core.options.Constants.PLUGIN_PROPERTY_NAME;


@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
)

public class StepDefinitions {

    @Given("repo is created")
    public void repo_is_created() {
    }

    @When("framework is implemented")
    public void framework_is_implemented() {
    }

    @Then("{string} is printed")
    public void is_printed(String string) {
        System.out.println(string);
    }
}
