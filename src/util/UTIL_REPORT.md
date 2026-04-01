# `src/util` Comprehensive Report

This document explains every file and every function under `src/util`. The goal is not just to restate names, but to make the design easy to defend in an oral check.

## Big Picture

`src/util` contains two main responsibilities:

1. Game startup and loop orchestration
2. Console UI rendering and interaction

The package is split like this:

- `util/AnsiSupport.java`: decides whether ANSI color/styling should be used
- `util/GameApp.java`: builds the game, runs the turn loop, and handles player actions
- `util/SplashScreen.java`: prints the startup title screen
- `util/ui/*`: the modular console UI system

The important design idea is separation of concerns:

- `GameApp` controls game flow and calls methods on `logic.Game`
- `ConsoleUI` handles actual printing and keyboard input
- `ConsoleRenderer` and the package-private `Console*View` helpers only build strings
- Formatting helpers like `ConsoleText` and `ConsoleTokenFormat` keep layout logic out of the higher-level classes

That separation is one of the strongest things to mention to your professor.

---

## File: `AnsiSupport.java`

### Purpose

This class decides whether the terminal should use ANSI escape sequences for color, bold text, dim text, screen clearing, and styled boxes.

It is deliberately tiny and `final`, with only static behavior.

### Class design

- `final`: meant to be a utility class, not extended
- private constructor: prevents accidental instantiation

### Function: `isSupported()`

### What it does

Returns `true` if the program should output ANSI styling codes, otherwise `false`.

### Logic

1. If `System.console()` is `null`, it returns `false`
2. It reads the operating system name from `os.name`
3. If the OS looks like Windows, it returns `true`
4. Otherwise it checks the `TERM` environment variable
5. It returns `true` if `TERM` exists and is not `"dumb"`

### Why it is written this way

- `System.console() == null` usually means output is not attached to an interactive terminal
- Windows terminals historically needed extra setup for ANSI support, so the comment explains that Jansi should be installed before using the UI
- Unix-like terminals usually expose ANSI capability through `TERM`

### Key oral explanation

This is a capability detector. It prevents raw escape sequences from appearing in unsupported consoles and allows the rest of the UI to stay platform-aware without duplicating checks.

---

## File: `GameApp.java`

### Purpose

This is the main game coordinator for local play. It does not implement the underlying game rules itself; instead, it:

- loads cards and nobles
- creates the `Board`, `Deck`, and `Player` objects
- runs the main turn loop
- asks `logic.Game` whether actions are legal
- calls `logic.Game` to execute legal actions
- delegates display work to `ConsoleUI`
- delegates AI choices to `SplendorAI`

### Constants

### `DEFAULT_WIN_SCORE`

- pulled from `GameConfig.getWinningPoints()`
- acts as the default target score for the overload of `runGameLoop`

### `CARDS_FILEPATH`

- path to the card CSV from config

### `NOBLES_FILEPATH`

- path to the noble CSV from config

### Function: `setupGame(int numPlayers, boolean[] isAI)`

### What it does

Convenience overload that calls the 3-argument version with `playerNames = null`.

### Why it exists

Lets callers create a game without explicitly providing names.

### Function: `setupGame(int numPlayers, boolean[] isAI, String[] playerNames)`

### What it does

Builds a fully initialized `Game` object.

### Step-by-step behavior

1. Loads all cards from CSV using `CardLoader.loadCards`
2. On failure, prints an error and continues with an empty list
3. Splits cards into level 1, 2, and 3 lists
4. Builds one `Deck` per level
5. Shuffles each deck
6. Loads all nobles from CSV using `CardLoader.loadNobles`
7. On failure, prints an error and continues with an empty noble list
8. Shuffles the nobles
9. Chooses how many nobles to show using `GameConfig.getInitialNobles(numPlayers)`
10. Creates the `Board`
11. Creates each `Player`
12. Chooses player names with this priority:
- provided nonblank custom name
- `"AI: X"` for AI players
- `"Player: X"` for humans
13. Returns `new Game(board, players)`

### Important design points

- This method prepares state, but does not start the loop
- It assumes `logic.Game`, `Board`, `Deck`, and `Player` enforce actual rule behavior later
- Error handling is soft: failures are reported to `stderr`, not thrown upward

### Potential professor question

If file loading fails, the method still returns a `Game`, but potentially with empty decks or nobles. That keeps the program from crashing immediately, but it may produce a broken game state later. This is a robustness tradeoff.

### Function: `runGameLoop(Game game, Scanner sc, ConsoleUI ui)`

### What it does

Overload that uses `DEFAULT_WIN_SCORE`.

### Function: `runGameLoop(Game game, Scanner sc, ConsoleUI ui, int winScore)`

### What it does

Runs the full turn-by-turn local game until the game ends or the user quits.

### High-level flow

1. Prints a text intro banner
2. Repeats while `!game.isGameOver()`
3. Gets the current player
4. Displays the whole board via `ui.displayGameState`
5. Handles either a human turn or an AI turn
6. If the action was valid:
- forces token returns if above limit
- checks noble visitation
- triggers the final round if score reached `winScore`
- advances to next turn
7. After loop exits, determines and prints the winner

