package uk.gov.hmcts.reform.pcs.reference.commondata;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.reference.api.CommonDataApi;
import uk.gov.hmcts.reform.pcs.reference.dto.ListOfValue;
import uk.gov.hmcts.reform.pcs.reference.dto.ListOfValuesResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataSyncServiceTest {

    private static final String SERVICE_ID = "AAA3";
    private static final String IDAM_TOKEN = "Bearer idam-token";
    private static final String S2S_TOKEN = "s2s-token";
    private static final ReferenceDataSource SOURCE = ReferenceDataSource.HEARING_TYPE;

    @Mock
    private CommonDataApi commonDataApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private ReferenceDataRepository repository;
    @Mock
    private ReferenceDataCache cache;

    private ReferenceDataSyncService underTest;

    @BeforeEach
    void setUp() {
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(IDAM_TOKEN);
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        underTest = new ReferenceDataSyncService(
            commonDataApi, authTokenGenerator, systemUpdateUserTokenProvider, repository, cache, SERVICE_ID
        );
        lenient().doThrow(feignException(404)).when(commonDataApi)
            .getListOfValues(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldRequestEveryMappedCategoryForTheConfiguredService() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha", null, 1L, null, null, "Y"));
        stubTable(SOURCE, existing());

        // when
        underTest.synchronise();

        // then
        for (ReferenceDataSource source : ReferenceDataSource.values()) {
            verify(commonDataApi).getListOfValues(source.getCategoryId(), IDAM_TOKEN, S2S_TOKEN, SERVICE_ID);
        }
    }

    @Test
    void shouldInsertAllActiveValuesIntoAnEmptyTable() {
        // given
        stubUpstream(SOURCE,
                     lov("A", "Alpha", "Alffa", 1L, null, null, "Y"),
                     lov("B", "Beta", null, 2L, null, null, "Y"));
        ReferenceTable table = stubTable(SOURCE, existing());

        // when
        underTest.synchronise();

        // then
        assertThat(applied(table).inserts()).containsExactly(
            record("A", "Alpha", "Alffa", 1, null, null, true),
            record("B", "Beta", null, 2, null, null, true)
        );
        assertThat(applied(table).updates()).isEmpty();
        verify(cache, times(2)).reload(SOURCE);
    }

    @Test
    void shouldInsertOnlyMissingValuesAndLeaveUnchangedOnesUntouched() {
        // given
        stubUpstream(SOURCE,
                     lov("A", "Alpha", null, 1L, null, null, "Y"),
                     lov("B", "Beta", null, 2L, null, null, "Y"));
        ReferenceTable table = stubTable(SOURCE, existing(record("A", "Alpha", null, 1, null, null, true)));

        // when
        underTest.synchronise();

        // then
        assertThat(applied(table).inserts()).containsExactly(record("B", "Beta", null, 2, null, null, true));
        assertThat(applied(table).updates()).isEmpty();
    }

    @Test
    void shouldUpdateOnlyValuesThatChanged() {
        // given
        stubUpstream(SOURCE,
                     lov("A", "Alpha", null, 1L, null, null, "Y"),
                     lov("B", "Beta renamed", null, 2L, null, null, "Y"));
        ReferenceTable table = stubTable(SOURCE, existing(
            record("A", "Alpha", null, 1, null, null, true),
            record("B", "Beta", null, 2, null, null, true)
        ));

        // when
        underTest.synchronise();

        // then
        assertThat(applied(table).inserts()).isEmpty();
        assertThat(applied(table).updates()).containsExactly(record("B", "Beta renamed", null, 2, null, null, true));
    }

    @Test
    void shouldNotWriteWhenUpstreamMatchesExistingDataAndSoBeIdempotent() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha", "Alffa", 1L, null, null, "Y"));
        stubTable(SOURCE, existing(record("A", "Alpha", "Alffa", 1, null, null, true)));

        // when
        underTest.synchronise();
        underTest.synchronise();

        // then
        verify(repository, never()).apply(any(), any(), any());
        verify(cache, times(2)).reload(SOURCE);
    }

    @Test
    void shouldIgnoreSurroundingWhitespaceIncludingNonBreakingSpaces() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha ", " Alffa ", 1L, null, null, "Y"));
        stubTable(SOURCE, existing(record("A", "Alpha", "Alffa", 1, null, null, true)));

        // when
        underTest.synchronise();

        // then
        verify(repository, never()).apply(any(), any(), any());
    }

    @Test
    void shouldTreatBlankOptionalValuesAsNull() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha", "  ", null, "", null, "Y"));
        stubTable(SOURCE, existing(record("A", "Alpha", null, null, null, null, true)));

        // when
        underTest.synchronise();

        // then
        verify(repository, never()).apply(any(), any(), any());
    }

    @Test
    void shouldPersistParentCategoryAndKeyWhenTableSupportsThem() {
        // given
        ReferenceDataSource subchannel = ReferenceDataSource.HEARING_SUBCHANNEL;
        stubUpstream(subchannel, lov("VIDCVP", "Video - CVP", null, null, "HearingChannel", "VID", "Y"));
        ReferenceTable table = stubTable(subchannel, existing(), ReferenceColumn.VALUE_EN, ReferenceColumn.VALUE_CY,
                                         ReferenceColumn.PARENT_CATEGORY, ReferenceColumn.PARENT_KEY);

        // when
        underTest.synchronise();

        // then
        assertThat(applied(table).inserts()).containsExactly(
            record("VIDCVP", "Video - CVP", null, null, "HearingChannel", "VID", true)
        );
    }

    @Test
    void shouldIgnoreUpstreamAttributesTheTableDoesNotStore() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha", "Alffa", 7L, "Parent", "P", "Y"));
        ReferenceTable table = stubTable(SOURCE, existing(), ReferenceColumn.VALUE_EN);

        // when
        underTest.synchronise();

        // then
        assertThat(applied(table).inserts()).containsExactly(record("A", "Alpha", null, null, null, null, true));
    }

    @Test
    void shouldPreferTheActiveRowWhenUpstreamReturnsDuplicateKeys() {
        // given
        stubUpstream(SOURCE,
                     lov("A", "Alpha deleted", null, null, null, null, "D"),
                     lov("A", "Alpha", null, null, null, null, "Y"));
        ReferenceTable table = stubTable(SOURCE, existing(record("A", "Alpha", null, null, null, null, true)));

        // when
        underTest.synchronise();

        // then
        verify(repository, never()).apply(any(), any(), any());
        assertThat(table.records()).containsKey("A");
    }

    @Test
    void shouldKeepTheFirstRowWhenDuplicateKeysAreAllActive() {
        // given
        stubUpstream(SOURCE,
                     lov("A", "First", null, null, null, null, "Y"),
                     lov("A", "Second", null, null, null, null, "Y"));
        ReferenceTable table = stubTable(SOURCE, existing());

        // when
        underTest.synchronise();

        // then
        assertThat(applied(table).inserts()).containsExactly(record("A", "First", null, null, null, null, true));
    }

    @Test
    void shouldDeactivateAnExistingValueUpstreamHasDeletedWithoutRemovingIt() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha", null, null, null, null, "D"));
        ReferenceTable table = stubTable(SOURCE, existing(record("A", "Alpha", null, null, null, null, true)));

        // when
        underTest.synchronise();

        // then
        assertThat(applied(table).inserts()).isEmpty();
        assertThat(applied(table).updates()).containsExactly(record("A", "Alpha", null, null, null, null, false));
    }

    @Test
    void shouldNotInsertValuesThatUpstreamHasDeleted() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha", null, null, null, null, "D"));
        stubTable(SOURCE, existing());

        // when
        underTest.synchronise();

        // then
        verify(repository, never()).apply(any(), any(), any());
    }

    @Test
    void shouldLeaveExistingValuesMissingFromTheUpstreamResponseUntouched() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha", null, null, null, null, "Y"));
        ReferenceTable table = stubTable(SOURCE, existing(
            record("A", "Alpha", null, null, null, null, true),
            record("OLD", "Retired", null, null, null, null, true)
        ));

        // when
        underTest.synchronise();

        // then
        verify(repository, never()).apply(any(), any(), any());
        assertThat(table.records()).containsKey("OLD");
    }

    @Test
    void shouldIgnoreUpstreamValuesWithoutAKey() {
        // given
        stubUpstream(SOURCE,
                     lov(null, "No key", null, null, null, null, "Y"),
                     lov("  ", "Blank key", null, null, null, null, "Y"),
                     lov("A", "Alpha", null, null, null, null, "Y"));
        ReferenceTable table = stubTable(SOURCE, existing());

        // when
        underTest.synchronise();

        // then
        assertThat(applied(table).inserts()).containsExactly(record("A", "Alpha", null, null, null, null, true));
    }

    @Test
    void shouldSkipCategoriesThatAreNotAvailableUpstreamWithoutFailing() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha", null, null, null, null, "Y"));
        stubTable(SOURCE, existing(record("A", "Alpha", null, null, null, null, true)));

        // when / then
        underTest.synchronise();

        verify(cache, never()).reload(ReferenceDataSource.ADDITIONAL_ROLES);
    }

    @Test
    void shouldNotTouchExistingDataWhenUpstreamReturnsNoValues() {
        // given
        doReturn(new ListOfValuesResponse(List.of())).when(commonDataApi)
            .getListOfValues(eq(SOURCE.getCategoryId()), anyString(), anyString(), anyString());

        // when
        underTest.synchronise();

        // then
        verify(cache, never()).reload(SOURCE);
        verify(repository, never()).apply(any(), any(), any());
    }

    @Test
    void shouldNotTouchExistingDataWhenUpstreamResponseIsMalformed() {
        // given
        doReturn(new ListOfValuesResponse(null)).when(commonDataApi)
            .getListOfValues(eq(SOURCE.getCategoryId()), anyString(), anyString(), anyString());

        // when
        underTest.synchronise();

        // then
        verify(cache, never()).reload(SOURCE);
        verify(repository, never()).apply(any(), any(), any());
    }

    @Test
    void shouldContinueWithOtherCategoriesAndReportFailureWhenOneRequestFails() {
        // given
        ReferenceDataSource failing = ReferenceDataSource.FACILITIES;
        doThrow(new IllegalStateException("upstream down")).when(commonDataApi)
            .getListOfValues(eq(failing.getCategoryId()), anyString(), anyString(), anyString());
        stubUpstream(SOURCE, lov("A", "Alpha", null, null, null, null, "Y"));
        ReferenceTable table = stubTable(SOURCE, existing());

        // when / then
        assertThatThrownBy(() -> underTest.synchronise())
            .isInstanceOf(ReferenceDataSyncException.class)
            .hasMessageContaining(failing.getCategoryId());

        verify(repository).apply(any(), any(), any());
        verify(cache, never()).reload(failing);
        assertThat(table.records()).isEmpty();
    }

    @Test
    void shouldReportAuthenticationFailureWithoutWritingAnything() {
        // given
        doThrow(feignException(403)).when(commonDataApi)
            .getListOfValues(anyString(), anyString(), anyString(), anyString());

        // when / then
        assertThatThrownBy(() -> underTest.synchronise()).isInstanceOf(ReferenceDataSyncException.class);

        verify(repository, never()).apply(any(), any(), any());
        verify(cache, never()).reload(any());
    }

    @Test
    void shouldReportFailureAndSkipCacheRefreshWhenPersistingFails() {
        // given
        stubUpstream(SOURCE, lov("A", "Alpha", null, null, null, null, "Y"));
        ReferenceTable table = stubTable(SOURCE, existing());
        doThrow(new IllegalStateException("db down")).when(repository).apply(eq(table), any(), any());

        // when / then
        assertThatThrownBy(() -> underTest.synchronise())
            .isInstanceOf(ReferenceDataSyncException.class)
            .hasMessageContaining(SOURCE.getCategoryId());

        verify(cache, times(1)).reload(SOURCE);
    }

    private void stubUpstream(ReferenceDataSource source, ListOfValue... values) {
        doReturn(new ListOfValuesResponse(List.of(values))).when(commonDataApi)
            .getListOfValues(eq(source.getCategoryId()), anyString(), anyString(), anyString());
    }

    private ReferenceTable stubTable(ReferenceDataSource source, Map<String, ReferenceRecord> records,
                                     ReferenceColumn... columns) {
        Set<ReferenceColumn> supported = columns.length == 0
            ? EnumSet.of(ReferenceColumn.VALUE_EN, ReferenceColumn.VALUE_CY, ReferenceColumn.LOV_ORDER)
            : EnumSet.copyOf(Arrays.asList(columns));
        Map<ReferenceColumn, String> columnNames = new LinkedHashMap<>();
        supported.forEach(column -> columnNames.put(column, column.candidateNames().get(0)));
        ReferenceTable table = new ReferenceTable(source.getTableName(), columnNames, true, records);
        when(cache.reload(source)).thenReturn(table);
        return table;
    }

    private Map<String, ReferenceRecord> existing(ReferenceRecord... records) {
        Map<String, ReferenceRecord> byKey = new LinkedHashMap<>();
        Arrays.stream(records).forEach(record -> byKey.put(record.key(), record));
        return byKey;
    }

    private Applied applied(ReferenceTable table) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReferenceRecord>> inserts = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReferenceRecord>> updates = ArgumentCaptor.forClass(List.class);
        verify(repository).apply(eq(table), inserts.capture(), updates.capture());
        return new Applied(inserts.getValue(), updates.getValue());
    }

    private ListOfValue lov(String key, String valueEn, String valueCy, Long order, String parentCategory,
                            String parentKey, String active) {
        return new ListOfValue(key, valueEn, valueCy, order, parentCategory, parentKey, active);
    }

    private ReferenceRecord record(String key, String valueEn, String valueCy, Integer order,
                                   String parentCategory, String parentKey, boolean active) {
        return new ReferenceRecord(key, valueEn, valueCy, order, parentCategory, parentKey, active);
    }

    private FeignException feignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost", Map.of(), null,
                                         StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("getListOfValues", feign.Response.builder()
            .status(status).reason("error").request(request).headers(Map.of()).build());
    }

    private record Applied(List<ReferenceRecord> inserts, List<ReferenceRecord> updates) {
    }
}
