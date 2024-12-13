import { SimpleGit, simpleGit } from 'simple-git';

export class ChangedTestsRunner {
  public static async run(): Promise<void> {
    try {
      const changedTestFiles: string[] = await this.getChangedTestFiles();
      await this.runPlaywrightTests(changedTestFiles);
    } catch (error) {
      throw new Error('Failed to run tests:' + error);
    }
  }

  private static git: SimpleGit = simpleGit();

  private static async getChangedTestFiles(): Promise<string[]> {
    try {
      const stagedDiff: string = await this.git.diff(['--name-only', '--cached']);
      const unstagedDiff: string = await this.git.diff(['--name-only']);

      const changedFiles = new Set([
        ...stagedDiff.split('\n').filter(Boolean),
        ...unstagedDiff.split('\n').filter(Boolean),
      ]);

      return Array.from(changedFiles).filter(file => file.endsWith('.spec.ts'));
    } catch (error) {
      throw new Error('Error detecting changed test files: ' + error);
    }
  }

  private static async runPlaywrightTests(testFiles: string[]): Promise<void> {
    if (testFiles.length === 0) {
      return;
    }
    try {
      const command: string = `yarn playwright test ${testFiles.join(' ')} --project chromium`;
      await this.execCommand(command);
    } catch (error) {
      throw new Error('Error running Playwright tests: ' + error);
    }
  }

  private static execCommand(command: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const child = require('child_process').exec(command, (error: Error) => {
        if (error) {
          reject(error);
        } else {
          resolve();
        }
      });
      child.stdout.pipe(process.stdout);
      child.stderr.pipe(process.stderr);
    });
  }
}

ChangedTestsRunner.run().catch(error => {
  throw new Error('Execution failed:' + error);
});
