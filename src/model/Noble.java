package model;

import java.util.Map;
import interfaces.INoble;

// nobles visit players who collect enough bonuses — they give 3 prestige points
public class Noble implements INoble {
    private final String name;
    private final int prestigePoints; // always 3 in the standard game
    private final Map<Token, Integer> cost; // bonuses a player needs to attract this noble

    public Noble(String name, int prestigePoints, Map<Token, Integer> cost) {
        this.name = (name == null || name.isBlank()) ? "Noble" : name.trim();
        this.prestigePoints = prestigePoints;
        this.cost = cost;
    }

    public Noble(int prestigePoints, Map<Token, Integer> cost) {
        this("Noble", prestigePoints, cost);
    }

    public String getName() {
        return name;
    }

    public int getPrestigePoints() {
        return prestigePoints;
    }

    public Map<Token, Integer> getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "Noble{" +
                "Name=" + name +
                "PV=" + prestigePoints +
                ", Req=" + cost +
                '}';
    }
}
