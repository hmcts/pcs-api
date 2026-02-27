package uk.gov.hmcts.reform.pcs;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.SearchCriteriaField;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

@Component
public class SearchCriteria implements CCDConfig<PCSCase, State, UserRole> {

    private static final List<SearchCriteriaField> SEARCH_CRITERIA_LIST = List.of(
        SearchCriteriaField.builder().otherCaseReference("otherCaseReference").build()
    );

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder.searchCriteria()
            .fields(SEARCH_CRITERIA_LIST)
            .build();
    }
}
