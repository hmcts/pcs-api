/**
 * Demo project only: lets consecutive scenarios of one feature that share an identical Background run against
 * the same case and the same signed-in session, so the video is one continuous pass (support/demo-continuity.ts).
 *
 * The step hook is where the Background is recognised: playwright-bdd's test data says which steps came from
 * the Background and gives their resolved text, which is what the case is keyed on. Everywhere else these
 * hooks do nothing.
 */
import { BeforeStep } from './enforcement.fixtures';
import { enteringStep } from '../support/demo-continuity';

BeforeStep(async ({ world, $bddContext }) => {
  await enteringStep($bddContext.bddTestData.steps, $bddContext.stepIndex, world);
});
