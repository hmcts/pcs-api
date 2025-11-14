/**
 * Journey Data Collector
 * 
 * Collects and stores answers as the test progresses through the journey.
 * This data can later be validated against the Check Your Answers (CYA) page.
 * 
 * Usage:
 *   const collector = JourneyDataCollector.getInstance();
 *   collector.setAnswer('What is the address?', '15 Garden Drive, Luton, Bedfordshire, LU1 1AB');
 *   collector.setAnswer('Who is the claimant?', 'A registered provider of social housing');
 *   
 *   // Later, validate against CYA page
 *   await validateCYA(page, collector);
 */

export interface JourneyAnswer {
  question: string;
  answer: string;
  timestamp?: Date;
}

export class JourneyDataCollector {
  private static instance: JourneyDataCollector;
  private answers: Map<string, JourneyAnswer> = new Map();
  private journeyName: string = 'default';

  private constructor() {}

  /**
   * Get singleton instance
   */
  static getInstance(): JourneyDataCollector {
    if (!JourneyDataCollector.instance) {
      JourneyDataCollector.instance = new JourneyDataCollector();
    }
    return JourneyDataCollector.instance;
  }

  /**
   * Reset collector for a new journey
   */
  reset(journeyName?: string): void {
    this.answers.clear();
    this.journeyName = journeyName || 'default';
  }

  /**
   * Set an answer for a question
   * @param question - The question text (as it appears on CYA page)
   * @param answer - The answer text (as it appears on CYA page)
   */
  setAnswer(question: string, answer: string): void {
    this.answers.set(question, {
      question,
      answer: this.normalizeAnswer(answer),
      timestamp: new Date()
    });
  }

  /**
   * Set multiple answers at once
   */
  setAnswers(answers: Record<string, string>): void {
    Object.entries(answers).forEach(([question, answer]) => {
      this.setAnswer(question, answer);
    });
  }

  /**
   * Get answer for a question
   */
  getAnswer(question: string): string | undefined {
    return this.answers.get(question)?.answer;
  }

  /**
   * Get all collected answers
   */
  getAllAnswers(): Map<string, JourneyAnswer> {
    return new Map(this.answers);
  }

  /**
   * Get answers as a simple key-value object
   */
  getAnswersAsObject(): Record<string, string> {
    const result: Record<string, string> = {};
    this.answers.forEach((value, key) => {
      result[key] = value.answer;
    });
    return result;
  }

  /**
   * Normalize answer text for comparison (trim, lowercase, remove extra spaces)
   */
  private normalizeAnswer(answer: string): string {
    return answer
      .trim()
      .replace(/\s+/g, ' ') // Replace multiple spaces with single space
      .toLowerCase();
  }

  /**
   * Check if an answer exists for a question
   */
  hasAnswer(question: string): boolean {
    return this.answers.has(question);
  }

  /**
   * Get journey name
   */
  getJourneyName(): string {
    return this.journeyName;
  }

  /**
   * Get count of collected answers
   */
  getAnswerCount(): number {
    return this.answers.size;
  }
}

