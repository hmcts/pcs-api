import { Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

class FlowchartLogger {
  private static instance: FlowchartLogger;
  private filePath: string = path.join(process.cwd(), '/generators/output/flowchart.txt');
  private previousNode: string = '';
  private currentAlphabet: string = 'A';
  private alphabetStepCount: number = 0;
  private isEnabled: boolean = false;
  private pageHistory: Set<string> = new Set();
  private isFirstTest: boolean = true;

  private constructor() {}

  public static getInstance(): FlowchartLogger {
    if (!FlowchartLogger.instance) {
      FlowchartLogger.instance = new FlowchartLogger();
    }
    return FlowchartLogger.instance;
  }

  public enable(): void {
    this.isEnabled = true;
  }

  public disable(): void {
    this.isEnabled = false;
  }

  public resetForNewTest(): void {
    this.previousNode = '';
    this.alphabetStepCount = 0;
    this.pageHistory.clear();

    const currentCharCode = this.currentAlphabet.charCodeAt(0);
    const nextCharCode = currentCharCode === 90 ? 65 : currentCharCode + 1;
    this.currentAlphabet = String.fromCharCode(nextCharCode);

    // Only create file header for the first test
    if (this.isFirstTest) {
      if (fs.existsSync(this.filePath)) {
        fs.unlinkSync(this.filePath);
      }
      fs.writeFileSync(this.filePath, '```mermaid\ngraph TD\n');
      this.isFirstTest = false;
    }
  }

  public async logNavigation(page: Page): Promise<void> {
    if (!this.isEnabled) return;

    try {
      const { header: currentHeader, source: currentSource } = await this.getPageHeaderWithSource(page);
      const currentUrl = page.url();

      const pageIdentifier = `${currentUrl}|${currentHeader}`;

      // Skip duplicates
      if (this.pageHistory.has(pageIdentifier)) {
        return;
      }

      const nodeId = `${this.currentAlphabet}${this.alphabetStepCount}`;
      let entry = '';

      if (this.previousNode) {
        entry += `    ${this.previousNode} --> ${nodeId}\n`;
      }

      let displayHeader = currentHeader;
      if (currentSource !== 'h1') {
        displayHeader = `${currentHeader} - ${currentSource}`;
      }

      entry += `    ${nodeId}["${displayHeader}"]\n`;

      fs.appendFileSync(this.filePath, entry);

      this.previousNode = nodeId;
      this.pageHistory.add(pageIdentifier);
      this.alphabetStepCount++;

    } catch (error) {
      console.error('Error logging navigation:', error);
    }
  }

  public async forceLogFinalPage(page: Page): Promise<void> {
    if (!this.isEnabled) return;
    await this.logNavigation(page);
  }

  // Call this at the very end of all tests
  public closeFlowchart(): void {
    if (fs.existsSync(this.filePath)) {
      fs.appendFileSync(this.filePath, '```\n');
    }
  }

  private async getPageHeaderWithSource(page: Page): Promise<{ header: string; source: string }> {
    try {
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(500);

      // H1 - FIRST PRIORITY
      const h1Locator = page.locator('h1').first();
      const h1Count = await h1Locator.count().catch(() => 0);

      if (h1Count > 0) {
        const h1Text = await h1Locator.textContent().catch(() => null);
        if (h1Text?.trim() && !this.isCookiesHeader(h1Text)) {
          return { header: this.formatHeader(h1Text), source: 'h1' };
        }
      }

      // H2 - SECOND PRIORITY
      const h2Locator = page.locator('h2');
      const h2Count = await h2Locator.count().catch(() => 0);

      for (let i = 0; i < h2Count; i++) {
        const h2Text = await h2Locator.nth(i).textContent().catch(() => null);
        if (h2Text?.trim() && !this.isCookiesHeader(h2Text)) {
          return { header: this.formatHeader(h2Text), source: 'h2' };
        }
      }

      // Page title fallback
      const pageTitle = await page.title().catch(() => '');
      if (pageTitle && !this.isCookiesHeader(pageTitle)) {
        return { header: this.formatHeader(pageTitle), source: 'title' };
      }

      return { header: 'Page Content', source: 'content' };

    } catch (error) {
      return { header: 'Page Loaded', source: 'loaded' };
    }
  }

  private isCookiesHeader(header: string): boolean {
    const lowerHeader = header.toLowerCase();
    return lowerHeader.includes('cookie');
  }

  private formatHeader(header: string): string {
    return header
      .replace(/"/g, "'")
      .replace(/\s+/g, ' ')
      .trim()
      .substring(0, 60);
  }
}

export const flowchartLogger = FlowchartLogger.getInstance();
