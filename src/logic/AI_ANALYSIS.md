# Splendor AI Logic Analysis

**File:** `src/logic/SplendorAI.java`
**Date:** 2026-04-02

---

## Overview

`SplendorAI` is a **greedy, single-turn, rule-based bot**. It has no memory of past turns, no opponent modeling, and no lookahead — every decision is made solely from the current game state. Despite its simplicity, most of its heuristics are well-aligned with solid Splendor strategy.

---

## Decision Pipeline

The bot evaluates actions in strict priority order and returns the first one that succeeds:

| Priority | Action | Method |
|---|---|---|
| 1 | Buy the best affordable visible card | `tryBuyCard` |
| 2 | Buy an affordable reserved card | `tryBuyReserved` |
| 3 | Reserve a promising card (or blind-draw) | `tryReserveCard` |
| 4 | Take the 3 most-needed gem colors | `tryTakeGems` |
| 5 | Fallback: take any 3 available gems | `takeThreeAvailable` |

---

## Action-by-Action Assessment

### 1. Buying a Visible Card — `tryBuyCard`

**Scoring formula:**
```
score = prestigePoints × 10
      + 5  (if card's bonus color helps toward any noble)
      + 2  (if card is level 3)
```

**What is good:**
- Prioritising prestige points is correct — winning requires reaching 15 points.
- Noble awareness is a real strategic consideration; cards that push toward multiple nobles simultaneously are very strong.
- Preferring level-3 cards as a tiebreaker is sound since they typically offer more points per cost.

**What is missing:**
- The score **ignores the card's bonus gem color** (discount value). A 0-point card that gives a blue bonus may be far more valuable than a 1-point card of an irrelevant color if several target cards need blue.
- The score **ignores cost efficiency** (points per gem spent). Two 3-point cards could have very different cost structures.
- No consideration of **opponent proximity to winning** — the bot never rushes or changes strategy based on threat level.

---

### 2. Buying a Reserved Card — `tryBuyReserved`

**Logic:** Buy the first reserved card that is currently affordable.

**What is good:**
- Correctly checks reserved cards separately since they use a different command format (`3:r:index`).

**What is missing:**
- Buys the **first** affordable reserved card rather than the **best** one. If the player has reserved two affordable cards, it should score and pick the better one using the same `scoreCardForPurchase` logic.

---

### 3. Reserving a Card — `tryReserveCard`

**Targeting logic:**
- Only reserves cards that need **1–2 more tokens** to afford (close targets).
- Scoring: `prestigePoints × 3 + (4 − tokensNeeded)` — prefers high-point cards that are nearly affordable.
- Falls back to a **blind draw from the lowest available deck** if no good target is found.

**What is good:**
- The "1–2 tokens away" threshold is strategically sound — reserving something 5 tokens away wastes the gold coin and a hand slot.
- Rewarding prestige points in the reserve score is correct.
- Reserving to **block opponents** from taking key cards is a core Splendor tactic that many bots overlook — this bot does not do it explicitly, but the targeting still reserves high-value cards, which has a similar incidental effect.

**What is missing / problematic:**
- **Blind deck reservation is weak.** Drawing blindly from the lowest deck (level 1) is almost always a poor move. Level-1 cards rarely offer prestige points and the gold coin is spent speculatively. The bot should skip blind draws unless the hand is empty and no gems can be taken.
- **No block-reservation logic.** The bot never explicitly reserves a card to stop a human player who is one card away from winning.
- **tokensNeededForCard does not account for current hand tokens.** The method subtracts bonuses but does not subtract tokens already held by the player, meaning "tokens needed" can be inflated. (See bug note below.)

---

### 4. Taking Gems — `tryTakeGems`

**Logic:**
1. Compute how much of each color is needed across all visible + reserved cards (deficit vs. current holdings).
2. Take the top-3 needed colors that are available on the board.
3. If a single color has a need score ≥ 2 and 4+ tokens of it are on the board, take 2 of that color instead.

**What is good:**
- Analysing need across the entire visible board (not just one target card) is strategically correct — it picks colors that are broadly useful.
- Including **reserved cards** in the need calculation correctly weighs cards the bot is actively working toward.
- Taking 2 of the same color when heavily needed is a real tactic, and the "need ≥ 2 and board has 4+" guard is correct per game rules.
- Skipping colors already on hand via the `req > p.getTokenCount(t)` filter avoids hoarding tokens of colors already satisfied.

