package de.hts.player;

/** Geschlecht eines Spielers. click-TT kodiert es als Zahl (1 = männlich, 2 = weiblich). */
public enum Sex {
    MALE("m"),
    FEMALE("w"),
    UNKNOWN("");

    private final String shortLabel;

    Sex(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public static Sex fromClickTt(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return switch (code) {
            case 1 -> MALE;
            case 2 -> FEMALE;
            default -> UNKNOWN;
        };
    }

    @Override
    public String toString() {
        return shortLabel;
    }
}
