import { Page } from '@playwright/test';
import { SessionUtils } from '@hmcts/playwright-common';
import * as fs from 'fs';
import * as path from 'path';

export class SessionManager {
  private static readonly SESSION_DIR = path.join(process.cwd(), '.auth');
  // Common IDAM session cookie names - can be overridden via SESSION_COOKIE_NAME env var
  private static readonly SESSION_COOKIE_NAME = process.env.SESSION_COOKIE_NAME || 'Idam.Session';
  private static readonly STORAGE_STATE_FILE = 'storage-state.json';

  /**
   * Gets the storage state file path (Playwright's native format)
   */
  static getStorageStatePath(): string {
    if (!fs.existsSync(this.SESSION_DIR)) {
      fs.mkdirSync(this.SESSION_DIR, { recursive: true });
    }
    return path.join(this.SESSION_DIR, this.STORAGE_STATE_FILE);
  }

  /**
   * Checks if a session is valid using SessionUtils from @hmcts/playwright-common
   * Uses the storageState file which contains cookies in Playwright's format
   */
  static isSessionValid(): boolean {
    const storageStatePath = this.getStorageStatePath();
    try {
      // SessionUtils expects a file with cookies array, storageState has cookies in it
      return SessionUtils.isSessionValid(storageStatePath, this.SESSION_COOKIE_NAME);
    } catch (error) {
      return false;
    }
  }

  /**
   * Saves storage state for use in playwright config
   * This is Playwright's native way to persist authentication state
   */
  static async saveStorageState(page: Page): Promise<void> {
    const storageStatePath = this.getStorageStatePath();
    const storageState = await page.context().storageState();
    fs.writeFileSync(storageStatePath, JSON.stringify(storageState, null, 2), 'utf-8');
  }

  /**
   * Clears the storage state file
   */
  static clearStorageState(): void {
    const storageStatePath = this.getStorageStatePath();
    if (fs.existsSync(storageStatePath)) {
      fs.unlinkSync(storageStatePath);
    }
  }

  /**
   * Clears all session files
   */
  static clearAllSessions(): void {
    if (fs.existsSync(this.SESSION_DIR)) {
      const files = fs.readdirSync(this.SESSION_DIR);
      files.forEach(file => {
        const filePath = path.join(this.SESSION_DIR, file);
        if (fs.statSync(filePath).isFile()) {
          fs.unlinkSync(filePath);
        }
      });
    }
  }
}

