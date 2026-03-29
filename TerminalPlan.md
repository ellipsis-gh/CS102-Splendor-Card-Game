# Terminal UI Redesign Plan

## Goal

Rewrite `ConsoleUI.java` into a fully interactive terminal UI. No new files are created. `Main.java` is updated only where needed to call the new `ConsoleUI` methods instead of its own inline print/Scanner logic.

The result must be:

- Always showing the full board state on screen
- Clearing and redrawing the screen at the start of every new turn
- Arrow-key navigation for all input — no typed commands
- Contextual view overlays (reserved cards, nobles, market, opponent details)
- A help overlay accessible with `?` at any time
- ANSI colour-coded and styled with box-drawing characters
- Highest code quality: clear Javadoc on every public method, inline comments on non-obvious logic, named constants for every magic value, no duplicated rendering code

---

## Files Changed

| File | What changes |
|------|-------------|
| `src/model/ConsoleUI.java` | Complete rewrite — becomes the single class responsible for all rendering and all human input. Contains ANSI constants, menu engine, overlay renderer, and all view/help screens as private methods. |
| `src/model/Main.java` | Remove inline `printBoard`, `printPlayerStatus`, all `do*` action methods, and direct `Scanner` calls. Replace with calls to the new `ConsoleUI` public methods. Game loop structure stays the same. |

No other files are touched. No new files are created.

---

## ConsoleUI — Internal Structure

Everything lives inside one class. Private sections are separated by block comments for readability.

```
ConsoleUI.java
│
├── // ── ANSI CONSTANTS ──────────────────────────────────────
│   private static final String RESET, BOLD, DIM, INVERT, CLEAR ...
│   private static final String FG_GREEN, FG_WHITE, FG_BLUE ...
│
├── // ── PUBLIC API ───────────────────────────────────────────
│   + render(Board, List<Player>, int currentIndex)
│       Clears screen, draws full board + both player panels.
│       Called at the start of every turn.
│
│   + doHumanTurn(Game, Player) : boolean
│       Drives the full multi-step turn: action menu → sub-menu
│       → confirm → execute game move → return-tokens if needed.
│       Returns true when a valid move has been made.
│
│   + showNobleVisit(Noble)
│       Prints a one-line notification when a noble is awarded.
│
│   + showWinner(Player)
│       Prints the end-of-game banner.
│
├── // ── MENU ENGINE ──────────────────────────────────────────
│   - showMenu(String prompt, List<String> options, boolean[] enabled) : int
│       Core arrow-key menu. Renders the option list with an
│       inverted-colour highlight on the selected row. UP/DOWN
│       move the cursor (skipping disabled rows automatically).
│       ENTER returns the chosen index. ESC returns -1 (go back).
│       '?' opens the help overlay. 'v'/'V' opens the view menu.
│
│   - showConfirm(String question) : boolean
│       Renders a horizontal Yes / No prompt. LEFT/RIGHT or
│       TAB switch between them. ENTER confirms. ESC cancels.
│
├── // ── VIEW OVERLAYS ────────────────────────────────────────
│   - showViewMenu(Board, Player, List<Player>)
│       Opens the View sub-menu and routes to the chosen overlay.
│
│   - showReservedCards(Player)
│       Overlay: player's reserved hand with full cost details
│       and a "still need X" line computed from current bonuses.
│
│   - showNobles(Board, Player)
│       Overlay: all nobles with per-noble progress for the player.
│
│   - showFullMarket(Board)
│       Overlay: all 12 visible card slots with complete costs.
│
│   - showOpponentDetails(List<Player>, int currentIndex)
│       Overlay: tokens, bonuses, card counts for all opponents.
│
├── // ── HELP OVERLAY ─────────────────────────────────────────
│   - showHelp()
│       Full-screen overlay: controls, game rules, symbol guide.
│
├── // ── ACTION SUB-MENUS ─────────────────────────────────────
│   - doTakeThreeGems(Game, Player) : boolean
│   - doTakeTwoSameGems(Game, Player) : boolean
│   - doBuyCard(Game, Player) : boolean
│   - doReserveCard(Game, Player) : boolean
│   - doReturnTokens(Game, Player)
│
├── // ── BOARD RENDERING ──────────────────────────────────────
│   - renderHeader(Player)
│   - renderBankAndNobles(Board)
│   - renderMarketRow(Board, int level)
│   - renderPlayerPanel(Player)
│   - renderOpponentSummary(List<Player>, int currentIndex)
│   - renderKeyBar()          ← bottom hint line: [↑↓] Navigate ...
│
└── // ── RENDERING UTILITIES ──────────────────────────────────
    - colorForToken(Token) : String
    - gemDot(Token) : String         ← coloured ● symbol
    - stars(int n) : String          ← yellow ★ repeated n times
    - formatCost(Map<Token,Integer>) : String
    - formatTokenMap(Map<Token,Integer>) : String
    - drawBox(List<String> lines)    ← wraps content in ┌─┐ box
    - readKey() : int[]              ← raw keypress reader
    - printColoured(String text, String ansiCode)
```

