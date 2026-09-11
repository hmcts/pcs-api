// The accessibility audit runs once per navigation and costs ~843ms a scan. A regression run
// performs ~370 of them, so it is ~5 minutes of worker time and ~2.5 minutes of the run's wall
// clock across two workers. Agreed with QA to run it on nightly only.
//
// Explicit opt-in rather than inferring the pipeline: E2E_PIPELINE_TYPE is only set after the
// tests, and E2E_TEST_SCOPE happens to be nightly-only today but is not a statement of intent.
export const a11yEnabled = process.env.E2E_A11Y === 'true';
