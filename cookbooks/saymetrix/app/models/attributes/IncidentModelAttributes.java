package models.attributes;

public enum IncidentModelAttributes {

    POSITION("position"), SUBSCRIBER("subscriber"), CELL_ID("cellId"), FREQUENCY("frequency"), LOCATION_TECH("locationTech"), INCIDENT_TYPE("incidentType"), DATE("date");
    public String asNamedInClass;

    private IncidentModelAttributes(String asNamedInClass) {
        this.asNamedInClass = asNamedInClass;
    }
}
