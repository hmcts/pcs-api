import { Page } from '@playwright/test';
import config from '../config';

export abstract class Base {
  constructor(protected readonly page: Page) {}

  async goto(url: string) {
    await this.page.goto(url);
  }
}
