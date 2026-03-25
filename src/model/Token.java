package model;

// the 5 gem colors used in the game, plus gold which acts as a wild card
public enum Token {
   GREEN,
   WHITE,
   BLUE,
   BLACK,
   RED,
   GOLD; // wild — given to a player when they reserve a card

   private Token() {
   }
}
