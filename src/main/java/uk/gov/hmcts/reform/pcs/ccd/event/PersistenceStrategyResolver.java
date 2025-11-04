package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Determines whether a case's mutable data is handled by the
 * internal CCD database or delegated to an external, service implemented persistence handler.
 * This implementation uses prefix-based matching for case types to support the preview environment.
 */
@Service
@Slf4j
@ConfigurationProperties("ccd.decentralised")
public class PersistenceStrategyResolver {

    private Map<String, URI> caseTypeServiceUrls = Map.of();

    /**
     * Sets the case type service URLs when the application starts.
     * The keys in the map are converted to lowercase to ensure case-insensitivity.
     */
    public void setCaseTypeServiceUrls(Map<String, URI> caseTypeServiceUrls) {
        this.caseTypeServiceUrls = new HashMap<>();
        caseTypeServiceUrls.forEach((key, value) ->
            this.caseTypeServiceUrls.put(key.toLowerCase(), value)
        );
        if (this.caseTypeServiceUrls.isEmpty()) {
            log.info("No decentralised persistence URLs configured.");
        } else {
            log.info("##### RSS: Loaded {} decentralised case type route(s) for prefixes: {}",
                this.caseTypeServiceUrls.size(),
                this.caseTypeServiceUrls.keySet());
        }
    }

    public URI getTestUrl() {
        return caseTypeServiceUrls.get("pcs");
    }

}
