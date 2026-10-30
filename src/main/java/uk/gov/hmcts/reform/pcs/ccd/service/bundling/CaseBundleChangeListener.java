package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.hibernate.Hibernate;
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
 * Writes to documents in the bundle, including cascades, plus application and counterclaim changes
 * that show or hide their documents. Other writes leave the bundle as it is, so are not re-rendered.
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
        if (entity instanceof DocumentEntity document && mayBeInBundle(document)) {
            changed(entity);
        }
    }

    @PostUpdate
    public void updated(Object entity) {
        boolean affectsBundle = entity instanceof DocumentEntity document && mayBeInBundle(document)
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
        boolean affectsBundle = switch (entity) {
            case DocumentEntity document -> mayBeInBundle(document);
            case GenAppEntity application -> DocumentsView.isVisibleToInternalReaders(application);
            case CounterClaimEntity counterClaim -> DocumentsView.isVisibleToInternalReaders(counterClaim);
            default -> false;
        };
        if (affectsBundle) {
            changed(entity);
        }
    }

    // A document's application or counterclaim is only checked if already loaded, as loading it
    // during flush is not allowed. A document that moves into or out of the bundle when its
    // application or counterclaim is issued is queued by that entity's update.
    private static boolean mayBeInBundle(DocumentEntity document) {
        return !Hibernate.isInitialized(document.getGeneralApplication())
            || !Hibernate.isInitialized(document.getCounterClaim())
            || CaseBundleDefinition.isInBundle(document);
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
