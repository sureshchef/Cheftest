package models;

import com.google.gson.annotations.Expose;
import play.Play;
import play.cache.Cache;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "incidenttype")
public class IncidentType extends Model {
    private static final String CACHE_PREFIX_INCIDENT = Play.id + "-itype-";
    private static final String CACHE_PREFIX_INCIDENTS = Play.id + "-itypes";

    @Required(message = "This is a required field")
    @Column(name = "akey", unique = true, nullable = false)
    @Expose
    public String key;
    @Required
    @Expose
    public String name;
    @ManyToOne
    @Expose
    public IncidentGroup incidentGroup;

    public static IncidentType findByKey(String key) {
        IncidentType type = Cache.get(CACHE_PREFIX_INCIDENT + key, IncidentType.class);
        if(type == null) {
            type = IncidentType.find("byKey", key).first();
            Cache.set(CACHE_PREFIX_INCIDENT + key, type, "30mn");
        }
        return type;
    }

    /**
     * Retrieve the list of defined IncidentTypes. Use this in preference
     * to findAll to leverage caching.
     *
     * @return
     */
    public static List<IncidentType> findAll(boolean useCache) {
        List<IncidentType> types = null;

        if(useCache) {
            types = Cache.get(CACHE_PREFIX_INCIDENTS, List.class);
            if(types == null) {
                types = IncidentType.all().fetch();
                Cache.set(CACHE_PREFIX_INCIDENTS, types, "30mn");
            }
        } else {
            types = findAll();
        }
        return types;
    }
}