### Human turn handling

- shows the action menu
- reads input with `readLine(sc)`
- accepts:
- `1`: take gems
- `2`: buy a card
- `3`: reserve a card
- `r`: show reserved cards
- `q`: quit
- keeps asking until a valid action succeeds

### AI turn handling

- calls `doAITurn`
- pauses for Enter so the player can read what happened

### Endgame logic

When a player reaches the target score:

- the game does not immediately stop
- `game.triggerEnd(game.getCurrentPlayerIndex())` starts the final-round logic
- the rest of the round finishes according to `logic.Game`

That matches Splendor’s end-of-round structure.

### Function: `printActionMenu(Player p)`

### What it does

Prints the local player action menu.

### Important note

The parameter `p` is not actually used in the body. It could be removed without changing behavior. The method probably kept the parameter for symmetry or future customization.

### Function: `doTakeGems(Game game, Player p, Scanner sc)`

### What it does

Handles the “take gems” action for a human player.

### Logic

1. Reads token availability from the board
2. Builds a list of available non-gold colors
3. If no colors exist, action fails
4. Shows each available color with a number
5. Explains the two legal input forms:
- two identical numbers means “take two of same color”
- three numbers means “take three different colors”
6. Parses the line
7. If two equal numbers:
- validates index
- checks `game.canTakeTwoSameGems`
- calls `game.takeTwoSameGems`
8. If three numbers:
- validates all indices
- checks `game.canTakeThreeDifferentGems`
- calls `game.takeThreeDifferentGems`
9. Otherwise prints an error and returns `false`

### Why this is important

This method does not hardcode Splendor legality itself. It lets the `Game` object decide with `canTake...` methods, then calls the corresponding action method.

### Function: `getBuyableSlotTags(Game game, Player p)`

### What it does

Returns a list of identifiers for all cards the player can currently afford.

### Output format

- visible market cards: `"level-slot"` such as `"2-1"`
- reserved cards: `"r-index"` such as `"r-0"`

### Logic

1. Scans all visible cards in levels 1 to 3
2. Adds a tag if `game.canBuyVisibleCard` is true
3. Scans reserved cards in the player hand
4. Adds a tag if `game.canBuyReservedCard` is true

### Why it matters

This is a reusable query/helper method. It separates “what can be bought” from “how a human menu is shown.”

### Function: `doBuyCard(Game game, Player p, Scanner sc)`

### What it does

Shows all currently affordable cards and executes the selected purchase.

### Logic

1. Builds two parallel lists:
- `options`: strings shown to the user
- `actions`: `Runnable`s that execute the matching purchase
2. Scans visible cards and reserved cards
3. Adds an action only if the card is affordable
4. Prints the numbered list
5. Reads numeric input
6. Runs the chosen `Runnable`

### Why the `Runnable` design is good

It decouples menu display from execution. Once the menu is built, selection becomes a simple index lookup.

### Formatting behavior

Each option includes:

- level/slot or reserved id
- prestige points
- bonus token label
- compact cost string from `ConsoleUI.formatCostCompactStatic`

### Function: `doReserveCard(Game game, Player p, Scanner sc)`

### What it does

Shows all reservable cards and executes the selected reserve action.

### Logic

1. Rejects action if the player already has 3 reserved cards
2. Builds `options` and `actions`
3. Adds visible card reserve options from level 3 down to 1
4. Adds face-down deck reserve options for deck levels 1 to 3
5. Prints the menu
6. Runs the selected reserve action

### Design notes

- Visible cards are listed highest level first, likely to emphasize stronger cards
- Deck reservations are listed separately with remaining deck count
- Again, `Runnable` is used to unify menu and execution

### Function: `returnExcessTokens(Game game, Player p, Scanner sc)`

### What it does

Enforces the 10-token limit for a human player after a successful action.

### Logic

1. Returns immediately if the player is within the limit
2. Shows how many tokens must be returned
3. Repeats while `game.mustReturnTokens(p)` is true
4. Builds a numbered list of token types the player actually holds
5. Reads a number
6. Calls `game.returnToken(p, t, 1)`

### Why it is written this way

The player chooses exactly which tokens to give back, which matters strategically.

### Function: `doAITurn(Game game, Player p)`

### What it does

Interprets the AI’s encoded action string and tries to execute it.

### Input source

`SplendorAI.chooseAction(p, board)`

### Expected action formats

- `"1:T1:T2:T3"` for taking three different gems
- `"2:T"` for taking two of the same gem
- `"3:r:index"` for buying a reserved card
- `"3:level:slot"` for buying a visible card
- `"4:deck:level"` for reserving from a deck
- `"4:level:slot"` for reserving a visible card

### Logic

1. Ask AI for an action string
2. If `null`, skip turn with a message
3. Split by `":"`
4. Match action type by `parts[0]`
5. Validate legality through `game.can...`
6. Execute the corresponding game action
7. Print a one-line summary
8. Catch any exception and report failure

### Important oral point

The AI does not directly mutate state. It proposes an action string, and `GameApp` still validates and executes it through `logic.Game`.

