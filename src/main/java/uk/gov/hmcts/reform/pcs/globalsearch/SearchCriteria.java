package uk.gov.hmcts.reform.pcs.globalsearch;

import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.SearchCriteriaField;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import static java.util.List.of;

@Slf4j
@Component
@AllArgsConstructor
public class SearchCriteria implements CCDConfig<PCSCase, State, UserRole> {

    private static final List<SearchCriteriaField> SEARCH_CRITERIA_LIST = of(
        SearchCriteriaField.builder().otherCaseReference("otherCaseReference").build()
    );

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder.searchCriteria().fields(SEARCH_CRITERIA_LIST).build();
    }
}
