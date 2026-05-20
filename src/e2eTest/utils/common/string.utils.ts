/**
 * Escapes special regex characters in a string so it can be used safely in RegExp.
 */
export function escapeForRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Returns a RegExp that matches the exact text with optional leading/trailing whitespace.
 */
export function exactTextWithOptionalWhitespaceRegex(text: string): RegExp {
  return new RegExp('^\\s*' + escapeForRegex(text) + '\\s*$');
}

/**
 * Returns a formatted British standard time
 */
export function getCurrentBSTTime(): string {
  const now = new Date();
  const formatted = now.toLocaleString("en-GB", {
    timeZone: "Europe/London",
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
    second: "2-digit",
    hour12: true,
  });

  return formatted.replace(/am|pm/, (match) => match.toUpperCase());
}