### Function: `returnExcessTokensAI(Game game, Player p)`

### What it does

Makes the AI return tokens when over the limit.

### Logic

1. Checks if return is needed
2. Asks `SplendorAI.chooseTokensToReturn`
3. Returns each listed token by calling `game.returnToken`

### Design point

The AI chooses the strategy, but `GameApp` performs the actual mutation.

### Function: `checkNobleVisit(Game game, Player p)`

### What it does

Checks whether the player qualifies for a noble and prints a message if one is awarded.

### Logic

- calls `game.checkAndAwardNoble(p)`
- if result is non-null, prints the noble visit announcement

### Function: `printLine(String ch, int len)`

### What it does

Prints a repeated-character separator using `String.repeat`.

### Function: `tokenLabel(Token token)`

### What it does

Converts a `Token` enum value into a short display label:

- `BLACK -> Blk`
- `BLUE -> Blu`
- `GREEN -> Grn`
- `RED -> Red`
- `WHITE -> Wht`
- `GOLD -> Gld`

### Why it exists

Compact labels fit menus and card summaries better than full names.

### Function: `formatCardShort(Card c)`

### What it does

Builds a one-line card summary for AI action messages.

### Example shape

`PV:2 BLUE+ Blk1 Red2`

### Function: `readLine(Scanner sc)`

### What it does

Reads a line safely from the scanner.

### Behavior

- returns `null` if there is no next line
- otherwise trims whitespace
- converts input to lowercase

### Why that matters

Menu handling becomes simpler because uppercase and lowercase are treated the same.

---

## File: `SplashScreen.java`

### Purpose

Displays the large startup title screen for three contexts:

- local game
- server mode
- client mode

This class is presentation-only.

### Constants

This file defines ANSI styling constants such as:

- `RESET`, `BOLD`, `DIM`
- gem colors for black, blue, green, red, white, gold
- `CLEAR` for clear-screen behavior

It also defines layout constants:

- `TITLE`: block-art rows for the “SPLENDOR” banner
- `TITLE_WIDTH`: visible width of the art
- `INNER`: content width inside the outer box
- `TITLE_PAD`: computed left padding used to center the art

### Function: `local()`

### What it does

Calls `render` with text specific to local play.

### Function: `server(int numPlayers, int port)`

### What it does

Calls `render` with text specific to server mode, including player count and port.

### Function: `client()`

### What it does

Calls `render` with text specific to client mode.

### Function: `render(boolean ansi, String modeLine, String detailLine)`

### What it does

Builds and prints the full splash screen.

### Logic

1. Clears the screen
2. Builds box border strings
3. Colors the outer border gold if ANSI is enabled
4. Prints top border and blank padding lines
5. Prints the ASCII title, centered
6. Prints the gem legend
7. Prints the tagline
8. Prints mode/detail lines
9. Prints bottom border

### Design strengths

- The public entry methods are tiny and expressive
- `render` centralizes all layout logic
- ANSI and non-ANSI modes both work
- visible width is carefully managed

### Function: `gemLegend(boolean ansi)`

### What it does

Returns the legend row describing gem colors.

### Behavior

- without ANSI: plain text dots and star labels
- with ANSI: each symbol is colored to match the gem

### Function: `centred(String text, int width, boolean ansi)`

### What it does

Centers text in a fixed visible width.

### Key detail

It uses `visLen(text)` so ANSI escape sequences do not break width calculations.

### Function: `visLen(String s)`

### What it does

Measures visible length by removing ANSI escape codes before calling `length()`.

### Function: `println(String s)`

### What it does

Tiny wrapper around `System.out.println`.

### Why it may exist

Mostly stylistic consistency inside the renderer.

### Function: `clearScreen(boolean ansi)`

### What it does

Clears the terminal before drawing the splash screen.

### Behavior

- ANSI mode: sends the clear-screen escape code and flushes output
- fallback mode: prints 40 blank lines to push old content away

---

## File: `ui/ConsoleAnsi.java`

### Purpose

Provides ANSI-related text styling utilities used by the console UI package.

This class is package-private, so it is an internal helper rather than part of the public UI API.

### Constants

- `ESC`: ANSI escape prefix
- `RESET`: reset sequence
- `BOLD`: bold sequence
- `DIM`: dim sequence

### Function: `bold(String text, boolean ansi)`

### What it does

Wraps text in bold escape codes if ANSI is enabled.

### Function: `dim(String text, boolean ansi)`

### What it does

Wraps text in dim styling if ANSI is enabled.

### Function: `tokenShortLabel(Token token)`

### What it does

Returns compact 3-letter token labels.

### Function: `tokenLabel(Token token)`

### What it does

Returns user-facing token text. Right now it is effectively the same as `tokenShortLabel`, except `null` becomes `"(none)"`.

### Function: `colorizeToken(Token token, String text, boolean ansi)`

### What it does

Applies a foreground color to token-related text.

### Why it is important

The method colors only the token abbreviation, not surrounding text, so spacing and alignment remain stable.

### Function: `gemChip(Token token, boolean ansi)`

### What it does

