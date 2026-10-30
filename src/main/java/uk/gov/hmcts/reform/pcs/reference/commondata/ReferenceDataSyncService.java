package uk.gov.hmcts.reform.pcs.reference.commondata;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.reference.api.CommonDataApi;
import uk.gov.hmcts.reform.pcs.reference.dto.ListOfValue;
import uk.gov.hmcts.reform.pcs.reference.dto.ListOfValuesResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ReferenceDataSyncService {

    private static final String ACTIVE_FLAG = "Y";

    private final CommonDataApi commonDataApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final ReferenceDataRepository repository;
    private final ReferenceDataCache cache;
    private final String serviceId;

    public ReferenceDataSyncService(
        CommonDataApi commonDataApi,
        AuthTokenGenerator authTokenGenerator,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        ReferenceDataRepository repository,
        ReferenceDataCache cache,
        @Value("${common-data.service-id}") String serviceId
    ) {
        this.commonDataApi = commonDataApi;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.repository = repository;
        this.cache = cache;
        this.serviceId = serviceId;
    }

    public void synchronise() {
        List<String> failedCategories = new ArrayList<>();

        for (ReferenceDataSource source : ReferenceDataSource.values()) {
            try {
                synchronise(source);
            } catch (RuntimeException ex) {
                log.error("Reference data synchronisation failed for category {}", source.getCategoryId(), ex);
                failedCategories.add(source.getCategoryId());
            }
        }

        if (!failedCategories.isEmpty()) {
            throw new ReferenceDataSyncException(failedCategories);
        }
    }

    private void synchronise(ReferenceDataSource source) {
        Optional<List<ReferenceRecord>> incoming = fetch(source);
        if (incoming.isEmpty()) {
            return;
        }

        ReferenceTable current = cache.reload(source);
        Set<ReferenceColumn> persistedColumns = current.columns().keySet();

        List<ReferenceRecord> inserts = new ArrayList<>();
        List<ReferenceRecord> updates = new ArrayList<>();
        for (ReferenceRecord record : incoming.get()) {
            ReferenceRecord candidate = record.restrictedTo(persistedColumns);
            ReferenceRecord existing = current.records().get(candidate.key());
            if (existing == null) {
                if (candidate.active()) {
                    inserts.add(candidate);
                }
            } else if (!existing.equals(candidate)) {
                updates.add(candidate);
            }
        }

        if (inserts.isEmpty() && updates.isEmpty()) {
            log.info("Reference data for {} is up to date", source.getTableName());
            return;
        }

        repository.apply(current, inserts, updates);
        cache.reload(source);
        log.info("Reference data for {} synchronised: {} inserted, {} updated",
                 source.getTableName(), inserts.size(), updates.size());
    }

    private Optional<List<ReferenceRecord>> fetch(ReferenceDataSource source) {
        ListOfValuesResponse response;
        try {
            response = commonDataApi.getListOfValues(
                source.getCategoryId(),
                systemUpdateUserTokenProvider.getAuthToken(),
                authTokenGenerator.generate(),
                serviceId
            );
        } catch (FeignException.NotFound ex) {
            log.warn("Category {} is not available for service {}", source.getCategoryId(), serviceId);
            return Optional.empty();
        }

        if (response == null || response.listOfValues() == null || response.listOfValues().isEmpty()) {
            log.warn("No values returned for category {}; existing data left unchanged", source.getCategoryId());
            return Optional.empty();
        }

        return Optional.of(toRecords(source, response.listOfValues()));
    }

    private List<ReferenceRecord> toRecords(ReferenceDataSource source, List<ListOfValue> values) {
        Map<String, ReferenceRecord> recordsByKey = new LinkedHashMap<>();
        values.stream()
            .filter(Objects::nonNull)
            .map(this::toRecord)
            .forEach(record -> {
                if (record.key() == null) {
                    log.warn("Ignoring {} value without a key", source.getCategoryId());
                } else {
                    recordsByKey.merge(record.key(), record, this::preferActive);
                }
            });
        return List.copyOf(recordsByKey.values());
    }

    private ReferenceRecord preferActive(ReferenceRecord first, ReferenceRecord second) {
        return !first.active() && second.active() ? second : first;
    }

    private ReferenceRecord toRecord(ListOfValue value) {
        return new ReferenceRecord(
            clean(value.key()),
            clean(value.valueEn()),
            clean(value.valueCy()),
            value.lovOrder() == null ? null : value.lovOrder().intValue(),
            clean(value.parentCategory()),
            clean(value.parentKey()),
            ACTIVE_FLAG.equalsIgnoreCase(clean(value.activeFlag()))
        );
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replaceAll("^[\\s\\u00A0]+|[\\s\\u00A0]+$", "");
        return trimmed.isEmpty() ? null : trimmed;
    }
}