---

## Public API — Called from Main.java

`Main.java` creates one `ConsoleUI` instance and uses only these methods:

```java
ConsoleUI ui = new ConsoleUI();

// start of each human turn
ui.render(game.getBoard(), game.getPlayers(), game.getCurrentPlayerIndex());

// get and execute the human player's move
boolean moved = ui.doHumanTurn(game, player);

// after a noble is awarded
ui.showNobleVisit(noble);

// at game end
ui.showWinner(game.determineWinner());
```

Everything else — arrow-key input, menus, overlays, sub-menus — is handled internally.

---

## Screen Layout (every turn)

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
║  LEVEL 3  [deck: 12]                                         ║
║  [3-0]  ★★★  +● BLK   BLK×7                                 ║
║  [3-1]  ★★★  +● WHT   GRN×3  WHT×5  BLU×3                  ║
║  [3-2]  ★★   +● RED   RED×6  BLK×3                          ║
║  [3-3]  ★    +● GRN   BLU×7                                 ║
╠══════════════════════════════════════════════════════════════╣
║  LEVEL 2  [deck: 20]                                         ║
║  [2-0]  ★★   +● BLU   GRN×2  RED×1  BLK×4                  ║
║  [2-1]  ★    +● RED   WHT×3  BLU×2  BLK×2                  ║
║  [2-2]  (empty)                                              ║
║  [2-3]  ★    +● GRN   RED×5                                 ║
╠══════════════════════════════════════════════════════════════╣
║  LEVEL 1  [deck: 30]                                         ║
║  [1-0]       +● BLK   GRN×1  BLU×2  RED×1                  ║
║  [1-1]       +● WHT   BLK×2  RED×1                          ║
║  [1-2]       +● BLU   GRN×3                                 ║
║  [1-3]       +● RED   WHT×1  BLU×1  GRN×1  BLK×1           ║
╠══════════════════════════════════════════════════════════════╣
║  YOUR STATUS                                                  ║
║  Tokens:  GRN:2  WHT:1  BLU:0  BLK:3  RED:1  GLD:1  (8/10) ║
║  Bonuses: GRN:1  WHT:0  BLU:2  BLK:0  RED:0                ║
║  Reserved: 1 card  •  Nobles earned: 0                       ║
╠══════════════════════════════════════════════════════════════╣
║  OPPONENTS                                                    ║
║  AI  •  Score:3  •  Tokens:5  •  Bonuses: BLK:2  •  Res:1   ║
╠══════════════════════════════════════════════════════════════╣
║  [↑↓] Navigate   [Enter] Select   [?] Help   [V] View        ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Colour Scheme (ANSI)

All codes are `private static final String` constants at the top of `ConsoleUI`.

| Element                       | Style                                     | ANSI code        |
|-------------------------------|-------------------------------------------|------------------|
| GREEN gem / token / bonus     | Bright Green                              | `\u001B[32m`     |
| WHITE gem / token / bonus     | Bright White                              | `\u001B[97m`     |
| BLUE gem / token / bonus      | Bright Blue                               | `\u001B[34m`     |
| BLACK gem / token / bonus     | Dark Grey                                 | `\u001B[90m`     |
| RED gem / token / bonus       | Bright Red                                | `\u001B[31m`     |
| GOLD gem / token              | Bright Yellow                             | `\u001B[33m`     |
| Prestige points ★             | Yellow                                    | `\u001B[33m`     |
| Section headers               | Bold                                      | `\u001B[1m`      |
| Current player name           | Bold Cyan                                 | `\u001B[1;36m`   |
| **Selected menu row**         | Inverted (black on white)                 | `\u001B[7m`      |
| Disabled / unaffordable row   | Dim dark grey                             | `\u001B[2;90m`   |
| Warning / error message       | Bright Red                                | `\u001B[31m`     |
| Notification (noble, etc.)    | Bright Cyan                               | `\u001B[36m`     |
| Reset all                     | —                                         | `\u001B[0m`      |

---

## Arrow-Key Menu System

### How the cursor works

Every prompt is a vertical list. The selected row is shown with **inverted colours** and a `>` prefix. Unselected rows have a two-space indent. This matches the Claude Code `Yes / No / Allow` style exactly.

