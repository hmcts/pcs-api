package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.bundling.managed.CaseBundles;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

/** All document writes, including cascades, plus changes that make application documents visible. */
@Component
public class CaseBundleInvalidation {
    private final ObjectProvider<CaseBundles> bundles;

    public CaseBundleInvalidation(ObjectProvider<CaseBundles> bundles) {
        this.bundles = bundles;
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    public void changed(Object entity) {
        CaseBundles managed = bundles.getIfAvailable();
        if (managed == null) {
            return;
        }
        PcsCaseEntity pcsCase = switch (entity) {
            case DocumentEntity document -> document.getPcsCase();
            case GenAppEntity application -> application.getPcsCase();
            case CounterClaimEntity counterClaim -> counterClaim.getPcsCase();
            default -> null;
        };
        // Only the association identifier is read; no lazy load or repository query during flush.
        if (pcsCase != null) {
            managed.invalidate(CaseBundleDefinition.NAME, pcsCase.getId().toString());
        }
    }
}
