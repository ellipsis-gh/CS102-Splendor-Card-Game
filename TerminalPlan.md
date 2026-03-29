# Terminal UI Redesign Plan

## Goal

Replace the current `Scanner`-based input loop in `Main.java` and the plain-text rendering in `ConsoleUI.java` with a fully interactive terminal UI that:

- Always shows the full board state on screen at all times
- Clears and redraws the screen at the start of every new turn
- All input is done with arrow keys + Enter — no typing commands
- Contextual view commands available at any time (reserved cards, nobles, market, help)
- ANSI colour coding and box-drawing characters make the board readable and attractive

---

## Library: Lanterna

Use **[Lanterna](https://github.com/mabe02/lanterna)** (pure-Java terminal UI library, one JAR).

- Provides: raw key input (arrows, Enter, Esc), ANSI colour, screen buffering, cursor control
- Cross-platform: Windows Terminal, macOS Terminal, Linux — no `stty` hacks needed
- Add to project: download `lanterna-3.x.x.jar` and put it on the classpath, or add via Maven/Gradle

---

## Screen Layout (every turn)

The screen is split into two zones that are always visible:

- **Top zone** — the static board display, redrawn fresh at the start of each turn
- **Bottom zone** — the interactive prompt / menu area, updated as the player navigates

```
╔══════════════════════════════════════════════════════════════╗
║                      S P L E N D O R                         ║
║                  Player 1's Turn  •  Score: 4                ║
╠══════════════╦═══════════════════════════════════════════════╣
║  BANK GEMS   ║  NOBLES                                       ║
║  ● GRN:  4   ║  [0]  3pts  WHT×3  BLU×3                     ║
║  ● WHT:  3   ║  [1]  3pts  BLK×4                            ║
║  ● BLU:  2   ║  [2]  3pts  GRN×3  RED×2                     ║
║  ● BLK:  4   ║                                               ║
║  ● RED:  3   ║                                               ║
║  ★ GLD:  5   ║                                               ║
╠══════════════╩═══════════════════════════════════════════════╣
║  LEVEL 3  [deck: 12 remaining]                               ║
║  [3-0]  ★★★  +BLK  BLK×7                                    ║
║  [3-1]  ★★★  +WHT  GRN×3  WHT×5  BLU×3                     ║
║  [3-2]  ★★   +RED  RED×6  BLK×3                             ║
║  [3-3]  ★    +GRN  BLU×7                                    ║
╠══════════════════════════════════════════════════════════════╣
║  LEVEL 2  [deck: 20 remaining]                               ║
║  [2-0]  ★★   +BLU  GRN×2  RED×1  BLK×4                     ║
║  [2-1]  ★    +RED  WHT×3  BLU×2  BLK×2                     ║
║  [2-2]  (empty)                                              ║
║  [2-3]  ★    +GRN  RED×5                                    ║
╠══════════════════════════════════════════════════════════════╣
║  LEVEL 1  [deck: 30 remaining]                               ║
║  [1-0]       +BLK  GRN×1  BLU×2  RED×1                     ║
║  [1-1]       +WHT  BLK×2  RED×1                             ║
║  [1-2]       +BLU  GRN×3                                    ║
║  [1-3]       +RED  WHT×1  BLU×1  GRN×1  BLK×1              ║
╠══════════════════════════════════════════════════════════════╣
║  YOUR STATUS                                                  ║
║  Tokens:   GRN:2  WHT:1  BLU:0  BLK:3  RED:1  GLD:1  (8/10)║
║  Bonuses:  GRN:1  WHT:0  BLU:2  BLK:0  RED:0               ║
║  Reserved: 1 card  •  Nobles earned: 0                       ║
╠══════════════════════════════════════════════════════════════╣
║  OPPONENTS                                                    ║
║  AI  •  Score: 3  •  Tokens: 5  •  Bonuses: BLK:2  •  Res:1 ║
╠══════════════════════════════════════════════════════════════╣
║  [↑↓] Navigate   [Enter] Select   [?] Help   [V] View        ║
╚══════════════════════════════════════════════════════════════╝

  What would you like to do?

  > Take 3 different gems
    Take 2 of the same gem
    Buy a card
    Reserve a card
```

---

## Colour Scheme (ANSI)

| Element                        | Style                                      | ANSI code            |
|--------------------------------|--------------------------------------------|----------------------|
| GREEN gem / token / bonus      | Bright Green text                          | `\u001B[32m`         |
| WHITE gem / token / bonus      | Bright White text                          | `\u001B[97m`         |
| BLUE gem / token / bonus       | Bright Blue text                           | `\u001B[34m`         |
| BLACK gem / token / bonus      | Dark Grey text                             | `\u001B[90m`         |
| RED gem / token / bonus        | Bright Red text                            | `\u001B[31m`         |
| GOLD gem / token               | Bright Yellow text                         | `\u001B[33m`         |
| Prestige points ★              | Yellow                                     | `\u001B[33m`         |
| Section headers                | Bold                                       | `\u001B[1m`          |
| Current player name            | Bold Cyan                                  | `\u001B[1;36m`       |
| **Selected menu item**         | Inverted (black text on white background)  | `\u001B[7m`          |
| Unaffordable / unavailable     | Dim dark grey                              | `\u001B[2;90m`       |
| Affordable card (highlighted)  | Bold white                                 | `\u001B[1;97m`       |
| Warning / error message        | Bright Red                                 | `\u001B[31m`         |
| Info / notification            | Bright Cyan                                | `\u001B[36m`         |
| Reset all                      | —                                          | `\u001B[0m`          |

---

## Arrow-Key Menu System

This is the core interaction model — identical in feel to the Claude Code terminal prompt.

### How it works

Every prompt in the game is rendered as a vertical list of options. The player never types anything — all navigation is with keys:

| Key           | Action                                      |
|---------------|---------------------------------------------|
| `↑` / `↓`    | Move the highlight cursor up or down        |
| `Enter`       | Confirm the currently highlighted option    |
| `Esc`         | Go back one step (cancel current sub-menu)  |
| `?`           | Open the Help overlay (from anywhere)       |
| `V`           | Open the View menu (from anywhere)          |

### What the cursor looks like

The selected row is rendered with **inverted colours** (white background, black text) and a `>` marker. Unselected rows have no background. This is exactly how Claude Code renders its `Yes / No / Allow` prompts.

```
  What would you like to do?

  > Take 3 different gems          <- selected: inverted colours, > marker
    Take 2 of the same gem         <- unselected: normal text
    Buy a card
    Reserve a card
```

When the player presses `↓`:

```
  What would you like to do?

    Take 3 different gems
  > Take 2 of the same gem         <- cursor moved down
    Buy a card
    Reserve a card
```

Unavailable actions (e.g. "Take 2 same" when no colour has 4+ tokens) are shown in dim grey and are **skipped** when the cursor moves over them — the cursor jumps past them automatically, just like disabled options in Claude Code.

```
  What would you like to do?

  > Take 3 different gems
    Take 2 of the same gem  (no colour has 4+ available)    <- dim, skipped
    Buy a card
    Reserve a card
```

### Confirmation prompts (Yes / No style)

For any destructive or irreversible action (e.g. reserving a blind deck card), a confirmation is shown as a two-option horizontal prompt — exactly like Claude Code's `Yes / No`:

```
  Reserve the top card from Deck 2? You won't see it until after.

  > Yes    No
```

Left/Right arrows or Tab move between `Yes` and `No`. Enter confirms.

---

## Multi-Step Action Flows

### Step 1 — Action type (shown every turn)

```
  What would you like to do?

  > Take 3 different gems
    Take 2 of the same gem
    Buy a card
    Reserve a card
```

### Step 2a — Take 3 different gems

Pick three colours one at a time. Already-chosen colours are shown in dim grey and are skipped by the cursor automatically.

```
  Pick your 1st gem:

  > ● GREEN   (4 available)
    ● WHITE   (3 available)
    ● BLUE    (2 available)
    ● BLACK   (4 available)
    ● RED     (3 available)
```

After picking GREEN:

```
  Pick your 2nd gem:

    ● GREEN   (chosen)          <- dim, cursor skips this
  > ● WHITE   (3 available)
    ● BLUE    (2 available)
    ● BLACK   (4 available)
    ● RED     (3 available)
```

After picking WHITE, only one colour remains — auto-confirm or show final pick. Then show a confirmation summary:

```
  Confirm: Take 1 ● GREEN, 1 ● WHITE, 1 ● BLUE?

  > Yes    No
```

### Step 2b — Take 2 of the same gem

Only colours with 4+ tokens on the board appear. If only one qualifies, it is auto-selected and goes straight to confirm.

```
  Pick a gem colour (need 4+ on board):

  > ● BLACK   (4 available)
    ● GREEN   (4 available)
```

### Step 2c — Buy a card

All buyable cards are listed: market cards (levels 1–3) and the player's reserved cards. Affordable ones are shown normally; unaffordable ones are dim grey but the cursor still visits them (so the player can see what they are missing).

```
  Which card would you like to buy?

  > [1-0]       +● BLK   GRN×1  BLU×2  RED×1      (can afford)
    [1-1]       +● WHT   BLK×2  RED×1              (can afford)
    [2-0]  ★★   +● BLU   GRN×2  RED×1  BLK×4      (need 1 more BLK)   <- dim
    [2-1]  ★    +● RED   WHT×3  BLU×2  BLK×2      (need 2 more WHT)   <- dim
    [r-0]  ★    +● BLU   RED×2  BLK×1              (can afford)
```

If the player tries to confirm an unaffordable card, the cursor stays put and an inline error appears below the list in red:

```
  You cannot afford [2-0]. You need 1 more BLK gem.
```

### Step 2d — Reserve a card

Both visible market cards and blind deck draws are listed together.

```
  Which card would you like to reserve?  (You have 1/3 reserved)

  > [1-0]       +● BLK   GRN×1  BLU×2  RED×1
    [1-1]       +● WHT   BLK×2  RED×1
    [2-0]  ★★   +● BLU   GRN×2  RED×1  BLK×4
    ── Blind draw from deck ──
    [deck 1]  Level 1 top card  (30 remaining)
    [deck 2]  Level 2 top card  (20 remaining)
    [deck 3]  Level 3 top card  (12 remaining)
```

Selecting a deck entry shows the Yes/No confirmation prompt described above. Selecting a visible card goes straight through.

### Return tokens (when over 10)

Shown as a sub-step after any action that causes the player to exceed 10 tokens. Colours the player doesn't hold are dim and skipped.

```
  You have 11 tokens. You must return 1.

    ● GREEN   (you have 0)    <- dim, skipped
  > ● WHITE   (you have 1)
    ● BLUE    (you have 0)    <- dim, skipped
    ● BLACK   (you have 3)
    ● RED     (you have 1)
    ★ GOLD    (you have 1)
```

---

## View Commands (available at any time)

Pressing `V` at any point during a turn opens the **View Menu** as an overlay. The board behind it does not change.

```
  View...

  > My Reserved Cards
    All Nobles
    Full Market
    Opponent Details
    Back
```

### View: My Reserved Cards

Shows the player's full reserved hand with complete card details. Pressing any key closes the overlay.

```
  ┌─ Your Reserved Cards (1/3) ─────────────────────────────┐
  │                                                           │
  │  [r-0]  ★  +● BLUE                                       │
  │      Cost:  RED×2  BLK×1                                 │
  │      You need:  RED×0  BLK×0  (can afford now)           │
  │                                                           │
  │  [r-1]  ★★  +● GREEN                                     │
  │      Cost:  WHT×2  BLU×3  RED×1                          │
  │      You need:  WHT×1  BLU×1  (need 2 more gems)         │
  │                                                           │
  │  [Press any key to close]                                 │
  └───────────────────────────────────────────────────────────┘
```

Crucially, the "You need" line shows exactly what is still missing after applying the player's current bonuses and tokens — not the raw card cost.

### View: All Nobles

Shows each noble's full requirements and how close the player is to earning each one.

```
  ┌─ Nobles ────────────────────────────────────────────────┐
  │                                                          │
  │  [0]  3 pts  —  needs WHT×3  BLU×3                      │
  │      Your bonuses:  WHT:0  BLU:2  →  need WHT×3  BLU×1  │
  │                                                          │
  │  [1]  3 pts  —  needs BLK×4                             │
  │      Your bonuses:  BLK:0  →  need BLK×4                │
  │                                                          │
  │  [2]  3 pts  —  needs GRN×3  RED×2                      │
  │      Your bonuses:  GRN:1  RED:0  →  need GRN×2  RED×2  │
  │                                                          │
  │  [Press any key to close]                                │
  └──────────────────────────────────────────────────────────┘
```

### View: Full Market

Shows all 12 visible market cards with complete cost details laid out in a grid, grouped by level. Useful when the main board is too compact to read card costs clearly.

```
  ┌─ Market ─────────────────────────────────────────────────┐
  │                                                           │
  │  LEVEL 3  (deck: 12 remaining)                           │
  │  [3-0]  ★★★  +● BLK   Cost: BLK×7                       │
  │  [3-1]  ★★★  +● WHT   Cost: GRN×3  WHT×5  BLU×3        │
  │  [3-2]  ★★   +● RED   Cost: RED×6  BLK×3                │
  │  [3-3]  ★    +● GRN   Cost: BLU×7                       │
  │                                                           │
  │  LEVEL 2  (deck: 20 remaining)                           │
  │  [2-0]  ★★   +● BLU   Cost: GRN×2  RED×1  BLK×4        │
  │  [2-1]  ★    +● RED   Cost: WHT×3  BLU×2  BLK×2        │
  │  [2-2]  (empty slot)                                     │
  │  [2-3]  ★    +● GRN   Cost: RED×5                       │
  │                                                           │
  │  LEVEL 1  (deck: 30 remaining)                           │
  │  [1-0]       +● BLK   Cost: GRN×1  BLU×2  RED×1        │
  │  [1-1]       +● WHT   Cost: BLK×2  RED×1                │
  │  [1-2]       +● BLU   Cost: GRN×3                       │
  │  [1-3]       +● RED   Cost: WHT×1  BLU×1  GRN×1  BLK×1 │
  │                                                           │
  │  [Press any key to close]                                 │
  └───────────────────────────────────────────────────────────┘
```

### View: Opponent Details

Shows a full breakdown of each opponent's tokens, bonuses, purchased card count, and reserved card count (not the cards themselves — those are secret).

```
  ┌─ Opponent Details ───────────────────────────────────────┐
  │                                                           │
  │  AI  •  Score: 3 pts  •  10 tokens held                  │
  │      Tokens:   GRN:0  WHT:2  BLU:1  BLK:4  RED:2  GLD:1 │
  │      Bonuses:  GRN:0  WHT:0  BLU:0  BLK:2  RED:0        │
  │      Purchased: 3 cards   Reserved: 1 card (hidden)      │
  │      Nobles earned: 0                                     │
  │                                                           │
  │  [Press any key to close]                                 │
  └───────────────────────────────────────────────────────────┘
```

---

## Help Overlay (`?` key)

Pressing `?` at any point opens a full-screen help overlay explaining all controls and game rules. It does not interrupt the current action — pressing any key closes it and returns to exactly where the player was.

```
  ┌─ Help ───────────────────────────────────────────────────┐
  │                                                           │
  │  CONTROLS                                                 │
  │  ↑ / ↓      Move cursor up / down                        │
  │  Enter      Confirm selected option                       │
  │  Esc        Go back / cancel                             │
  │  V          Open view menu (reserved, nobles, market...)  │
  │  ?          Show this help screen                         │
  │                                                           │
  │  GAME RULES                                               │
  │  Goal        Reach 15 prestige points first               │
  │  Gems        Take 3 different OR 2 of the same (need 4+) │
  │  Buy         Pay gem cost; bonuses reduce cost            │
  │  Reserve     Take any card; get 1 GOLD token              │
  │  Nobles      Auto-awarded when you have enough bonuses    │
  │  Token limit Max 10 tokens — return extras after a move   │
  │                                                           │
  │  SYMBOLS                                                  │
  │  ★           1 prestige point                             │
  │  ●           Gem token (coloured by type)                 │
  │  +● BLK      Card gives 1 permanent BLACK bonus           │
  │  [r-0]       Your reserved card at slot 0                 │
  │  [deck 2]    Blind draw from Level 2 deck                 │
  │                                                           │
  │  [Press any key to close]                                 │
  └───────────────────────────────────────────────────────────┘
```

---

## Files to Create / Modify

### New files

| File | Purpose |
|------|---------|
| `src/model/TerminalRenderer.java` | Renders the full static board (top zone) using ANSI codes and box-drawing chars. Stateless — call `render(board, players, currentIndex)` to redraw the whole screen. |
| `src/model/AnsiMenu.java` | The core menu engine. Takes a `List<MenuItem>` (label + enabled flag), draws the list with the cursor, reads raw keypresses from Lanterna, returns the chosen index. Handles cursor skipping over disabled items. |
| `src/model/AnsiOverlay.java` | Renders modal overlays (bordered box, title, multi-line content). Used by all View panels and the Help screen. Takes a `List<String>` of content lines, renders the box, blocks until any key is pressed. |
| `src/model/AnsiColors.java` | All ANSI escape code constants: colours, bold, dim, reset, invert, clear screen. No logic — pure constants. |
| `src/model/TurnController.java` | Orchestrates a single human turn. Calls `TerminalRenderer.render()` at the start, then drives the multi-step action flow using `AnsiMenu`, handles the return-tokens sub-step, and calls the appropriate `Game` methods. |

### Modified files

| File | Change |
|------|--------|
| `src/model/Main.java` | Remove `printBoard`, `printPlayerStatus`, all `do*` action methods. Replace with `TerminalRenderer.render()` + `TurnController.doHumanTurn()`. Keep setup, game loop, and AI turn handling. |
| `src/model/ConsoleUI.java` | Delete — fully superseded by `TerminalRenderer`. |

---

## Implementation Order

1. **`AnsiColors.java`** — constants only, no dependencies
2. **`TerminalRenderer.java`** — render the static board; verify it looks correct in terminal
3. **`AnsiOverlay.java`** — build and test the overlay box renderer standalone
4. **`AnsiMenu.java`** — arrow-key list navigation; test with a hardcoded dummy list
5. **`TurnController.java`** — wire action flows using renderer + menu + overlay
6. **`Main.java`** — swap out old print/scanner code
7. Delete `ConsoleUI.java`

---

## Key Implementation Notes

### Screen clear at turn start

```java
// AnsiColors.java
public static final String CLEAR      = "\u001B[2J";   // erase entire screen
public static final String CURSOR_HOME = "\u001B[H";   // move cursor to top-left

// TerminalRenderer.render() — first two lines
System.out.print(AnsiColors.CLEAR);
System.out.print(AnsiColors.CURSOR_HOME);
```

### MenuItem model for the menu engine

```java
// AnsiMenu.java — inner record
record MenuItem(String label, boolean enabled) {}
```

`enabled = false` items are rendered in `AnsiColors.DIM` and the cursor skips over them automatically when the player presses `↑` or `↓`. This mirrors the Claude Code `Yes / No / Allow` behaviour exactly.

### Reading arrow keys with Lanterna

```java
KeyStroke key = terminal.readInput(); // blocks until a key is pressed
switch (key.getKeyType()) {
    case ArrowUp    -> moveCursorUp();
    case ArrowDown  -> moveCursorDown();
    case Enter      -> confirmSelection();
    case Escape     -> goBack();
    case Character  -> handleCharKey(key.getCharacter()); // '?' and 'v'
}
```

### Cursor redraw strategy (no flicker)

Do not clear the whole screen when the cursor moves within a menu — only redraw the menu lines. Save the terminal row where the menu starts, then overwrite only those lines using cursor-positioning escape codes. The board above stays untouched.

```java
// Move cursor to specific row and rewrite just that line
System.out.printf("\u001B[%d;1H", menuStartRow + i); // move to row, col 1
System.out.print(renderMenuRow(i, i == selectedIndex));
```

### Affordability annotation in buy menu

Before rendering the buy-card list, call `player.canAffordCard(card)` for each card. For unaffordable cards, also compute and display the shortfall per colour so the player knows exactly what they are missing — this is the "need 1 more BLK" inline annotation.

### Token symbols

```
●  (U+25CF)  coloured gem circle
★  (U+2605)  yellow prestige star
─  (U+2500)  horizontal box line (overlay separators)
│  (U+2502)  vertical box line
┌ ┐ └ ┘  (U+250C U+2510 U+2514 U+2518)  overlay corners
```

---

## Out of Scope for This Plan

- Network / multiplayer UI (handled separately in `src/network/`)
- AI turn visualisation beyond the current printed log line
- Mouse support
- Saving / loading game state
