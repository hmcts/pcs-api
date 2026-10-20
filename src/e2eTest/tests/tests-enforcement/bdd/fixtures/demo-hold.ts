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

const nothingLoaded = (url: string): boolean => !url || url.startsWith('about:');

function setUpSteps(
  steps: { textWithKeyword?: string; keywordType?: string }[],
  stepIndex: number,
): DemoSetUpStep[] {
  return steps
    .map((step, index) => ({ text: step.textWithKeyword ?? '', keywordType: step.keywordType, done: index < stepIndex }))
    .filter(({ text, keywordType }) => text && keywordType === 'Context')
    .map(({ text, done }) => ({ text, done }));
}

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
