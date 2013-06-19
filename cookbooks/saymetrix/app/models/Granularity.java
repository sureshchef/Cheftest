package models;

/**
 * Granularity of a reporting metric.
 */
public enum Granularity {
    // WARN Do not change the integers below
    DAILY(0),
    WEEKLY(1),
    MONTHLY(2);

    final int id;

    private Granularity(int id) {
        this.id = id;
    }

    public static Granularity fromInt(int id) {
        for(Granularity g : values()) {
            if(g.id == id) {
                return g;
            }
        }
        throw new IllegalArgumentException("Illegal Granularity (" + id + ")");
    }

    public int getId() {
        return id;
    }
}