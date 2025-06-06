import { AxeBuilder } from '@axe-core/playwright';
import { Page, expect } from '@playwright/test';
import fs from 'fs';

export class AxeTest {
  public static async run(page: Page): Promise<void> {
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22a', 'wcag22aa'])
      .analyze();

    const violations = results.violations;

    if (violations.length > 0) {
      fs.writeFileSync('axe-results.json', JSON.stringify(results, null, 2));

      console.error(`\nAccessibility violations found on ${page.url()}:`);
      violations.forEach(v => {
        console.log(`\n[${v.impact}] ${v.id} - ${v.description}`);
        v.nodes.forEach(node => {
          console.log(`  Affected element: ${node.target}`);
          console.log(`  Failure summary: ${node.failureSummary}`);
        });
      });

      throw new Error(`Accessibility issues found on page: ${page.url()}`);
    }

    expect.soft(violations).toEqual([]);
  }
}
