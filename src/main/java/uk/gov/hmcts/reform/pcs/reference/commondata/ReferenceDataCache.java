package uk.gov.hmcts.reform.pcs.reference.commondata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReferenceDataCache {

    private final ReferenceDataRepository repository;
    private final Map<ReferenceDataSource, ReferenceTable> tables = new ConcurrentHashMap<>();

    public ReferenceTable reload(ReferenceDataSource source) {
        ReferenceTable table = repository.load(source);
        tables.put(source, table);
        return table;
    }

    public Optional<ReferenceRecord> find(ReferenceDataSource source, String key) {
        return Optional.ofNullable(table(source).records().get(key));
    }

    public Map<String, ReferenceRecord> records(ReferenceDataSource source) {
        return Collections.unmodifiableMap(table(source).records());
    }

    private ReferenceTable table(ReferenceDataSource source) {
        ReferenceTable table = tables.get(source);
        if (table != null) {
            return table;
        }
        ReferenceTable loaded = repository.load(source);
        ReferenceTable existing = tables.putIfAbsent(source, loaded);
        return existing != null ? existing : loaded;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(
        fixedDelayString = "${common-data.cache.refresh-interval}",
        initialDelayString = "${common-data.cache.refresh-interval}"
    )
    public void reloadAll() {
        for (ReferenceDataSource source : ReferenceDataSource.values()) {
            try {
                reload(source);
            } catch (RuntimeException ex) {
                log.error("Failed to refresh cached reference data for {}", source.getTableName(), ex);
            }
        }
    }
}
