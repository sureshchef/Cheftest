package models;

import com.google.gson.annotations.Expose;
import play.Play;
import play.cache.Cache;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "network_event_type")
public class NetworkEventType extends Model {
    private static final String CACHE_PREFIX_NETWORKEVENTTYPE = Play.id + "-netype-";
    private static final String CACHE_PREFIX_NETWORKEVENTTYPES = Play.id + "-netypes";

    @Required(message = "This is a required field")
    @Column(name = "tkey", unique = true, nullable = false)
    @Expose
    public String key;
    @Required
    @Expose
    public String name;

    public static NetworkEventType findByKey(String key) {
        NetworkEventType type = Cache.get(CACHE_PREFIX_NETWORKEVENTTYPE + key, NetworkEventType.class);
        if(type == null) {
            type = NetworkEventType.find("byKey", key).first();
            Cache.set(CACHE_PREFIX_NETWORKEVENTTYPE + key, type, "30mn");
        }
        return type;
    }

    /**
     * Retrieve the list of defined IncidentTypes. Use this in preference
     * to findAll to leverage caching.
     *
     * @return
     */
    public static List<NetworkEventType> findAll(boolean useCache) {
        List<NetworkEventType> types = null;

        if(useCache) {
            types = Cache.get(CACHE_PREFIX_NETWORKEVENTTYPES, List.class);
            if(types == null) {
                types = NetworkEventType.all().fetch();
                Cache.set(CACHE_PREFIX_NETWORKEVENTTYPES, types, "30mn");
            }
        } else {
            types = findAll();
        }
        return types;
    }
}