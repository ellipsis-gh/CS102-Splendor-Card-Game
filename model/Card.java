package model;
import java.util.Map;

public class Card {
    private final int level;
    private final int prestigePoints;
    private final Token bonus;
    private final Map<Token, Integer> cost;

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
