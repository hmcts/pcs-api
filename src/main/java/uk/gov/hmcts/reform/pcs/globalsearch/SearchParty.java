package uk.gov.hmcts.reform.pcs.globalsearch;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.SearchPartyField;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

import static java.util.List.of;

@Component
public class SearchParty implements CCDConfig<PCSCase, State, UserRole> {

    private static final List<SearchPartyField> SEARCH_PARTY_LIST = of(
        SearchPartyField.builder()
            // TODO: replace firstName with a computed fullName field on Party
            .searchPartyName("firstName")
            .searchPartyEmailAddress("emailAddress")
            .searchPartyAddressLine1("address.AddressLine1")
            .searchPartyPostCode("address.PostCode")
            .searchPartyDOB("dateOfBirth")
            .searchPartyCollectionFieldName("parties")
            .searchPartyDOD("")
            .build()
    );

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder.searchParty()
            .fields(SEARCH_PARTY_LIST);
    }
}