Returns a small token badge used inside card boxes.

### Behavior

- non-ANSI: plain label such as `Blk`
- ANSI: background-colored chip such as a colored rectangle with the token label inside

### Function: `gemBorderColor(Token token)`

### What it does

Returns the ANSI border color code that should be used for cards of a given bonus color.

### Why it exists

It makes market cards visually scannable by matching border color to bonus color.

---

## File: `ui/ConsoleBoxes.java`

### Purpose

Builds borders and joins multiple rendered boxes into rows or grids.

This class is the structural layout helper for card boxes, noble boxes, and player panels.

### Function: `borderTop(int innerWidth, String borderColor, boolean ansi)`

### What it does

Builds the top border for a box of visible width `innerWidth`.

### Behavior

- uses Unicode box-drawing characters from `ConsoleConstants`
- optionally wraps the full border in ANSI color

### Function: `borderBottom(int innerWidth, String borderColor, boolean ansi)`

### What it does

Same as `borderTop`, but for the bottom border.

### Function: `borderLeft(String borderColor, boolean ansi)`

### What it does

Returns the left vertical border, optionally colored.

### Function: `borderRight(String borderColor, boolean ansi)`

### What it does

Returns the right vertical border, optionally colored.

### Function: `joinBoxesSideBySide(List<List<String>> boxes)`

### What it does

Takes several already-rendered boxes and places them horizontally beside each other.

### Important logic

1. Finds the tallest box height
2. Pads shorter boxes with blank inner rows
3. Keeps their bottom borders at the bottom
4. Joins each row with two spaces between boxes

### Why it is necessary

Different content can produce different box heights. Without normalization, side-by-side layout would break.

### Function: `renderBoxesGrid(List<List<String>> boxes, int columns)`

### What it does

Renders many boxes into multiple rows of a grid.

### Logic

1. Splits the box list into blocks of `columns`
2. Calls `joinBoxesSideBySide` on each block
3. Adds blank lines between row blocks

### Typical uses

- player boxes: 2 columns
- noble boxes: up to 3 columns

---

## File: `ui/ConsoleConstants.java`

### Purpose

Stores layout constants shared across the console UI.

This centralization makes sizes easy to justify and tweak.

### Constants

### `MAX_RESERVED`

- maximum reserved cards per player
- reflects the Splendor rule of 3

### Width constants

- `CARD_BOX_WIDTH = 30`
- `NOBLE_BOX_WIDTH = 34`
- `PLAYER_BOX_WIDTH = 54`

These are total widths including borders.

### Border glyph constants

- `H`, `TL`, `TR`, `BL`, `BR`, `VB`

These are Unicode box-drawing characters.

### `COST_ORDER`

- fixed token order used everywhere:
- black, blue, green, red, white

### Why fixed ordering matters

Consistent ordering makes costs and token maps easier to compare visually across the UI.

---

## File: `ui/ConsoleGameOverView.java`

### Purpose

Builds the final “game over” screen, including both the final board and the winner/standings banner.

### Function: `renderGameOverString(Game game, boolean ansi, int winScore)`

### What it does

Builds the full end screen as a string.

### Logic

1. Calls `ConsoleGameStateView.renderGameStateString(...)`
2. Adds a blank line
3. Appends the game-over banner from `renderGameOverBanner(...)`

### Why this design is good

The end screen reuses the normal board renderer, so there is one consistent way to show the game state.

### Function: `renderGameOverBanner(Game game, boolean ansi, int winScore)`

### What it does

Builds a gold-framed results box showing:

- “GAME OVER”
- winner
- winner score
- congratulation line
- final standings
- tie-break explanation

### Logic

1. Gets all players and the winner
2. Sets fixed width `inner = 78`
3. Uses gold ANSI border color when ANSI is enabled
4. Builds title and trophy strings
5. Adds top/title/blank lines
6. If winner exists:
- prints winner name
- prints winner score relative to `winScore`
- prints congratulations
7. Otherwise prints a fallback line
8. Builds a copied `sorted` list of players
9. Sorts by:
- higher score first
- fewer purchased cards first as tie-break
10. Prints each player’s rank, score, card count, and noble count
11. Adds the tie-break note
12. Adds bottom border

### Important oral point

This view does not decide who wins. It asks `game.determineWinner()`. The UI only formats the outcome.

---

## File: `ui/ConsoleGameStateView.java`

### Purpose

Builds the main in-game board view shown every turn.

This is one of the most important UI files because it assembles the whole screen from smaller rendering helpers.

### Function: `renderGameStateString(Game game, boolean ansi, int winScore)`

### What it does

Builds the complete multi-line game-state string for the current turn.

### Main sections in output order

1. Header
2. Legend
3. Turn or final banner
4. Score summary
5. Player grid
6. Bank/token supply
7. Nobles
8. Market rows
9. Reserved-card tip for humans

### Detailed logic

