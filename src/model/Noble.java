package model;

import java.util.Map;

// nobles visit players who collect enough bonuses — they give 3 prestige points
public class Noble {
    private final int prestigePoints; // always 3 in the standard game
    private final Map<Token, Integer> cost; // bonuses a player needs to attract this noble

    public Noble(int prestigePoints, Map<Token, Integer> cost) {
        this.prestigePoints = prestigePoints;
        this.cost = cost;
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
                "PV=" + prestigePoints +
                ", Req=" + cost +
                '}';
    }
}
