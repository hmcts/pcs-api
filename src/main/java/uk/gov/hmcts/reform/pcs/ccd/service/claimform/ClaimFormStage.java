package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

/**
 * The stages of claim-form generation, used to attribute a terminal failure to the step that broke
 * (so support can tell a Docmosis render problem from a CDAM store problem without reading a stack
 * trace). Surfaced as the {@code failureStage} dimension on the terminal failure log / App Insights.
 */
public enum ClaimFormStage {
    /** Building the payload from case data (read-only transaction). */
    PAYLOAD,
    /** Rendering the PDF via Docmosis / dg-docassembly. */
    RENDER,
    /** Storing/attaching the document to the case via CDAM. */
    STORE
}
