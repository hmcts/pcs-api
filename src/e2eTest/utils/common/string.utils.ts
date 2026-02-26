/**
 * Escapes special regex characters in a string so it can be used safely in RegExp.
 */
export function escapeForRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