1. Validates `game` with `Objects.requireNonNull`
2. Reads:
- board
- players
- current player index
- current player
- leader score using `ConsoleScoreView.maxScore`
3. Builds header text with bold and dim styling
4. Builds the color legend using `ConsoleAnsi.colorizeToken`
5. If the game is over:
- show `FINAL: Game Over`
- show score summary with current index `-1`
6. Otherwise:
- compute how many points current player still needs
- show `TURN: Pn Name`
- show highlighted score badge
- show full score summary
7. Append player summaries from `ConsolePlayerView.renderPlayersGrid`
8. Append bank token counts using `ConsoleTokenFormat.formatTokenMap`
9. Append nobles section using `ConsoleNobleView.renderNoblesSection`
10. Append market section level 3 to 1, including deck counts
11. If current player is human and has reserved cards, append a tip about `[R]`

### Why it is good architecture

This class reads like a top-level storyboard of the screen. It does not contain low-level box rendering or token formatting details.

---

## File: `ui/ConsoleMarketView.java`

### Purpose

Renders the market rows and individual card boxes.

### Function: `buildMarketRowString(int level, Card[] row, boolean ansi)`

### What it does

Builds one full market row for a given card level.

### Logic

1. Precomputes wrapped cost chunks for each card
2. Tracks the maximum number of cost lines needed by any card in the row
3. Renders each slot as a box using that shared max line count
4. Joins the boxes horizontally with `ConsoleBoxes.joinBoxesSideBySide`

### Why precomputing matters

If one card has a long cost string and another has a short one, all boxes still stay the same height.

### Function: `computeWrappedCostChunks(Card card, int inner, boolean ansi)`

### What it does

Determines how many visible lines are needed for the card’s cost text inside a fixed-width card box.

### Logic

1. Builds the prefix: bonus chip plus `"Cost "`
2. Measures visible width of the prefix
3. Computes remaining width for cost text
4. Formats the cost compactly
5. Wraps it with `ConsoleText.wrapBySpaces`
6. Returns `"-"` if no chunks exist

### Function: `renderCardBox(String id, Card card, boolean ansi, List<String> costChunks, int maxCostLines)`

### What it does

Creates the individual lines for one market card box.

### Logic for non-null card

1. Chooses border color from the card’s bonus
2. Builds top/bottom/left/right borders
3. Adds top border
4. Adds first content line with slot id on the left and points on the right
5. Adds the first cost line with bonus chip and first cost chunk
6. Adds any additional wrapped cost lines, aligned under the cost section
7. Adds bottom border

### Logic for null card

1. Shows the slot id
2. Shows `(empty)`
3. Pads remaining needed lines
4. Adds bottom border

### Important design point

The method uses fixed `CARD_BOX_WIDTH`, visible-length calculation, and shared `maxCostLines` to keep market rows aligned.

---

## File: `ui/ConsoleNobleView.java`

### Purpose

Renders the nobles section and shows how close the current player is to each noble.

### Function: `renderNoblesSection(List<Noble> nobles, Player currentPlayer, boolean ansi)`

### What it does

Builds the full nobles section.

### Logic

1. Adds the “Nobles” heading
2. If there are none, prints `(none)`
3. Otherwise renders each noble box
4. Arranges them in a grid of up to 3 columns

### Function: `renderNobleBox(int index, Noble noble, Player currentPlayer, boolean ansi)`

### What it does

Builds a single noble box.

### Content

- `[Nindex] name`
- prestige points
- required bonuses
- current player’s remaining requirements or `*** READY! ***`

### Logic

1. Creates a box using `NOBLE_BOX_WIDTH`
2. Prints id/name on the left and points on the right
3. Prints full cost
4. Compares noble cost against the current player’s permanent bonuses
5. For each missing color, appends colored token label plus amount still needed
6. If nothing is missing, shows `READY`

### Important oral point

The progress shown here is specifically for the current player, not all players. It is a tactical helper.

---

## File: `ui/ConsolePlayerView.java`

### Purpose

Renders the per-player summary boxes at the top of the board.

### Function: `renderPlayersGrid(List<Player> players, int currentIndex, int winScore, int leaderScore, boolean ansi)`

### What it does

Builds all player boxes and arranges them into a grid.

### Layout rule

- 1 or 2 players: that many columns
- 3 or 4 players: 2 columns

### Function: `renderPlayerBox(int index, Player player, boolean isCurrent, int winScore, int leaderScore, boolean ansi)`

### What it does

Builds one player summary box.

### Content

- player id, human/AI marker, and name
- right-side current-turn marker `<==`
- score badge
- reserved count, bought-card count, noble count
- token inventory
- permanent bonuses

### Logic

1. Uses `PLAYER_BOX_WIDTH`
2. Uses bold border styling if ANSI is enabled
3. Builds name row, bolding it if current
4. Uses `ConsoleScoreView.renderScoreBadge`
5. Uses `ConsoleTokenFormat.formatTokenMapFixed` for tokens and bonuses

### Why fixed-width token formatting matters

Counts can change every turn. Fixed-width formatting prevents the box borders from shifting horizontally.

---

## File: `ui/ConsoleRenderer.java`

### Purpose

This is the public rendering facade for the UI package.

External code calls this class, while the lower-level view classes stay package-private.

### Why this matters

It gives the package a clean public API and hides implementation details.