```
  What would you like to do?

  > Take 3 different gems          ← selected: inverted + >
    Take 2 of the same gem         ← unselected: normal text
    Buy a card
    Reserve a card
```

Pressing `↓`:

```
  What would you like to do?

    Take 3 different gems
  > Take 2 of the same gem         ← cursor moved
    Buy a card
    Reserve a card
```

Disabled rows (invalid moves) are dim grey. The cursor **skips over them** automatically — pressing `↓` when the cursor is above a disabled row jumps past it to the next enabled row.

```
  What would you like to do?

  > Take 3 different gems
    Take 2 of the same gem  (no colour has 4+)   ← dim, skipped
    Buy a card
    Reserve a card
```

### Yes / No confirmation prompt

Used before any irreversible action (blind deck reserve). Rendered as a horizontal pair, not a list. `←` / `→` or Tab switch focus. Enter confirms.

```
  Reserve the top card from Deck 2?

  > Yes    No
```

After pressing `→`:

```
  Reserve the top card from Deck 2?

    Yes  > No
```

### Key bindings

| Key        | Action                                         |
|------------|------------------------------------------------|
| `↑` / `↓` | Move cursor (skips disabled rows)              |
| `Enter`    | Confirm selection                              |
| `Esc`      | Cancel / go back one step                     |
| `←` / `→` | Switch focus in Yes/No prompt                 |
| `?`        | Open help overlay (from anywhere in a turn)   |
| `V`        | Open view menu (from anywhere in a turn)      |

---

## Multi-Step Action Flows

### Step 1 — Action type

```
  What would you like to do?

  > Take 3 different gems
    Take 2 of the same gem
    Buy a card
    Reserve a card
```

### Step 2a — Take 3 different gems

Three sequential colour picks. Already-chosen colours and zero-stock colours are dim and skipped.

```
  Pick your 1st gem:              |  Pick your 2nd gem:
                                  |
  > ● GREEN   (4 available)       |    ● GREEN  (chosen)    ← dim, skipped
    ● WHITE   (3 available)       |  > ● WHITE  (3 available)
    ● BLUE    (2 available)       |    ● BLUE   (2 available)
    ● BLACK   (4 available)       |    ● BLACK  (4 available)
    ● RED     (3 available)       |    ● RED    (3 available)
```

After all three are chosen, a confirm prompt appears:

```
  Confirm: take 1 ● GREEN, 1 ● WHITE, 1 ● BLUE?

  > Yes    No
```

### Step 2b — Take 2 of the same gem

Only colours with 4+ tokens on the board are listed. If exactly one qualifies, it goes straight to confirm.

```
  Pick a gem colour (need 4+ on board):

  > ● BLACK   (4 available)
    ● GREEN   (4 available)
```

### Step 2c — Buy a card

All market cards (levels 1–3) and reserved cards listed together. Affordable cards are full brightness. Unaffordable cards are dim with an inline shortfall note. Cursor visits all rows but pressing Enter on an unaffordable one shows an error and stays put.

```
  Which card would you like to buy?

  > [1-0]       +● BLK   GRN×1  BLU×2  RED×1      can afford
    [1-1]       +● WHT   BLK×2  RED×1              can afford
    [2-0]  ★★   +● BLU   GRN×2  RED×1  BLK×4      need 1 more BLK   ← dim
    [r-0]  ★    +● BLU   RED×2  BLK×1              can afford
```

If the player tries to confirm a dim card:

```
  You cannot afford [2-0] — you still need 1 BLK gem.
```

### Step 2d — Reserve a card

Visible market cards and blind deck draws in one list. Blind entries include remaining deck count.

```
  Which card would you like to reserve?  (1 / 3 slots used)

  > [1-0]       +● BLK   GRN×1  BLU×2  RED×1
    [1-1]       +● WHT   BLK×2  RED×1
    [2-0]  ★★   +● BLU   GRN×2  RED×1  BLK×4
    ── blind draw ──────────────────────────
    [deck 1]    Level 1 top card  (30 remaining)
    [deck 2]    Level 2 top card  (20 remaining)
    [deck 3]    Level 3 top card  (12 remaining)
```

Blind draws show the Yes / No confirm. Visible cards confirm immediately.

### Return tokens (when over 10)

Shown automatically after any move that puts the player over 10. Colours with zero tokens are dim and skipped.

```
  You have 11 tokens. You must return 1.

    ● GREEN   (you have 0)   ← dim, skipped
  > ● WHITE   (you have 1)
    ● BLUE    (you have 0)   ← dim, skipped
    ● BLACK   (you have 3)
    ● RED     (you have 1)
    ★ GOLD    (you have 1)
```

