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
|--config&emsp;&emsp;&emsp;#configurations - change winning points, etc.\
&emsp;&emsp;|--config.properties\
&emsp;&emsp;;|--GameConfig.java\
|--game&emsp;&emsp;&emsp;#this package is in charge of loading decks, etc\
&emsp;&emsp;|--CardLoader.java\
&emsp;&emsp;|--ConsoleUI.java\
&emsp;&emsp;|--deckGetter.java\
|--logic&emsp;&emsp;&emsp;#this package contains the AI and other game logic\
&emsp;&emsp;|--Game.java\
&emsp;&emsp;|--SplendorAI.java\
|--model&emsp;&emsp;&emsp;#this package contains all base classes for game items\
&emsp;&emsp;|--Bank.java\
&emsp;&emsp;|--Board.java\
&emsp;&emsp;|--Card.java\
&emsp;&emsp;|--Deck.java\
&emsp;&emsp;|--Noble.java\
&emsp;&emsp;|--Player.java\
&emsp;&emsp;|--Token.java\
|--network\
&emsp;&emsp;|--ClientHandler.java\
&emsp;&emsp;|--ClientMain.java\
&emsp;&emsp;|--GameServer.java\
&emsp;&emsp;|--NetworkFormatter.java\
&emsp;&emsp;|--ServerMain&nbsp;&nbsp;&nbsp;#what's this for?\
&emsp;&emsp;|--ServerMain.java\
|--util\
&emsp;&emsp;|--ui\
&emsp;&emsp;&emsp;&emsp;|--ConsoleUI.java\
&emsp;&emsp;|--GameApp.java\
|--Main.java&emsp;&emsp;&emsp;#user's access point\
      

## Files

- `model/Main.java` - Game loop and console UI
- `model/SplendorAI.java` - Algorithmic AI (buys when possible, reserves good cards, takes needed gems)
- `model/Board.java` - Board, tokens, cards
- `model/Player.java` - Player state and actions
- `model/Card.java`, `Deck.java`, `Noble.java`, `Token.java` - Game pieces
- `model/CardLoader.java` - Loads cards from CSV
- `Splendor Cards.csv` - Card data
