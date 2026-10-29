package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import jakarta.persistence.PostLoad;
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
import uk.gov.hmcts.reform.pcs.ccd.view.DocumentsView;

import java.util.Objects;

/**
 * All document writes, including cascades, plus application and counterclaim changes that show or
 * hide their documents. Other application and counterclaim writes leave the bundle as it is.
 */
@Component
public class CaseBundleChangeListener {
    private final ObjectProvider<CaseBundles> bundles;

    public CaseBundleChangeListener(ObjectProvider<CaseBundles> bundles) {
        this.bundles = bundles;
    }

    @PostLoad
    public void loaded(Object entity) {
        remember(entity);
    }

    @PostPersist
    public void persisted(Object entity) {
        remember(entity);
        // A new application's or counterclaim's documents are persisted too, and queue the bundle themselves.
        if (entity instanceof DocumentEntity) {
            changed(entity);
        }
    }

    @PostUpdate
    public void updated(Object entity) {
        boolean affectsBundle = entity instanceof DocumentEntity
            || entity instanceof GenAppEntity application && !Objects.equals(application.getVisibleWhenLoaded(),
                DocumentsView.isVisibleToInternalReaders(application))
            || entity instanceof CounterClaimEntity counterClaim && !Objects.equals(
                counterClaim.getVisibleWhenLoaded(), DocumentsView.isVisibleToInternalReaders(counterClaim));
        if (affectsBundle) {
            changed(entity);
        }
        remember(entity);
    }

    @PostRemove
    public void removed(Object entity) {
        changed(entity);
    }

    private static void remember(Object entity) {
        if (entity instanceof GenAppEntity application) {
            application.setVisibleWhenLoaded(DocumentsView.isVisibleToInternalReaders(application));
        } else if (entity instanceof CounterClaimEntity counterClaim) {
            counterClaim.setVisibleWhenLoaded(DocumentsView.isVisibleToInternalReaders(counterClaim));
        }
    }

    private void changed(Object entity) {
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
            managed.changed(CaseBundleDefinition.NAME, pcsCase.getId().toString());
        }
    }
}
