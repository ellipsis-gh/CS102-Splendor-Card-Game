package com.splendor.model;

import java.util.Map;

public class Noble {
    private final int prestigePoints;
    private final Map<Token, Integer> cost;

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
