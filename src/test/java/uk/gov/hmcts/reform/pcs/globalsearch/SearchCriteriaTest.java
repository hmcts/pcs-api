package uk.gov.hmcts.reform.pcs.globalsearch;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchCriteriaTest {

    private SearchCriteria underTest;

    @BeforeEach
    void setUp() {
        underTest = new SearchCriteria();
    }

    @Test
    void shouldConfigureSearchCriteria() {
        DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder = mock(DecentralisedConfigBuilder.class);
       var searchCriteriaBuilder = mock(uk.gov.hmcts.ccd.sdk.api.SearchCriteria.SearchCriteriaBuilder.class);

       when(configBuilder.searchCriteria()).thenReturn(searchCriteriaBuilder);
       when(searchCriteriaBuilder.fields(anyList())).thenReturn(searchCriteriaBuilder);

       underTest.configureDecentralised(configBuilder);

       verify(configBuilder, times(1)).searchCriteria();
       verify(searchCriteriaBuilder, times(1)).fields(anyList());
    }
}
