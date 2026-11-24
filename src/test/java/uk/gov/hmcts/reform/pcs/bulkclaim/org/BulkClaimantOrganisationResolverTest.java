package uk.gov.hmcts.reform.pcs.bulkclaim.org;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimOrganisationException.Reason;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkClaimantOrganisationResolverTest {

    private static final String ORG_ID = "ABC1234";

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    private BulkClaimantOrganisationResolver underTest;

    @BeforeEach
    void setUp() {
        underTest = new BulkClaimantOrganisationResolver(organisationDetailsService);
    }

    @Test
    void shouldReturnActiveSolicitorOrganisation() {
        OrganisationDetailsResponse org = organisation("ACTIVE", List.of("SOLICITOR_PROFILE"));
        when(organisationDetailsService.getOrganisationById(ORG_ID)).thenReturn(org);

        assertThat(underTest.resolve(ORG_ID)).isSameAs(org);
    }

    @Test
    void shouldRejectNonSolicitorProfiles() {
        for (List<String> profiles : List.of(List.of("ORGANISATION_PROFILE", "LOCALAUTH_PROFILE"),
                                             List.of("ORGANISATION_PROFILE"),
                                             List.of("LOCALAUTH_PROFILE"))) {
            underTest = new BulkClaimantOrganisationResolver(organisationDetailsService);
            when(organisationDetailsService.getOrganisationById(ORG_ID))
                .thenReturn(organisation("ACTIVE", profiles));

            assertReason(Reason.NOT_A_SOLICITOR_ORGANISATION);
        }
    }

    @Test
    void shouldAcceptSolicitorProfileAlongsideGenericOrganisationProfile() {
        OrganisationDetailsResponse org = organisation("ACTIVE", List.of("ORGANISATION_PROFILE", "SOLICITOR_PROFILE"));
        when(organisationDetailsService.getOrganisationById(ORG_ID)).thenReturn(org);

        assertThat(underTest.resolve(ORG_ID)).isSameAs(org);
    }

    @Test
    void shouldReportNotFoundWhenPrdReturns404() {
        when(organisationDetailsService.getOrganisationById(ORG_ID)).thenThrow(feign(404));

        assertReason(Reason.NOT_FOUND);
    }

    @Test
    void shouldReportNotFoundWhenPrdRejectsIdentifierAsMalformed() {
        when(organisationDetailsService.getOrganisationById(ORG_ID)).thenThrow(feign(400));

        assertReason(Reason.NOT_FOUND);
    }

    @Test
    void shouldReportNotFoundWhenPrdReturnsNothing() {
        when(organisationDetailsService.getOrganisationById(ORG_ID)).thenReturn(null);

        assertReason(Reason.NOT_FOUND);
    }

    @Test
    void shouldPropagateOtherPrdFailuresUntouched() {
        when(organisationDetailsService.getOrganisationById(ORG_ID)).thenThrow(feign(500));

        assertThatThrownBy(() -> underTest.resolve(ORG_ID)).isInstanceOf(FeignException.class);
    }

    @Test
    void shouldReportNotActiveForAnyOtherStatus() {
        when(organisationDetailsService.getOrganisationById(ORG_ID))
            .thenReturn(organisation("PENDING", List.of("SOLICITOR_PROFILE")));

        assertReason(Reason.NOT_ACTIVE);
    }

    @Test
    void shouldReportProfileNotUsableWhenNoProfiles() {
        when(organisationDetailsService.getOrganisationById(ORG_ID))
            .thenReturn(organisation("ACTIVE", List.of()));

        assertReason(Reason.PROFILE_NOT_USABLE);
    }

    @Test
    void shouldReportProfileNotUsableWhenAmbiguousProfiles() {
        when(organisationDetailsService.getOrganisationById(ORG_ID))
            .thenReturn(organisation("ACTIVE", List.of("SOLICITOR_PROFILE", "LOCALAUTH_PROFILE")));

        assertReason(Reason.PROFILE_NOT_USABLE);
    }

    @Test
    void shouldServeSecondResolveFromCache() {
        when(organisationDetailsService.getOrganisationById(ORG_ID))
            .thenReturn(organisation("ACTIVE", List.of("SOLICITOR_PROFILE")));

        underTest.resolve(ORG_ID);
        underTest.resolve(ORG_ID);

        verify(organisationDetailsService, times(1)).getOrganisationById(ORG_ID);
    }

    @Test
    void shouldNotCacheFailedLookups() {
        when(organisationDetailsService.getOrganisationById(ORG_ID))
            .thenThrow(feign(404))
            .thenReturn(organisation("ACTIVE", List.of("SOLICITOR_PROFILE")));

        assertReason(Reason.NOT_FOUND);
        assertThat(underTest.resolve(ORG_ID)).isNotNull();
    }

    @Test
    void shouldAcceptAcceptedPbaIgnoringCase() {
        OrganisationDetailsResponse org = organisation("ACTIVE", List.of("SOLICITOR_PROFILE"));
        org.setPaymentAccount(List.of("PBA0078010", "PBA0078011"));

        assertThat(underTest.isPbaAccountValid(org, "pba0078011")).isTrue();
        assertThat(underTest.isPbaAccountValid(org, "PBA0078010")).isTrue();
    }

    @Test
    void shouldRejectPendingUnknownOrMissingPba() {
        OrganisationDetailsResponse org = organisation("ACTIVE", List.of("SOLICITOR_PROFILE"));
        org.setPaymentAccount(List.of("PBA0078010"));
        org.setPendingPaymentAccount(List.of("PBA0099999"));

        assertThat(underTest.isPbaAccountValid(org, "PBA0099999")).isFalse();
        assertThat(underTest.isPbaAccountValid(org, "PBA0000000")).isFalse();
        assertThat(underTest.isPbaAccountValid(org, null)).isFalse();
        org.setPaymentAccount(null);
        assertThat(underTest.isPbaAccountValid(org, "PBA0078010")).isFalse();
    }

    private void assertReason(Reason expected) {
        assertThatThrownBy(() -> underTest.resolve(ORG_ID))
            .isInstanceOfSatisfying(BulkClaimOrganisationException.class,
                                    ex -> assertThat(ex.getReason()).isEqualTo(expected));
    }

    private static OrganisationDetailsResponse organisation(String status, List<String> profiles) {
        return OrganisationDetailsResponse.builder()
            .organisationIdentifier(ORG_ID)
            .name("Org")
            .status(status)
            .organisationProfileIds(profiles)
            .build();
    }

    private static FeignException feign(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/refdata/internal/v1/organisations",
                                         new HashMap<>(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("getOrganisationById",
            feign.Response.builder().status(status).request(request).headers(new HashMap<>()).build());
    }
}