### Function: `renderGameStateString(Game game, boolean ansi, int winScore)`

### What it does

Delegates directly to `ConsoleGameStateView.renderGameStateString`.

### Function: `renderGameOverString(Game game, boolean ansi, int winScore)`

### What it does

Delegates directly to `ConsoleGameOverView.renderGameOverString`.

### Function: `renderReservedCardsString(Player player, boolean ansi)`

### What it does

Delegates directly to `ConsoleReservedView.renderReservedCardsString`.

### Function: `formatCostCompactStatic(Map<Token, Integer> cost)`

### What it does

Delegates directly to `ConsoleTokenFormat.formatCostCompactStatic`.

### Function: `renderGameStateString(Game game, boolean ansi)`

### What it does

Backward-compatible overload that defaults `winScore` to 10.

### Why the class is useful

If the internal renderer classes are reorganized later, callers do not need to change as long as this facade stays stable.

---

## File: `ui/ConsoleReservedView.java`

### Purpose

Renders the screen showing a player’s reserved cards.

### Function: `renderReservedCardsString(Player player, boolean ansi)`

### What it does

Builds the reserved-card view for one player.

### Content

- title with reserved count
- player name
- current permanent bonuses
- one section per reserved card

For each card it shows:

- slot id like `[r-0]`
- whether it is currently affordable
- card summary
- remaining cost after applying bonuses

### Logic

1. Null-checks `player`
2. Prints title, player, and bonuses
3. If hand is empty, prints `(none)`
4. Otherwise loops through each reserved card
5. Uses `player.canAffordCard(c)` to determine affordability label
6. Uses `ConsoleTokenFormat.formatCard`
7. Uses `ConsoleTokenFormat.remainingAfterBonuses`

### Important oral point

This view is informational only. It does not buy cards. It helps the player evaluate future moves.

---

## File: `ui/ConsoleScoreView.java`

### Purpose

Formats score-related UI elements.

### Function: `maxScore(List<Player> players)`

### What it does

Returns the highest player score in the list.

### Behavior

- if no players, returns `0`

### Function: `renderScoresSummary(List<Player> players, int currentIndex, int winScore, int leaderScore, boolean ansi)`

### What it does

Builds the compact score strip used near the top of the screen.

### Output style

Each player is shown as `P# score/winScore`.

### Highlight rules

- non-ANSI:
- current player gets brackets
- leader gets `*` if not current
- ANSI:
- current player gets bold brackets
- leader gets bold text

### Function: `renderScoreBadge(int score, int winScore, int toWin, boolean isCurrent, boolean isLeader, boolean ansi)`

### What it does

Builds the more prominent per-player score badge used inside player boxes and the turn banner.

### Output content

`SCORE: current/target   LEFT: remaining`

### Highlight rules

- no ANSI:
- current player gets `>>`
- leader gets `*`
- ANSI:
- current player gets bold black-on-yellow
- leader gets bold black-on-green
- others get bold text

### Why this class is separate

Score formatting rules are reused in multiple places, so centralizing them avoids duplicated styling logic.

---

## File: `ui/ConsoleText.java`

### Purpose

Provides low-level text layout utilities that are ANSI-aware.

This file is crucial because ANSI escape sequences break normal `String.length()` alignment logic.

### Function: `stripAnsi(String s)`

### What it does

Removes ANSI escape sequences from a string using regex.

### Function: `visLen(String s)`

### What it does

Returns visible character length by calling `stripAnsi`.

### Function: `padRight(String s, int width)`

### What it does

Pads text on the right until visible width reaches `width`.

### Function: `truncateVisible(String s, int width)`

### What it does

Truncates to a visible-width limit while preserving ANSI escape sequences.

### Important logic

1. If already short enough, return unchanged
2. If width is tiny, return dots only
3. Walk through characters manually
4. Copy ANSI sequences without counting them as visible width
5. Count visible characters normally
6. Append `...`
7. Append `RESET` if ANSI sequences were present

### Why this is hard

A normal substring could cut an ANSI sequence in half or miscount visible length. This method avoids that.

### Function: `fitLine(String s, int width)`

### What it does

Guarantees exact visible width:

- truncates if too long
- pads if too short

### Function: `rightMarkerLine(String left, String right, int innerWidth)`

### What it does

Places a left label and right marker on one line with the marker flush right.

### Use cases

- card header with points on the right
- player row with `<==` on the right

### Function: `centerLine(String text, int width)`

### What it does

Centers text using visible width.

### Function: `wrapBySpaces(String text, int width)`

### What it does

Wraps text into lines that fit a maximum width, splitting at spaces when possible.

### Logic

1. Trim input
2. Split into words
3. Build lines greedily
4. If a word is too long, delegate to `appendWordOrSplit`

### Function: `appendWordOrSplit(List<String> lines, StringBuilder current, String word, int w)`

### What it does

Either appends a word to the current line or splits the word into fixed-width pieces.

### Important note

This is package-private helper logic used only by `wrapBySpaces`.

---

## File: `ui/ConsoleTokenFormat.java`

### Purpose

Formats token maps, card costs, card summaries, and remaining costs after bonuses.

