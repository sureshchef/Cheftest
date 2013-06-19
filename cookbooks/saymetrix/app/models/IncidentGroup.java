package models;

import com.google.gson.annotations.Expose;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Collection;

@Entity
@Table(name = "incidentgroup")
public class IncidentGroup extends Model {

    @Required
    @Expose
    public String name;
    @OneToMany(mappedBy = "incidentGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    public Collection<IncidentType> members;

    public IncidentGroup(String name) {
        this.name = name;
    }
}
