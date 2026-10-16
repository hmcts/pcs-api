import { Before, Step, test } from './enforcement.fixtures';

/** What a pending step is waiting for: the Jira ticket that delivers it and a short description. */
export type PendingScope = { ticket?: string; summary: string };

function scenarioTickets(tags: string[]): string[] {
  return tags.filter(tag => /^@HDPI-\d+$/.test(tag)).map(tag => tag.slice(1));
}

function pendingMessage(scope: PendingScope, tags: string[]): string {
  const delivering = scope.ticket ? ` (${scope.ticket})` : '';
  return `Pending${delivering}: ${scope.summary}. Scenario tickets: ${scenarioTickets(tags).join(', ')}`;
}

/** Marks the running scenario as fixme. Use for behaviour that does not exist in pcs-api / XUI yet. */
export function markPending(scope: PendingScope, tags: string[]): void {
  test.fixme(true, pendingMessage(scope, tags));
}

/** Records a precondition the app cannot set up yet but does not depend on today, so the scenario carries on. */
export function approximatePrecondition(ticket: string, description: string): void {
  test.info().annotations.push({ type: 'approximated precondition', description: `${ticket}: ${description}` });
}

/**
 * Registers step expressions whose behaviour is not built yet. Each one marks the scenario as fixme,
 * naming the delivering ticket and the scenario's @HDPI tags. Replace an expression with a real step once
 * the behaviour exists.
 */
export function pendingSteps(scope: PendingScope, expressions: string[]): void {
  for (const expression of expressions) {
    Step(expression, { arityCheck: false }, async ({ $tags }) => markPending(scope, $tags));
  }
}

// Scenarios tagged @pending have no implemented steps beyond set-up: skip them before any case is created.
Before({ tags: '@pending' }, async ({ $tags }) => {
  markPending({ summary: 'scenario tagged @pending' }, $tags);
});
