import { Before, Step, test } from './enforcement.fixtures';

export type PendingScope = { ticket?: string; summary: string };

function scenarioTickets(tags: string[]): string[] {
  return tags.filter(tag => /^@HDPI-\d+$/.test(tag)).map(tag => tag.slice(1));
}

function pendingMessage(scope: PendingScope, tags: string[]): string {
  const delivering = scope.ticket ? ` (${scope.ticket})` : '';
  return `Pending${delivering}: ${scope.summary}. Scenario tickets: ${scenarioTickets(tags).join(', ')}`;
}

export function markPending(scope: PendingScope, tags: string[]): void {
  test.fixme(true, pendingMessage(scope, tags));
}

export function approximatePrecondition(ticket: string, description: string): void {
  test.info().annotations.push({ type: 'approximated precondition', description: `${ticket}: ${description}` });
}

export function pendingSteps(scope: PendingScope, expressions: string[]): void {
  for (const expression of expressions) {
    Step(expression, { arityCheck: false }, async ({ $tags }) => markPending(scope, $tags));
  }
}

Before({ tags: '@pending' }, async ({ $tags }) => {
  markPending({ summary: 'scenario tagged @pending' }, $tags);
});
