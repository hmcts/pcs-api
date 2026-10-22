import { Given } from '../fixtures/enforcement.fixtures';
import { backdateWarrantGeneration } from '../support/warrant-backdate';

/** Comfortably inside and outside the free period, away from the boundary the pcs-api tests cover. */
const INSIDE_FREE_PERIOD_MONTHS = 6;
const OUTSIDE_FREE_PERIOD_MONTHS = 13;

Given('that warrant was generated less than 12 calendar months ago', async ({ world }) => {
  await backdateWarrantGeneration(world, INSIDE_FREE_PERIOD_MONTHS);
});

Given('that warrant was generated more than 12 calendar months ago', async ({ world }) => {
  await backdateWarrantGeneration(world, OUTSIDE_FREE_PERIOD_MONTHS);
});
