// Session variable types
export type SessionValue = string | number | boolean | object | null | undefined;
export type SessionData = Record<string, SessionValue>;
export class SessionManager {
  private static instance: SessionManager;
  private sessionData: SessionData = {};
  private constructor() {}
  static getInstance(): SessionManager {
    if (!SessionManager.instance) {
      SessionManager.instance = new SessionManager();
    }
    return SessionManager.instance;
  }
  set(key: string, value: SessionValue): void {
    this.sessionData[key] = value;
    console.log(`Session variable set: ${key} = ${JSON.stringify(value)}`);
  }
  get<T = SessionValue>(key: string): T | undefined {
    const value = this.sessionData[key] as T;
    console.log(`Session variable retrieved: ${key} = ${JSON.stringify(value)}`);
    return value;
  }
}
