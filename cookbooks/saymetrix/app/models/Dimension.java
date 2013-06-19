package models;

public enum Dimension {
    // WARN DO not change the integers below
    ACCOUNT(0),
    SUBSCRIBER(7),
    INCIDENT_TYPE(2),
    POSITION(3),
    FREQUENCY(4),
    CELL(6),
    OS(1),
    LOCATION_TECH(5),
    REGION(8);

    final int id;

    private Dimension(int id) {
        this.id = id;
    }

    public static Dimension fromInt(int id) {
        for(Dimension d : values()) {
            if(d.id == id) {
                return d;
            }
        }
        throw new IllegalArgumentException("Illegal Dimension (" + id + ")");
    }

    public int getId() {
        return id;
    }
}