This is strictly formatting plus a light calculation helper for “remaining after bonuses.”

### Function: `pointsLabel(int points)`

### What it does

Returns text like `"3 pts"`.

### Function: `formatCard(Card c, boolean ansi)`

### What it does

Builds a one-line card summary with points, bonus, and cost.

### Function: `formatCost(Map<Token, Integer> cost, boolean ansi)`

### What it does

Formats a cost map in token order, skipping zeros.

### Example

`Blk2 Blu1 Red3`

### Function: `formatTokenMap(Map<Token, Integer> map, boolean includeGold, boolean ansi)`

### What it does

Formats a token inventory such as a bank or player holdings.

### Behavior

- skips zero-count colors
- optionally includes gold
- returns `—` if nothing is present

### Function: `formatTokenMapFixed(Map<Token, Integer> map, boolean includeGold, boolean ansi)`

### What it does

Formats a token map in fixed-width style, always showing every color with two-digit counts.

### Example shape

`Blk02 Blu00 Grn01 Red03 Wht00 Gld01`

### Why it exists

Useful in boxed layouts where width must stay stable.

### Function: `formatCostCompactStatic(Map<Token, Integer> cost)`

### What it does

Formats cost as plain compact text without ANSI.

### Use cases

- action menus
- logs
- UI facade methods

### Function: `remainingAfterBonuses(Card card, Player player)`

### What it does

Computes how much cost remains after permanent card bonuses reduce the price.

### Logic

For each token color in `COST_ORDER`:

- read original card cost
- subtract player bonuses
- clamp at zero
- only store positive remainder

### Important rule connection

This implements Splendor’s permanent-discount idea. It does not spend tokens itself; it only computes the unpaid portion.

---

## File: `ui/ConsoleUI.java`

### Purpose

This is the public console interaction layer.

It differs from `ConsoleRenderer`:

- `ConsoleRenderer` only builds strings
- `ConsoleUI` actually clears the screen, prints, pauses, and reads input

This is a very important architectural distinction.

### Constants

### `ANSI_CLEAR`

- clear-screen escape sequence

### Setup bounds

- `MIN_PLAYERS = 2`
- `MAX_PLAYERS = 4`
- `MIN_WIN_SCORE = 5`
- `MAX_WIN_SCORE = 30`

### ANSI style constants

- `ESC`, `RESET`, `BOLD`, `DIM`

### Fields

### `sc`

- shared `Scanner` used for all interactive input

### `ansiEnabled`

- result of `AnsiSupport.isSupported()`

### Nested class: `SetupOptions`

### Purpose

Simple immutable holder for:

- `numPlayers`
- `winScore`

### Why it exists

It keeps setup-related return values grouped together and keeps the calling code cleaner.

### Constructor: `ConsoleUI()`

### What it does

Creates a new `Scanner(System.in)` and delegates to the other constructor.

### Constructor: `ConsoleUI(Scanner scanner)`

### What it does

Stores the provided scanner and determines ANSI support.

### Function: `getWinningPoints(int defaultWinScore)`

### What it does

Shows a standalone setup prompt asking for the target prestige score.

### Behavior

- clears the screen
- prints heading and explanation
- calls `promptIntInline`

### Function: `getNumberOfPlayers()`

### What it does

Shows a standalone setup prompt asking for player count.

### Function: `displayGameState(Game game, int winScore)`

### What it does

Clears the screen and prints the full current board using `ConsoleRenderer`.

### Function: `displayGameOver(Game game, int winScore)`

### What it does

Clears the screen and prints the final board plus results.

### Function: `displayReservedCards(Player player)`

### What it does

Clears the screen, prints the reserved-card view, then pauses for Enter.

### Important observation

`GameApp` also pauses after calling this method, which may create an extra Enter press depending on how the flow is used. That is a small behavioral detail worth noticing.

### Function: `runLocalSetupMenu(int defaultPlayers, int defaultWinScore)`

### What it does

Runs a single-screen setup menu where current settings stay visible while the user edits them.

### Logic

1. Clamp defaults into valid ranges
2. Loop forever:
- clear screen
- print current settings
- ask for choice
3. Handle:
- `1`: edit players
- `2`: edit win score
- `s`: return new `SetupOptions`
- `q`: return `null`
- invalid: show error and pause

### Why it is nice UI

The user does not bounce between unrelated setup screens. Everything stays visible in one place.

### Function: `getPlayerTypes(int numPlayers)`

### What it does

Prompts whether each player is AI or human.

### Behavior

- clamps player count into valid range
- uses `y/n` prompts
- blank input defaults to human

### Function: `renderGameStateString(Game game, boolean ansi, int winScore)`

### What it does

Static convenience wrapper around `ConsoleRenderer.renderGameStateString`.

### Function: `renderGameOverString(Game game, boolean ansi, int winScore)`

### What it does

Static convenience wrapper around `ConsoleRenderer.renderGameOverString`.

### Function: `formatCostCompactStatic(Map<Token, Integer> cost)`

### What it does

Static convenience wrapper around `ConsoleRenderer.formatCostCompactStatic`.

