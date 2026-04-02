package model;

/** Difficulty level for AI-controlled players. */
public enum Difficulty {
    /** Naive play: takes the first 3 available gems, buys the first affordable card, never reserves. */
    EASY,

    /** Fixed greedy AI: need-based gem collection, buys best-scored card, smarter reservations. */
    MEDIUM,

    /**
     * Full board analysis: builds a discount engine, tracks opponent threat level,
     * shifts to endgame urgency when any player hits 12+ pts, and blocks leaders.
     */
    HARD,

    /**
     * AI-powered: sends the full board state to the Claude API and lets the model
     * choose the optimal move. Falls back to HARD if the API call fails.
     */
    INSANE
}
