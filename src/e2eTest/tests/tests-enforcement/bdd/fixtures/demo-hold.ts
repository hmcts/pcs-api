import { AfterStep, BeforeStep } from './enforcement.fixtures';
import { holdPage, pagePacingEnabled } from '@utils/page-pacing';
import { showDemoCaption } from '../support/demo-captions';

/**
 * Demo project only (the fixtures turn pacing on there; elsewhere these hooks do nothing).
 *
 * Before each step: captions the step on screen. Set-up (Given steps and the Background, Gherkin's "Context"
 * steps) is captioned as such, so the viewer can tell it from the When and Then steps the scenario is about.
 *
 * After each When and Then step: holds the screen so the outcome stays readable. A failed or pending step
 * is not held.
 */
BeforeStep(async ({ page, $bddContext, $testInfo }) => {
  if (!pagePacingEnabled()) {
    return;
  }
  try {
    const step = $bddContext.bddTestData.steps[$bddContext.stepIndex];
    const setUp = step?.keywordType === 'Context';
    await showDemoCaption(page, {
      scenario: $testInfo.title,
      step: step?.textWithKeyword ?? $bddContext.step.title,
      setUp,
    });
  } catch {
    // Only for the recording.
  }
});

AfterStep(async ({ page, $bddContext, $step }) => {
  if (!pagePacingEnabled() || $step.error !== undefined) {
    return;
  }
  const keywordType = $bddContext.bddTestData.steps[$bddContext.stepIndex]?.keywordType;
  if (keywordType === 'Action' || keywordType === 'Outcome') {
    await holdPage(page);
  }
});
