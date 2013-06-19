package models;

import com.google.gson.annotations.Expose;
import flexjson.JSONSerializer;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import play.Play;
import play.data.validation.Required;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;

@Entity
@Table(name = "network_event")
public class NetworkEvent extends GenericModel {
    @Id
    @GeneratedValue
    @Expose
    public Long id;
    @Expose
    @Required()
    @Columns(columns = {
        @Column(name = "start"),
        @Column(name = "end")})
    @Type(type = "org.joda.time.contrib.hibernate.PersistentInterval")
    public Interval eventPeriod;
    @Expose
    @Required()
    public String subject;
    @Lob
    @Expose
    @Required()
    public String description;
    @Required
    @Expose
    @JoinColumn(name = "event_type_id")
    @ManyToOne
    public NetworkEventType eventType;
    @Expose
    @Required()
    @ManyToMany
    public List<Site> sites = new ArrayList<Site>();
    @Expose
    @ManyToOne
    public WebUser creator;
    @Expose
    @Required
    @Column(name = "created")
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    public DateTime createdOn;
    @Expose
    @Transient
    public int numOfIncidents = 0;

    @Override
    public String toString() {
        return String.format("eventPeriod=%s, subject=%s, description=%s, eventType=%s, sites=%s, creator=%s, createdOn=%s", eventPeriod, subject, description, eventType, sites, creator, createdOn);
    }

    public static JSONSerializer createSerializer() {
        return new JSONSerializer().include(
            "sEcho", "iTotalRecords", "iTotalDisplayRecords", "aaData.subject",
            "aaData.subject", "aaData.description", "aaData.eventType.key",
            "aaData.eventType.name", "subject", "description", "id", "aaData.id",
            "eventType.key", "eventType.name", "eventPeriod.startMillis", "eventPeriod.endMillis", "aaData.eventPeriod.startMillis", "aaData.eventPeriod.endMillis",
            "sites", "sites.key", "sites.latitude", "sites.longitude",
            "aaData.creator", "aaData.creator.firstname", "aaData.creator.lastname", "aaData.createdOn", "aaData.createdOn.millis",
            "creator", "creator.firstname", "creator.lastname", "createdOn", "createdOn.millis", "numOfIncidents"
        ).exclude("*").prettyPrint(Play.mode == Play.Mode.DEV);
    }
    
    public static List<NetworkEvent> findAll(String sort) {
        String query = "SELECT ne, count(i) as amount FROM Incident AS i RIGHT JOIN i.networkEvent AS ne GROUP BY ne.id ORDER BY";
        if ("incidents".equalsIgnoreCase(sort)) {
            query = query.concat(" amount DESC,");
        }
        query = query.concat(" ne.eventPeriod.start DESC");
        
        StringBuilder qString = new StringBuilder(query);
        Query q = JPA.em().createQuery(qString.toString());
        
        List<NetworkEvent> finalList = new ArrayList();
        List<Object[]> outerList = q.getResultList();
        for (Object[] innerList : outerList) {
            NetworkEvent ne = (NetworkEvent) innerList[0];
            ne.numOfIncidents = new Long(innerList[1].toString()).intValue();
            finalList.add(ne);
        }
        
        return finalList;
    }
    
    public static List<NetworkEvent> findByDate(DateTime date, String sort) {
        String query = "SELECT ne, count(i) as amount FROM Incident AS i RIGHT JOIN i.networkEvent AS ne WHERE :start >= ne.eventPeriod.start AND :end <= ne.eventPeriod.end GROUP BY ne.id ORDER BY";
        if ("incidents".equalsIgnoreCase(sort)) {
            query = query.concat(" amount DESC,");
        }
        query = query.concat(" ne.eventPeriod.start DESC");
        
        StringBuilder qString = new StringBuilder(query);
        Query q = JPA.em().createQuery(qString.toString());
        q.setParameter("start", date.toDate());
        q.setParameter("end", date.toDate());
        
        List<NetworkEvent> finalList = new ArrayList();
        List<Object[]> outerList = q.getResultList();
        for (Object[] innerList : outerList) {
            NetworkEvent ne = (NetworkEvent) innerList[0];
            ne.numOfIncidents = new Long(innerList[1].toString()).intValue();
            finalList.add(ne);
        }
        
        return finalList;
    }

    public void setNumOfIncidents() {
        String query = "SELECT COUNT(i) FROM Incident i WHERE i.networkEvent.id = :neId";
        StringBuilder qString = new StringBuilder(query);
        Query q = JPA.em().createQuery(qString.toString());
        q.setParameter("neId", id);
        Long aLong = (Long) q.getSingleResult();
        numOfIncidents = aLong.intValue();
    }
}
