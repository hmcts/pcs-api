import { AfterStep, BeforeStep } from './enforcement.fixtures';
import type { EnforcementWorld } from './enforcement.fixtures';
import { holdPage, pagePacingEnabled } from '@utils/page-pacing';
import {
  DemoSetUpStep,
  resolvedValuesIn,
  showDemoCaption,
  showDemoCaptionResolved,
} from '../support/demo-captions';
import { dashedCaseNumber } from '../support/bailiff-events';

/**
 * Demo project only (the fixtures turn pacing on there; elsewhere these hooks do nothing).
 *
 * Before each step: captions the step on screen. Set-up (Given steps and the Background, Gherkin's "Context"
 * steps) is captioned as such, so the viewer can tell it from the When and Then steps the scenario is about.
 * While nothing is loaded in the browser the caption comes with a card listing the scenario's set-up steps,
 * because set-up goes through the APIs and would otherwise be a blank page for as long as it takes.
 *
 * The caption also names the real value behind any value the step quotes illustratively — a generated warrant
 * number, the scenario's own case reference — both before the step and again afterwards, for the step that
 * generated the value itself.
 *
 * After each When and Then step: holds the screen so the outcome stays readable. A failed or pending step
 * is not held.
 */
BeforeStep(async ({ page, world, $bddContext, $testInfo }) => {
  if (!pagePacingEnabled()) {
    return;
  }
  try {
    const step = $bddContext.bddTestData.steps[$bddContext.stepIndex];
    const setUp = step?.keywordType === 'Context';
    const text = step?.textWithKeyword ?? $bddContext.step.title;
    await showDemoCaption(page, {
      scenario: $testInfo.title,
      step: text,
      setUp,
      resolved: resolvedValuesIn(text, illustrativeValues(world, text)),
      setUpCard: nothingLoaded(page.url())
        ? setUpSteps($bddContext.bddTestData.steps, $bddContext.stepIndex)
        : undefined,
    });
  } catch {
    // Only for the recording.
  }
});

AfterStep(async ({ page, world, $bddContext, $step }) => {
  if (!pagePacingEnabled() || $step.error !== undefined) {
    return;
  }
  const step = $bddContext.bddTestData.steps[$bddContext.stepIndex];
  try {
    const text = step?.textWithKeyword ?? $bddContext.step.title;
    await showDemoCaptionResolved(page, resolvedValuesIn(text, illustrativeValues(world, text)));
  } catch {
    // Only for the recording.
  }
  if (step?.keywordType === 'Action' || step?.keywordType === 'Outcome') {
    await holdPage(page);
  }
});

/** Whether the browser still has nothing in it: no document, or the context's starting page. */
const nothingLoaded = (url: string): boolean => !url || url.startsWith('about:');

/** The scenario's set-up steps, as the card lists them, with the ones already run ticked off. */
function setUpSteps(
  steps: { textWithKeyword?: string; keywordType?: string }[],
  stepIndex: number,
): DemoSetUpStep[] {
  return steps
    .map((step, index) => ({ text: step.textWithKeyword ?? '', keywordType: step.keywordType, done: index < stepIndex }))
    .filter(({ text, keywordType }) => text && keywordType === 'Context')
    .map(({ text, done }) => ({ text, done }));
}

/**
 * What the values this step quotes illustratively turned out to be in this run: the warrant and writ numbers
 * generated on approval, the dates and names the steps had to make up, and the case reference, which every
 * feature file quotes as a made-up one because every scenario creates its own case.
 */
function illustrativeValues(world: EnforcementWorld, step: string): Record<string, string> {
  const values: Record<string, string> = {
    ...world.enforcementNumbers,
    ...world.aliases,
    ...world.batchPrintCaseFor,
  };
  for (const [reference, actual] of Object.entries(values)) {
    if (/^\d{16}$/.test(actual)) {
      values[reference] = dashedCaseNumber(actual);
    }
  }
  if (world.caseReference) {
    for (const [, quoted] of step.matchAll(/"(\d{4}-\d{4}-\d{4}-\d{4})"/g)) {
      values[quoted] ??= dashedCaseNumber(world.caseReference);
    }
  }
  return values;
}