---

## View Overlays (`V` key)

```
  View...

  > My Reserved Cards
    All Nobles
    Full Market
    Opponent Details
    Back
```

All overlays are rendered by `drawBox()` — one private method that wraps any list of strings in a `┌─┐` border. Pressing any key closes the overlay and returns to the same menu step.

### My Reserved Cards

```
  ┌─ Your Reserved Cards (1 / 3) ───────────────────────────┐
  │                                                           │
  │  [r-0]  ★  +● BLUE                                       │
  │      Cost:   RED×2  BLK×1                                │
  │      Still need:  nothing — can afford now               │
  │                                                           │
  │  [r-1]  ★★  +● GREEN                                     │
  │      Cost:   WHT×2  BLU×3  RED×1                         │
  │      Still need:  WHT×1  BLU×1                           │
  │                                                           │
  │  [Any key to close]                                       │
  └───────────────────────────────────────────────────────────┘
```

"Still need" is computed from the card cost minus the player's current bonuses and tokens — not the raw cost.

### All Nobles

```
  ┌─ Nobles ─────────────────────────────────────────────────┐
  │                                                           │
  │  [0]  3 pts  —  WHT×3  BLU×3                             │
  │      Progress:  WHT: 0/3   BLU: 2/3  →  need WHT×3      │
  │                                                           │
  │  [1]  3 pts  —  BLK×4                                    │
  │      Progress:  BLK: 0/4   →  need BLK×4                │
  │                                                           │
  │  [2]  3 pts  —  GRN×3  RED×2                             │
  │      Progress:  GRN: 1/3   RED: 0/2  →  need GRN×2 RED×2│
  │                                                           │
  │  [Any key to close]                                       │
  └───────────────────────────────────────────────────────────┘
```

Progress uses the player's purchased-card bonuses only (not tokens).

### Full Market

```
  ┌─ Full Market ────────────────────────────────────────────┐
  │                                                           │
  │  LEVEL 3  (deck: 12 remaining)                           │
  │  [3-0]  ★★★  +● BLK   BLK×7                             │
  │  [3-1]  ★★★  +● WHT   GRN×3  WHT×5  BLU×3              │
  │  [3-2]  ★★   +● RED   RED×6  BLK×3                      │
  │  [3-3]  ★    +● GRN   BLU×7                             │
  │                                                           │
  │  LEVEL 2  (deck: 20 remaining)                           │
  │  [2-0]  ★★   +● BLU   GRN×2  RED×1  BLK×4              │
  │  [2-1]  ★    +● RED   WHT×3  BLU×2  BLK×2              │
  │  [2-2]  (empty)                                          │
  │  [2-3]  ★    +● GRN   RED×5                             │
  │                                                           │
  │  LEVEL 1  (deck: 30 remaining)                           │
  │  [1-0]       +● BLK   GRN×1  BLU×2  RED×1              │
  │  [1-1]       +● WHT   BLK×2  RED×1                      │
  │  [1-2]       +● BLU   GRN×3                             │
  │  [1-3]       +● RED   WHT×1  BLU×1  GRN×1  BLK×1       │
  │                                                           │
  │  [Any key to close]                                       │
  └───────────────────────────────────────────────────────────┘
```

### Opponent Details

```
  ┌─ Opponent Details ───────────────────────────────────────┐
  │                                                           │
  │  AI  •  Score: 3 pts                                     │
  │      Tokens:    GRN:0  WHT:2  BLU:1  BLK:4  RED:2  GLD:1│
  │      Bonuses:   GRN:0  WHT:0  BLU:0  BLK:2  RED:0       │
  │      Purchased: 3 cards                                   │
  │      Reserved:  1 card (hidden)                          │
  │      Nobles:    0 earned                                  │
  │                                                           │
  │  [Any key to close]                                       │
  └───────────────────────────────────────────────────────────┘
```

---

## Help Overlay (`?` key)

```
  ┌─ Help ───────────────────────────────────────────────────┐
  │                                                           │
  │  CONTROLS                                                 │
  │  ↑ / ↓      Move cursor up and down                      │
  │  Enter      Confirm selected option                       │
  │  Esc        Go back / cancel current step                │
  │  ← / →      Switch between Yes and No                    │
  │  V          Open view menu                               │
  │  ?          Show this help screen                         │
  │                                                           │
  │  GAME RULES                                               │
  │  Goal        First to 15 prestige points wins             │
  │  Gems        Take 3 different OR 2 same (need 4+ on board)│
  │  Buy         Pay gem cost; your bonuses reduce cost first │
  │  Reserve     Take any card face-down; receive 1 GOLD      │
  │  Nobles      Auto-awarded when bonus total meets requirement│
  │  Token limit Max 10 — return extras immediately after move│
  │                                                           │
  │  SYMBOLS                                                  │
  │  ★           1 prestige point                             │
  │  ●           Gem token (colour-coded by type)             │
  │  +● BLK      This card gives 1 permanent BLACK discount   │
  │  [r-0]       Your reserved card at slot 0                 │
  │  [deck 2]    Blind draw from the Level 2 deck             │
  │                                                           │
  │  [Any key to close]                                       │
  └───────────────────────────────────────────────────────────┘
```

