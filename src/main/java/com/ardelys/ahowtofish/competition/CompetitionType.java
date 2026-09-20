package com.ardelys.ahowtofish.competition;

public enum CompetitionType {
    MOST_FISH("Most Fish Caught", "fish"),
    LARGEST_FISH("Largest Fish Caught", "kg"),
    TOTAL_WEIGHT("Highest Total Weight", "kg"),
    HIGHEST_VALUE("Highest Total Value", "$"),
    MOST_RARE("Most Rare Fish Caught", "rare fish");

    private final String displayName;
    private final String unit;

    CompetitionType(String displayName, String unit) {
        this.displayName = displayName;
        this.unit = unit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return switch (this) {
            case MOST_FISH -> "Catch the highest quantity of fish.";
            case LARGEST_FISH -> "Catch the heaviest single fish.";
            case TOTAL_WEIGHT -> "Accumulate the greatest total fish weight.";
            case HIGHEST_VALUE -> "Earn the highest total market value.";
            case MOST_RARE -> "Catch the most rare, legendary, or mythic fish.";
        };
    }
}