**What is missing / problematic:**
- **`tryTakeGems` has a dead-code block.** Lines 167–169 repeat the `canTakeThreeDifferent` check after the "take 2" loop, but this branch can never fire because the method would have already returned on success or reached `return null`. This is a logic bug — the second three-gem check was probably intended as a second attempt after the take-2 block fails, but it is unreachable.
- **No consideration of the 10-token limit.** The bot can take gems it will immediately have to return, wasting a turn.
- **Gem need analysis does not weigh card reachability.** A level-3 card that costs 7 tokens is included in need calculations equally with a level-1 card that costs 2, even though the level-3 card is far from reachable.

---

### 5. Fallback: Take Any Three — `takeThreeAvailable`

**Logic:** Takes the first 3 gem colors that have at least 1 token on the board.

**What is good:**
- A safe fallback that ensures the bot always makes a valid move.

**What is missing:**
- Even in fallback, the bot should prefer colors it currently holds fewer of, rather than always picking the first 3 in `GEM_COLORS` order (GREEN, WHITE, BLUE, BLACK, RED). This makes the bot slightly predictable.

---

### 6. Token Return — `chooseTokensToReturn`

**Logic:** When over 10 tokens, return the tokens of the color(s) with the lowest need score first. Gold is only returned as a last resort.

**What is good:**
- Protecting gold tokens is exactly right — gold is the most flexible resource in the game.
- Using the same need-scoring system as gem collection creates consistent decision-making.

**What is missing:**
- The need score used here (`howMuchWeNeedEachColor`) sums raw unmet costs across **all visible cards**, not just the ones the bot is targeting. This can over-count need for cards that are far from reachable, potentially leading the bot to hold tokens for cards it will never buy in a reasonable time.

---

## Bugs and Code Issues

| Location | Issue | Impact |
|---|---|---|
| `tryTakeGems`, lines 167–169 | Dead `canTakeThreeDifferent` check after the take-2 loop — unreachable code | Minor (no incorrect behavior, just dead code) |
| `tryReserveCard`, blind draw | Always draws from **lowest** deck level, not highest | Moderate — level-1 blind draws are almost never useful |
| `tryBuyReserved` | Buys **first** affordable reserved card, not best | Minor — can miss better reserved options |
| `addCardNeed` vs `howMuchWeNeedColor` | `addCardNeed` does **not** subtract current token holdings, while `getTop3NeededColors` does — these two need-calculation methods are inconsistent | Moderate — `howMuchWeNeedEachColor` (used for token return) may overstate need |

---

## Strategic Assessment

### What the bot does well

- **Buys cards when it can** — never delays a purchase unnecessarily.
- **Noble awareness** — factoring in noble progress when scoring cards is a legitimate and important heuristic.
- **Gem collection is card-driven** — collecting tokens based on actual card costs rather than randomly is fundamentally correct.
- **Gold token protection** — never voluntarily discards gold, which is the right call.
- **Respects game rules** — take-2 requires 4+ tokens on board, take-3 requires 3 distinct colors; the bot checks these correctly.

### What the bot does poorly

- **Ignores discount value of cards.** The single biggest factor in mid-game Splendor strategy is building a discount engine — buying cheap cards to get permanent bonuses that make expensive cards free. The bot never evaluates a card's bonus gem color, so it may spend 3 turns saving up for a 1-point card when a 0-point card with a needed bonus would unlock several expensive cards for free.
- **No opponent awareness.** The bot does not track opponent scores, token counts, or which cards they are close to buying. It will not block a winning opponent by reserving their target card.
- **No endgame urgency.** Late in the game (when someone is near 15 points), the bot should tighten its strategy to prioritize high-point cards even at the cost of efficiency. It does not do this.
- **Blind reservations are weak.** Reserving from the lowest deck for a gold coin is a poor use of a turn in almost all situations.
- **No plan cohesion.** Each turn is evaluated in isolation. The bot cannot plan "I will buy card A which gives me a discount that lets me buy card B next turn."

---

## Overall Verdict

The AI is **adequate for a beginner-level opponent** and will not make outright illegal moves or obviously self-destructive decisions. Its gem-taking and purchase-scoring logic is grounded in real Splendor strategy. However, it misses two of the most important strategic dimensions of the game — **discount engine building** and **opponent-reactive play** — which means an experienced human player will reliably defeat it.

### Suggested Improvements (in order of impact)

1. **Add bonus-value scoring to card purchase.** Factor in how much the card's gem bonus reduces the cost of future target cards.
2. **Fix the dead-code branch** in `tryTakeGems` (lines 167–169).
3. **Fix `tryBuyReserved`** to score and pick the best affordable reserved card, not just the first.
4. **Blind reservation logic:** Skip deck draws entirely, or only draw from the highest available deck level.
5. **Unify need-calculation methods** — `addCardNeed` and `getTop3NeededColors` use different formulas; consolidate them.
6. **Add threat detection:** If any opponent is within 1–2 cards of winning, the bot should shift to blocking reservation and maximum-point purchasing.