---

## Code Quality Requirements

These apply to every method written in `ConsoleUI.java`:

### Javadoc on every public method
```java
/**
 * Clears the terminal and renders the full game state for the start of a turn.
 * This is the only method that should call {@link #clearScreen()} directly.
 *
 * @param board         the current board state (tokens, cards, nobles)
 * @param players       all players in turn order
 * @param currentIndex  index into {@code players} of whose turn it is
 */
public void render(Board board, List<Player> players, int currentIndex) { ... }
```

### Inline comments on non-obvious logic
```java
// Arrow keys arrive as a 3-byte escape sequence: ESC, '[', then A/B/C/D.
// We read the first byte; if it is 27 (ESC) we read two more to identify the key.
// Any other first byte is treated as a regular character keypress.
int first = System.in.read();
```

### Named constants — no magic values
```java
private static final int    MAX_TOKENS       = 10;
private static final int    MAX_RESERVED     = 3;
private static final int    MIN_STACK_FOR_TWO = 4;
private static final String GEM_SYMBOL       = "\u25CF"; // ●
private static final String STAR_SYMBOL      = "\u2605"; // ★
```

### No duplicated rendering — one helper, used everywhere
```java
// Every coloured gem reference goes through this — never inline colour codes on token names
private String gemDot(Token token) {
    return colorForToken(token) + GEM_SYMBOL + RESET;
}
```

### Method length — keep methods focused
No method should do more than one logical thing. If a method is rendering and also computing affordability, split it.

---

## Raw Key Input (no library needed)

Arrow keys send a 3-byte escape sequence over stdin. `ConsoleUI` reads them directly using a private `readKey()` method. The terminal must be switched to raw mode so keypresses arrive immediately without waiting for Enter.

```java
/**
 * Reads a single keypress from stdin and returns an int code.
 * Regular characters return their ASCII value.
 * Arrow keys return one of the KEY_* constants below.
 * Blocks until a key is pressed.
 */
private int readKey() throws IOException { ... }

// Key code constants — used everywhere instead of raw numbers
private static final int KEY_UP    = 1000;
private static final int KEY_DOWN  = 1001;
private static final int KEY_LEFT  = 1002;
private static final int KEY_RIGHT = 1003;
private static final int KEY_ENTER = 13;
private static final int KEY_ESC   = 27;
```

**Raw mode on Windows (Windows Terminal):**
```java
// Switch to raw mode using a ProcessBuilder calling cmd.exe
// This is done once in the ConsoleUI constructor and restored on JVM shutdown
private void enableRawMode() {
    // Windows: enable ENABLE_VIRTUAL_TERMINAL_INPUT via a one-shot PowerShell call
    // or rely on Windows Terminal which handles ANSI natively without any changes
}
```

For cross-platform reliability, if raw mode cannot be enabled (e.g. inside an IDE), `ConsoleUI` falls back gracefully to the original numbered-menu `Scanner` input so the game still runs — raw mode is best-effort.

---

## Implementation Order

1. Write all ANSI constants and `colorForToken` / `gemDot` / `stars` helpers — nothing depends on these, and they are used everywhere
2. Write `drawBox` — needed by all overlays
3. Write `renderHeader`, `renderBankAndNobles`, `renderMarketRow`, `renderPlayerPanel`, `renderOpponentSummary`, `renderKeyBar`, then wire them into `render`
4. Write `readKey` and verify arrow keys work in the target terminal
5. Write `showMenu` and `showConfirm` — the full menu engine
6. Write `showHelp` and all four view overlays
7. Write the action sub-menus (`doTakeThreeGems`, `doTakeTwoSameGems`, `doBuyCard`, `doReserveCard`, `doReturnTokens`), then wire into `doHumanTurn`
8. Update `Main.java` — replace inline print/Scanner code with `ui.render()` and `ui.doHumanTurn()`

---

## Out of Scope

- Network / multiplayer UI (`src/network/` — untouched)
- AI turn visualisation (AI already prints its move via `Main.java` — no change)
- Mouse support
- Saving / loading game state
