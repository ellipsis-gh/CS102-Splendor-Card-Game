# Terminal UI Redesign Plan

## Goal

Replace the current `Scanner`-based input loop in `Main.java` and the plain-text rendering in `ConsoleUI.java` with a fully interactive terminal UI that:

- Always shows the full board state on screen
- Clears and redraws the screen at the start of every new turn
- Uses arrow-key navigation and Enter to select moves (no typing commands)
- Uses ANSI colour codes and box-drawing characters to make the board readable and attractive

---

## Library: Lanterna

Use **[Lanterna](https://github.com/mabe02/lanterna)** (Java terminal UI library, no external dependencies beyond a single JAR).

- Provides: raw key input (arrows, Enter, Esc), ANSI colour, screen buffering, cursor control
- Pure Java — works in any terminal that supports ANSI (Windows Terminal, macOS Terminal, Linux)
- Add to project: download `lanterna-3.x.x.jar` and add to the classpath, or add via Maven/Gradle

Alternative (no library): use raw ANSI escape codes printed via `System.out` directly — simpler to add but more manual work for key input. Lanterna is recommended.

---

## Screen Layout (every turn)

```
╔══════════════════════════════════════════════════════════════╗
║                        S P L E N D O R                       ║
║                    Player 1's Turn  •  Score: 4              ║
╠══════════════╦═══════════════════════════════════════════════╣
║  BANK GEMS   ║  NOBLES                                       ║
║  ●●  GRN: 4  ║  [0] 3pts  needs: WHT×3 BLU×3               ║
║  ●●  WHT: 3  ║  [1] 3pts  needs: BLK×4                      ║
║  ●●  BLU: 2  ║  [2] 3pts  needs: GRN×3 RED×2               ║
║  ●●  BLK: 4  ║                                               ║
║  ●●  RED: 3  ║                                               ║
║  ★★  GLD: 5  ║                                               ║
╠══════════════╩═══════════════════════════════════════════════╣
║  LEVEL 3  [deck: 12]                                         ║
║  [3-0] ★★★ +BLK  BLK×7                                      ║
║  [3-1] ★★★ +WHT  GRN×3 WHT×5 BLU×3                         ║
║  [3-2] ★★  +RED  RED×6 BLK×3                                ║
║  [3-3] ★    +GRN  BLU×7                                      ║
╠══════════════════════════════════════════════════════════════╣
║  LEVEL 2  [deck: 20]                                         ║
║  [2-0] ★★  +BLU  GRN×2 RED×1 BLK×4                         ║
║  [2-1] ★    +RED  WHT×3 BLU×2 BLK×2                         ║
║  [2-2] (empty)                                               ║
║  [2-3] ★    +GRN  RED×5                                      ║
╠══════════════════════════════════════════════════════════════╣
║  LEVEL 1  [deck: 30]                                         ║
║  [1-0]      +BLK  GRN×1 BLU×2 RED×1                         ║
║  [1-1]      +WHT  BLK×2 RED×1                                ║
║  [1-2]      +BLU  GRN×3                                      ║
║  [1-3]      +RED  WHT×1 BLU×1 GRN×1 BLK×1                  ║
╠══════════════════════════════════════════════════════════════╣
║  YOUR STATUS                                                  ║
║  Tokens:  GRN:2  WHT:1  BLU:0  BLK:3  RED:1  GLD:1  (8/10) ║
║  Bonuses: GRN:1  WHT:0  BLU:2  BLK:0  RED:0                 ║
║  Reserved: [r-0] ★ +BLU  RED×2 BLK×1                        ║
╠══════════════════════════════════════════════════════════════╣
║  OTHER PLAYERS                                               ║
║  AI  Score:3  Tokens:5  Bonuses: BLK:2  Reserved:1           ║
╚══════════════════════════════════════════════════════════════╝
```

The board stays static. The interactive menu appears as an overlay or below the board when needed.

---

## Colour Scheme (ANSI)

| Element              | Colour                        |
|----------------------|-------------------------------|
| GREEN gem / token    | Bright Green `\u001B[32m`     |
| WHITE gem / token    | Bright White `\u001B[97m`     |
| BLUE gem / token     | Bright Blue `\u001B[34m`      |
| BLACK gem / token    | Dark Grey `\u001B[90m`        |
| RED gem / token      | Bright Red `\u001B[31m`       |
| GOLD gem / token     | Bright Yellow `\u001B[33m`    |
| Prestige points ★    | Yellow `\u001B[33m`           |
| Section headers      | Bold `\u001B[1m`              |
| Current player name  | Bold Cyan `\u001B[1;36m`      |
| Selected menu item   | Black on White (inverted) `\u001B[7m` |
| Greyed-out (invalid) | Dark Grey `\u001B[90m`        |
| Reset                | `\u001B[0m`                   |

---

## Arrow-Key Navigation: Menu System

All human input goes through a `Menu` helper class.

### How it works

1. Print a numbered list of options (the board stays above)
2. Highlight the selected item with inverted colours
3. `UP` / `DOWN` arrow keys move the cursor
4. `ENTER` confirms the selection
5. `ESC` or `BACKSPACE` goes back one step

### Action menus (multi-step flow)

```
Step 1 — Choose action type
  > Take 3 different gems
    Take 2 of the same gem
    Buy a card
    Reserve a card

Step 2a (Take 3 gems) — Pick first gem colour
  > GREEN  (4 available)
    WHITE  (3 available)
    BLUE   (2 available)
    BLACK  (4 available)
    RED    (3 available)
  ... then pick 2nd, then 3rd (already-chosen colours greyed out)

Step 2b (Take 2 gems) — Pick a colour (only shows colours with 4+)
  > BLACK  (4 available)
    GREEN  (4 available)

Step 2c (Buy a card) — Pick a card
  > [1-0]  ★   +BLK  GRN×1 BLU×2 RED×1   [can afford]
    [1-1]  ★   +WHT  BLK×2 RED×1          [can afford]
    [2-0]  ★★  +BLU  GRN×2 RED×1 BLK×4   [need 1 more BLK]
    [r-0]  ★   +BLU  RED×2 BLK×1          [can afford]
  (unaffordable cards shown in grey, still selectable but rejected with message)

Step 2d (Reserve a card) — Pick a card or deck
  > [1-0] ...
    [deck 1]  (draw blind from Level 1)
    [deck 2]  (draw blind from Level 2)
    [deck 3]  (draw blind from Level 3)

Return tokens (if over 10) — Pick one token at a time to return
  You have 11 tokens. Return 1.
  > GREEN  (you have 2)
    BLACK  (you have 3)
    ...
```

---

## Files to Create / Modify

### New files

| File | Purpose |
|------|---------|
| `src/model/TerminalRenderer.java` | Replaces `ConsoleUI.java`. Renders the full board to stdout using ANSI escape codes and box-drawing characters. Stateless — call `render(board, players, currentIndex)` to redraw. |
| `src/model/AnsiMenu.java` | Arrow-key menu utility. Takes a `List<String>` of options, renders them with a highlighted cursor, reads raw keypresses via Lanterna (or `System.in` raw mode), returns the chosen index. |
| `src/model/AnsiColors.java` | Constants for all ANSI escape codes used in the project — colours, bold, reset, clear screen. |
| `src/model/TurnController.java` | Orchestrates a single human turn: calls `TerminalRenderer`, then uses `AnsiMenu` to walk through the multi-step action flow, then calls the appropriate `Game` method. Extracted from `Main.java`. |

### Modified files

| File | Change |
|------|--------|
| `src/model/Main.java` | Remove inline `printBoard`, `printPlayerStatus`, all action methods. Replace with calls to `TerminalRenderer.render()` and `TurnController.doHumanTurn()`. Keep setup and game loop. |
| `src/model/ConsoleUI.java` | Can be deleted or kept for reference — `TerminalRenderer` supersedes it. |

---

## Implementation Order

1. **`AnsiColors.java`** — define all constants first, nothing depends on these
2. **`TerminalRenderer.java`** — render the static board display; test by calling it standalone
3. **`AnsiMenu.java`** — implement arrow-key selection; test with a dummy list
4. **`TurnController.java`** — wire up the full turn flow using the renderer and menus
5. **`Main.java`** — swap out old print/scanner code for the new classes
6. Delete or archive `ConsoleUI.java`

---

## Key Implementation Notes

### Screen clear on each turn
```java
// AnsiColors.java
public static final String CLEAR = "\u001B[2J\u001B[H"; // clear screen + move cursor to top

// TerminalRenderer.java — call at the top of render()
System.out.print(AnsiColors.CLEAR);
```

### Reading arrow keys without Lanterna (fallback)
Arrow keys send 3-byte escape sequences: `ESC [ A` (up), `ESC [ B` (down), `ESC [ C` (right), `ESC [ D` (left).

To read them raw, switch the terminal to raw mode:
```java
// Unix/Mac: use stty via Runtime.exec
Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty raw -echo </dev/tty"});
// Windows: Lanterna handles this automatically — preferred for cross-platform
```

Using Lanterna's `DefaultTerminalFactory` is simpler and cross-platform — strongly recommended.

### Token dot symbols
Use Unicode circles to visually represent gem tokens instead of plain text:
- `●` (U+25CF) coloured with ANSI = coloured gem chip
- `★` (U+2605) yellow = prestige point star

### Greying out unaffordable cards
In the buy-card menu, compute `canAffordCard()` for each card before rendering. Wrap unaffordable options in `AnsiColors.DIM` so they visually communicate they are not available, but still allow selection (to show the player what they're missing).

---

## Out of Scope for This Plan

- Network / multiplayer UI (handled separately in `src/network/`)
- AI turn visualisation (AI turn already prints its move; no interactive menu needed)
- Mouse support
- Saving / loading game state