### Function: `clearScreen()`

### What it does

Clears the terminal for a redraw.

### Behavior

- ANSI enabled: send clear code and flush
- otherwise print many blank lines

### Function: `readLineLower()`

### What it does

Reads a line safely and normalizes it for menu use.

### Behavior

- returns `null` on EOF or read failure
- trims
- lowercases

### Function: `promptIntInline(String label, int min, int max, int current)`

### What it does

Prompts until the user gives a valid integer in range.

### Behavior

- blank input keeps current value
- EOF returns current value
- invalid input prints a range reminder

### Function: `pauseForEnter()`

### What it does

Shows a “Press Enter to continue...” prompt and waits if input is still available.

### Function: `clamp(int value, int min, int max)`

### What it does

Standard min/max clamping helper.

### Function: `bold(String text)`

### What it does

Applies bold styling if ANSI is enabled.

### Function: `dim(String text)`

### What it does

Applies dim styling if ANSI is enabled.

---

## Cross-File Design Summary

### 1. Public API vs internal helpers

Public-facing classes:

- `util.GameApp`
- `util.AnsiSupport`
- `util.SplashScreen`
- `util.ui.ConsoleUI`
- `util.ui.ConsoleRenderer`

Internal package-private helpers:

- `ConsoleAnsi`
- `ConsoleBoxes`
- `ConsoleConstants`
- `ConsoleGameOverView`
- `ConsoleGameStateView`
- `ConsoleMarketView`
- `ConsoleNobleView`
- `ConsolePlayerView`
- `ConsoleReservedView`
- `ConsoleScoreView`
- `ConsoleText`
- `ConsoleTokenFormat`

This is a clean encapsulation pattern. The outer API stays simple while the implementation remains modular.

### 2. Rendering pipeline

The string-rendering flow is:

`ConsoleUI` -> `ConsoleRenderer` -> specific `Console*View` helpers -> formatting/layout helpers

That lets both local and network modes reuse the same rendering.

### 3. ANSI-awareness

Several classes are designed around ANSI-safe width handling:

- `AnsiSupport`
- `SplashScreen`
- `ConsoleAnsi`
- `ConsoleText`
- `ConsoleBoxes`

The key problem they solve is that ANSI escape codes affect string length in memory but not visible width on screen.

### 4. Game logic boundary

The UI/util package generally does not enforce rules directly.

Instead it asks `logic.Game` things like:

- can the player take gems?
- can the player buy this card?
- can the player reserve this card?
- is a noble earned?

That is the correct dependency direction. The UI asks; the game model decides.

### 5. Repeated design pattern: build strings, don’t print

Most UI helpers return strings instead of printing immediately. That gives:

- reusability
- testability
- consistent output between local and network modes

Only `ConsoleUI`, `GameApp`, `SplashScreen`, and some setup/menu methods directly print.

---

## Likely Professor Questions and Strong Answers

### Why split `ConsoleUI` from `ConsoleRenderer`?

Because interaction and rendering are different responsibilities. `ConsoleUI` handles the console device, while `ConsoleRenderer` and the view helpers are pure string builders. That makes the rendering reusable and easier to test.

### Why are there so many small UI classes?

Each one owns a specific piece of the screen: players, market, nobles, scores, reserved cards, game over. That prevents one giant renderer file and makes oral explanation much easier.

### Why does `GameApp` call `game.can...` before `game.action...`?

It keeps rule validation inside the game logic layer. `GameApp` is an orchestrator, not the rule engine.

### Why use package-private classes in `util.ui`?

They hide internal rendering details. Other packages only need `ConsoleUI` or `ConsoleRenderer`.

### Why use fixed widths and visible-length helpers?

Because the console UI uses ANSI codes and box layouts. Normal string length would misalign borders and wrapped text.

### Why does `GameApp` use `Runnable` lists for menus?

It pairs each displayed menu option with the exact action to execute, avoiding large duplicated switch logic after the menu is built.

---

## Small Weak Spots You Should Be Ready to Mention

### `GameApp.printActionMenu(Player p)` does not use `p`

The parameter is unnecessary in the current implementation.

### `ConsoleUI.displayReservedCards(...)` already pauses

`GameApp` also waits for Enter after calling it, so there may be redundant pausing.

### Soft failure in `setupGame(...)`

If CSV loading fails, the method prints an error but still continues. That avoids crashing immediately, but it can lead to an unusable or incomplete game state.

### Some layout code depends on fixed widths

This is intentional for terminal stability, but it means very narrow terminals may still display poorly.

---

## Final Mental Model

If you need a short summary for discussion:

- `GameApp` runs the local game and delegates rules to `logic.Game`
- `ConsoleUI` handles real console interaction
- `ConsoleRenderer` exposes a stable rendering API
- the `Console*View` classes each render one section of the screen
- `ConsoleText`, `ConsoleBoxes`, `ConsoleAnsi`, and `ConsoleTokenFormat` are low-level support layers
- `AnsiSupport` decides whether styling is safe
- `SplashScreen` is the startup-only visual intro

If you explain it in that order, the codebase sounds intentional rather than accidental.
