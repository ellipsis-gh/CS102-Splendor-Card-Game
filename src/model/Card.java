package model;

import java.util.Map;

// one card in the game — has a level, prestige points, a gem bonus, and a cost to buy
public class Card {
    private final int level;                // 1, 2, or 3
    private final int prestigePoints;       // victory points this card gives when purchased
    private final Token bonus;              // the gem color it permanently adds as a discount
    private final Map<Token, Integer> cost; // gems required to buy it

    public Card(int level, int prestigePoints, Token bonus, Map<Token, Integer> cost) {
        this.level = level;
        this.prestigePoints = prestigePoints;
        this.bonus = bonus;
        this.cost = cost;
    }

    public int getLevel() {
        return level;
    }

    public int getPrestigePoints() {
        return prestigePoints;
    }

    public Token getBonus() {
        return bonus;
    }

    public Map<Token, Integer> getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "Card{" +
                "L=" + level +
                ", PV=" + prestigePoints +
                ", B=" + bonus +
                ", Cost=" + cost +
                '}';
    }
}
