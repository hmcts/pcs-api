package uk.gov.hmcts.reform.pcs.reference.commondata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataCacheTest {

    private static final ReferenceDataSource SOURCE = ReferenceDataSource.HEARING_PRIORITY;

    @Mock
    private ReferenceDataRepository repository;

    @InjectMocks
    private ReferenceDataCache underTest;

    @Test
    void shouldBeEmptyBeforeFirstLoad() {
        // when / then
        assertThat(underTest.records(SOURCE)).isEmpty();
        assertThat(underTest.find(SOURCE, "Urgent")).isEmpty();
    }

    @Test
    void shouldServeValuesFromTheLastReload() {
        // given
        ReferenceRecord urgent = new ReferenceRecord("Urgent", "Urgent", "Brys", null, null, null, true);
        when(repository.load(SOURCE)).thenReturn(table(urgent));

        // when
        underTest.reload(SOURCE);

        // then
        assertThat(underTest.find(SOURCE, "Urgent")).contains(urgent);
        assertThat(underTest.records(SOURCE)).containsOnlyKeys("Urgent");
    }

    @Test
    void shouldReplaceCachedValuesWhenTheDatabaseChanges() {
        // given
        ReferenceRecord before = new ReferenceRecord("Urgent", "Urgent", null, null, null, null, true);
        ReferenceRecord after = new ReferenceRecord("Urgent", "Very urgent", null, null, null, null, true);
        when(repository.load(SOURCE)).thenReturn(table(before)).thenReturn(table(after));

        // when
        underTest.reload(SOURCE);
        underTest.reload(SOURCE);

        // then
        assertThat(underTest.find(SOURCE, "Urgent")).contains(after);
    }

    @Test
    void shouldLoadEveryReferenceTableWhenRefreshingAll() {
        // given
        when(repository.load(any())).thenReturn(table());

        // when
        underTest.reloadAll();

        // then
        for (ReferenceDataSource source : ReferenceDataSource.values()) {
            verify(repository, times(1)).load(source);
        }
    }

    @Test
    void shouldKeepPreviousValuesAndCarryOnWhenRefreshOfATableFails() {
        // given
        ReferenceRecord urgent = new ReferenceRecord("Urgent", "Urgent", null, null, null, null, true);
        when(repository.load(any())).thenReturn(table());
        when(repository.load(SOURCE)).thenReturn(table(urgent)).thenThrow(new IllegalStateException("db down"));
        underTest.reload(SOURCE);

        // when
        underTest.reloadAll();

        // then
        assertThat(underTest.find(SOURCE, "Urgent")).contains(urgent);
        verify(repository).load(ReferenceDataSource.JUDGE_TYPE);
    }

    private ReferenceTable table(ReferenceRecord... records) {
        Map<String, ReferenceRecord> byKey = new LinkedHashMap<>();
        for (ReferenceRecord record : records) {
            byKey.put(record.key(), record);
        }
        return new ReferenceTable("hearing_priority", Map.of(), true, byKey);
    }
}
