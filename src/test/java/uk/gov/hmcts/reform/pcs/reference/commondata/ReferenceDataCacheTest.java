package uk.gov.hmcts.reform.pcs.reference.commondata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void shouldLoadFromTheDatabaseWhenATableHasNotBeenCachedYet() {
        // given
        ReferenceRecord urgent = new ReferenceRecord("Urgent", "Urgent", null, null, null, null, true);
        when(repository.load(SOURCE)).thenReturn(table(urgent));

        // when
        Optional<ReferenceRecord> found = underTest.find(SOURCE, "Urgent");
        Map<String, ReferenceRecord> records = underTest.records(SOURCE);

        // then
        assertThat(found).contains(urgent);
        assertThat(records).containsOnlyKeys("Urgent");
        verify(repository, times(1)).load(SOURCE);
    }

    @Test
    void shouldNotQueryTheDatabaseWhenAKeyIsMissingFromACachedTable() {
        // given
        ReferenceRecord urgent = new ReferenceRecord("Urgent", "Urgent", null, null, null, null, true);
        when(repository.load(SOURCE)).thenReturn(table(urgent));
        underTest.reload(SOURCE);

        // when
        Optional<ReferenceRecord> found = underTest.find(SOURCE, "Unknown");

        // then
        assertThat(found).isEmpty();
        verify(repository, times(1)).load(SOURCE);
    }

    @Test
    void shouldPropagateTheFailureWhenAnUncachedTableCannotBeLoaded() {
        // given
        when(repository.load(SOURCE)).thenThrow(new IllegalStateException("db down"));

        // when / then
        assertThatThrownBy(() -> underTest.find(SOURCE, "Urgent"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> underTest.records(SOURCE))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotOverwriteANewerTableLoadedWhileAnUncachedTableWasBeingRead() {
        // given
        ReferenceRecord stale = new ReferenceRecord("Urgent", "Stale", null, null, null, null, true);
        ReferenceRecord fresh = new ReferenceRecord("Urgent", "Fresh", null, null, null, null, true);
        AtomicInteger calls = new AtomicInteger();
        when(repository.load(SOURCE)).thenAnswer(invocation -> {
            if (calls.getAndIncrement() == 0) {
                underTest.reload(SOURCE);
                return table(stale);
            }
            return table(fresh);
        });

        // when
        Optional<ReferenceRecord> found = underTest.find(SOURCE, "Urgent");

        // then
        assertThat(found).contains(fresh);
        assertThat(underTest.find(SOURCE, "Urgent")).contains(fresh);
    }

    @Test
    void shouldNotAllowCallersToModifyTheCachedRecords() {
        // given
        ReferenceRecord urgent = new ReferenceRecord("Urgent", "Urgent", null, null, null, null, true);
        when(repository.load(SOURCE)).thenReturn(table(urgent));

        // when
        Map<String, ReferenceRecord> records = underTest.records(SOURCE);

        // then
        assertThatThrownBy(() -> records.put("Other", urgent))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(underTest.find(SOURCE, "Other")).isEmpty();
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
