package uk.gov.hmcts.reform.pcs.ccd.view.globalsearch;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.SearchCriteria;
import uk.gov.hmcts.ccd.sdk.type.SearchParty;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds the {@link SearchCriteria} used to index a case for global search.
 *
 * <p>Global search only indexes postcodes that appear on a SearchParty, so the property to be
 * repossessed is added as an extra SearchParty alongside the real parties to make it searchable.
 */
@Component
public class SearchCriteriaIndexer {

    /**
     * Builds the {@link SearchCriteria} for indexing the given case in global search.
     * @param pcsCase the case to index
     * @return search criteria covering the case parties and the property to be repossessed
     */
    public SearchCriteria buildSearchCriteria(PCSCase pcsCase) {
        List<SearchParty> searchParties = new ArrayList<>(
            Optional.ofNullable(pcsCase.getParties())
                .orElse(List.of())
                .stream()
                .map(ListValue::getValue)
                .map(this::toSearchParty)
                .toList());

        // Global search only indexes postcodes that appear on a SearchParty.
        // We add the property to be repossessed address as an extra SearchParty here to make it searchable.
        SearchParty propertySearchParty = toPropertySearchParty(pcsCase.getPropertyAddress());
        if (propertySearchParty != null) {
            searchParties.add(propertySearchParty);
        }

        return SearchCriteria.builder()
            .parties(ListValueUtils.wrapListItems(searchParties))
            .build();
    }

    private SearchParty toPropertySearchParty(AddressUK propertyAddress) {
        if (propertyAddress == null
            || propertyAddress.getPostCode() == null
            || propertyAddress.getPostCode().isBlank()) {
            return null;
        }

        return SearchParty.builder()
            .addressLine1(propertyAddress.getAddressLine1())
            .postcode(propertyAddress.getPostCode())
            .build();
    }

    private SearchParty toSearchParty(Party party) {
        AddressUK address = party.getAddress();
        return SearchParty.builder()
            .name(joinNonBlank(party.getFirstName(), party.getLastName()))
            .emailAddress(party.getEmailAddress())
            .addressLine1(address == null ? null : address.getAddressLine1())
            .postcode(address == null ? null : address.getPostCode())
            .dateOfBirth(party.getDateOfBirth())
            .build();
    }

    private static String joinNonBlank(String... parts) {
        String joined = Arrays.stream(parts)
            .filter(p -> p != null && !p.isBlank())
            .collect(Collectors.joining(" "));
        return joined.isEmpty() ? null : joined;
    }
}
