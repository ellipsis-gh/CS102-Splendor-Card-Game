# CS102 Splendor Card Game

A console-based implementation of the Splendor card game for 2 players.

## How to Run

1. **Compile:**
   ```
   compile.bat
   ```

2. **Run:**
   ```
   run.bat
   ```

## How to Play

- **Human vs AI:** At start, choose "y" to play against the AI
- **Goal:** First player to 15 prestige points wins!
- **Take gems:** Choose 3 different colors OR 2 of the same (need 4+ on board)
- **Buy cards:** Pay the cost with your tokens. Your bonuses reduce the cost. Gold = wild.
- **Reserve:** Take a card from the market or deck (max 3). You can take 1 gold if available.
- **Nobles:** When your bonuses meet a noble's requirements, they visit (+3 pts)
- **Token limit:** Max 10 tokens. Return extras when you go over.

## Card Format

Cards show: `PV:0 GREEN+ Cost: BLACKx2 BLUEx1` = 0 points, gives GREEN bonus, costs 2 black + 1 blue

## Directory Structure
data&nbsp;&nbsp;&nbsp;#csv files\
|--Nobles.csv\
|--Splendor Cards.csv

src\
|--config&nbsp;&nbsp;&nbsp;#configurations - change winning points, etc.\
&nbsp;&nbsp;|--config.properties\
&nbsp;&nbsp;|--GameConfig.java\
|--game&nbsp;&nbsp;&nbsp;#this package is in charge of loading decks, etc\
&nbsp;&nbsp;|--CardLoader.java\
&nbsp;&nbsp;|--ConsoleUI.java\
&nbsp;&nbsp;|--deckGetter.java\
|--logic&nbsp;&nbsp;&nbsp;#this package contains the AI and other game logic\
&nbsp;&nbsp;|--Game.java\
&nbsp;&nbsp;|--SplendorAI.java\
|--model&nbsp;&nbsp;&nbsp;#this package contains all base classes for game items\
&nbsp;&nbsp;|--Bank.java\
&nbsp;&nbsp;|--Board.java\
&nbsp;&nbsp;|--Card.java\
&nbsp;&nbsp;|--Deck.java\
&nbsp;&nbsp;|--Noble.java\
&nbsp;&nbsp;|--Player.java\
&nbsp;&nbsp;|--Token.java\
|--network\
&nbsp;&nbsp;|--ClientHandler.java\
&nbsp;&nbsp;|--ClientMain.java\
&nbsp;&nbsp;|--GameServer.java\
&nbsp;&nbsp;|--NetworkFormatter.java\
&nbsp;&nbsp;|--ServerMain&nbsp;&nbsp;&nbsp;#what's this for?\
&nbsp;&nbsp;|--ServerMain.java\
|--util\
&nbsp;&nbsp;|--ui\
&nbsp;&nbsp;&nbsp;&nbsp;|--ConsoleUI.java\
&nbsp;&nbsp;|--GameApp.java\
|--Main.java&nbsp;&nbsp;&nbsp;#user's access point\
      

## Files

- `model/Main.java` - Game loop and console UI
- `model/SplendorAI.java` - Algorithmic AI (buys when possible, reserves good cards, takes needed gems)
- `model/Board.java` - Board, tokens, cards
- `model/Player.java` - Player state and actions
- `model/Card.java`, `Deck.java`, `Noble.java`, `Token.java` - Game pieces
- `model/CardLoader.java` - Loads cards from CSV
- `Splendor Cards.csv` - Card data
