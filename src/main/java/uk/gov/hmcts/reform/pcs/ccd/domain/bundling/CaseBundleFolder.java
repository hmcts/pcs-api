package uk.gov.hmcts.reform.pcs.ccd.domain.bundling;

import java.util.List;

/**
 * One folder of a generated bundle, after em-stitching-api's BundleFolderDTO. Folders are the
 * case file view categories for now, in their display order.
 */
public record CaseBundleFolder(
    String folderName,
    String categoryId,
    int sortIndex,
    List<CaseBundleDocument> documents
) {
}
