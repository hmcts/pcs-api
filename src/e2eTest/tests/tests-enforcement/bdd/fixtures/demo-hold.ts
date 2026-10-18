import { AfterStep } from './enforcement.fixtures';
import { holdPage, pagePacingEnabled } from '@utils/page-pacing';

/**
 * In the demo project, holds the screen after each When and Then step so the outcome stays readable in the
 * video. The fixtures turn pacing on; elsewhere this does nothing. Given steps mostly set up data through the
 * API, and any page they open is already held by the page-change pause. A failed or pending step is not held.
 */
AfterStep(async ({ page, $bddContext, $step }) => {
  if (!pagePacingEnabled() || $step.error !== undefined) {
    return;
  }
  const keywordType = $bddContext.bddTestData.steps[$bddContext.stepIndex]?.keywordType;
  if (keywordType === 'Action' || keywordType === 'Outcome') {
    await holdPage(page);
  }
});
