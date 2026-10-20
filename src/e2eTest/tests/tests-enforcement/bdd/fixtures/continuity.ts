// Demo project only: lets consecutive scenarios of one feature that share an identical Background run against
// the same case and the same signed-in session (support/demo-continuity.ts).
import { BeforeStep } from './enforcement.fixtures';
import { enteringStep } from '../support/demo-continuity';

BeforeStep(async ({ world, $bddContext }) => {
  await enteringStep($bddContext.bddTestData.steps, $bddContext.stepIndex, world);
});
