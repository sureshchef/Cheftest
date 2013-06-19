package models;

import play.db.jpa.Model;

import javax.persistence.*;

/**
 * Additional Incident details that are not frequently needed.
 */
@Entity
@Table(name = "incident_details")
public class IncidentDetails extends Model {
    @OneToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "incident_id", nullable = false)
    public Incident incident;
    @Lob
    @Column(name="address_json")
    public String addressJson; // Raw data returned by reverse geocoder

    public IncidentDetails(Incident incident, String addressJson) {
        this.incident = incident;
        this.addressJson = addressJson;
    }
}
