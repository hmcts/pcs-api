import { performAction, performValidation } from '@utils/controller';
import { caseFlags } from '@data/page-data-figma/caseFlags.page.data';

export async function caseFlagsErrorValidation(): Promise<void> {
  // Test: Case flags page loads with expected heading and question
  await performValidation('mainHeader', caseFlags.mainHeader);
  await performValidation('text', {
    elementType: 'legend',
    text: caseFlags.whereShouldThisFlagBeAddedQuestion,
  });

  // Test: continue without selecting a flag location should show a validation error
  await performAction('clickButton', caseFlags.continueButton);
  await performValidation('errorMessage', {
    header: 'There is a problem',
    message: 'Select where the flag should be added',
  });
}
