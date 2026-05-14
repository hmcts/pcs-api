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


export function getCurrentGMTTime(): string {
  const now = new Date();

  const formatted = now.toLocaleString("en-GB", {
    timeZone: "UTC",
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


export function normalizeTimeWithoutSeconds(dateStr: string): string {
  const date = new Date(dateStr);
  date.setSeconds(0, 0);
  return date.toISOString();
}


