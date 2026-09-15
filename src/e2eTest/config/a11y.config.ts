// Nightly only, agreed with QA: the audit runs per navigation and costs ~2.5 minutes of a run.
export const a11yEnabled = process.env.E2E_A11Y === 'true';
