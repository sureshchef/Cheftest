package models;

import com.google.gson.annotations.Expose;
import java.util.List;
import javax.persistence.*;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.criteria.*;
import org.joda.time.Interval;

@Entity
@Table(name = "site")
public class Site extends Model {

    @Required()
    @Column(name = "skey", unique = true, nullable = false)
    @Expose
    public String key;
    @Required
    @Expose
    public Double latitude;
    @Required
    @Expose
    public Double longitude;
    @Expose
    public String street_address;
    @Expose
    public String technologies;

    public static Site findByKey(String key) {
        return Site.find("byKey", key).first();
    }
    
    public static List<Site> findAllFor(Interval period, String bounds) {
        CriteriaBuilder criteriaBuilder = em().getCriteriaBuilder();
        CriteriaQuery<Site> criteriaQuery = criteriaBuilder.createQuery(Site.class);
        Root<Site> siteRoot = criteriaQuery.from(Site.class);
        Predicate criteria = criteriaBuilder.conjunction();

        String[] mapBounds = bounds.split(",");

        criteria = criteriaBuilder.and(criteria, criteriaBuilder.between(siteRoot.<Double>get("latitude"), Double.parseDouble(mapBounds[0]), Double.parseDouble(mapBounds[2])));
        criteria = criteriaBuilder.and(criteria, criteriaBuilder.between(siteRoot.<Double>get("longitude"), Double.parseDouble(mapBounds[1]), Double.parseDouble(mapBounds[3])));
        
        criteriaQuery.select(siteRoot).where(criteria);
        
        TypedQuery<Site> query;
        query = em().createQuery(criteriaQuery);
        return query.getResultList();
    }    
}