package models;

import com.google.gson.annotations.Expose;
import models.attributes.IncidentModelAttributes;
import models.attributes.MobileSubscriberModelAttributes;
import models.enumerations.Frequency;
import models.enumerations.LocationTech;
import models.enumerations.Position;
import models.valueobject.FilterValueObject;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import play.data.validation.Required;
import play.db.jpa.GenericModel;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Entity
@Table(name = "filter")
public class Filter extends GenericModel {

    @Id
    @GeneratedValue
    @Expose
    public Long id;
    @Required(message = "This is a required field")
    @Column(nullable = false)
    @Expose
    public String name;
    @Expose
    @ManyToMany
    public Collection<IncidentType> incidentTypes;
    @Expose
    @Column(name="windowstart", nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    public DateTime incidentPeriodStart;
    @Expose
    @Column(name="windowend")
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    public DateTime incidentPeriodEnd;
    @Transient
    public Interval incidentPeriod;
    @Expose
    @Columns(columns = {
        @Column(name = "blackoutstart"),
        @Column(name = "blackoutend")})
    @Type(type = "org.joda.time.contrib.hibernate.PersistentInterval")
    public Interval blockOutPeriod;
    @Expose
    @ElementCollection
    public Collection<LocationTech> locationTech;
    @Expose
    @ElementCollection
    public Collection<Position> position;
    @Expose
    @ElementCollection
    public Collection<Frequency> frequency;
    @Expose
    public String msisdn;
    @Expose
    public String cellID;
    @ManyToMany
    public Collection<WebUser> users;
    @OneToMany
    @Expose
    public Collection<Account> accounts;
    
    public Interval getIncidentPeriod() {
        if (incidentPeriodStart == null && incidentPeriodEnd == null) {
            return null;
        } else if (incidentPeriodStart != null && incidentPeriodEnd == null) {
            incidentPeriodEnd = new DateTime();
        }
        incidentPeriod = new Interval(incidentPeriodStart, incidentPeriodEnd.toDateMidnight().toDateTime());
        return incidentPeriod;
    }

    public static CriteriaQuery<Incident> generateCriteria(Filter filter, String orderField, String orderDirection, String bounds) {
        CriteriaBuilder criteriaBuilder = em().getCriteriaBuilder();
        CriteriaQuery<Incident> criteriaQuery = criteriaBuilder.createQuery(Incident.class);
        Root<Incident> incidentRoot = criteriaQuery.from(Incident.class);
        Predicate criteria = criteriaBuilder.conjunction();

        /**
         * TODO need to rerun everything when no field is filled in
         *
         */
        if (filter.position != null && !filter.position.isEmpty()) {
            criteria = criteriaBuilder.and(criteria, incidentRoot.get(IncidentModelAttributes.POSITION.asNamedInClass).in(filter.position));
        }
        if (filter.msisdn != null && !filter.msisdn.equalsIgnoreCase("")) {
            criteria = criteriaBuilder.and(criteria, criteriaBuilder.like(incidentRoot.get(IncidentModelAttributes
                    .SUBSCRIBER.asNamedInClass).<String>get(MobileSubscriberModelAttributes.MSISDN.asNamedInClass),
                    "%" + filter.getNormalizedMsisdn()));
        }
        if (filter.cellID != null && !filter.cellID.isEmpty()) {
            criteria = criteriaBuilder.and(criteria, criteriaBuilder.equal(incidentRoot.get(IncidentModelAttributes
                    .CELL_ID.asNamedInClass), filter.cellID));
        }
        if (filter.frequency != null && !filter.frequency.isEmpty()) {
            criteria = criteriaBuilder.and(criteria, incidentRoot.get(IncidentModelAttributes.FREQUENCY.asNamedInClass).in(filter.frequency));
        }
        if (filter.locationTech != null && !filter.locationTech.isEmpty()) {
            criteria = criteriaBuilder.and(criteria, incidentRoot.get(IncidentModelAttributes.LOCATION_TECH.asNamedInClass).in(filter.locationTech));
        }
        if (filter.incidentTypes != null && !filter.incidentTypes.isEmpty()) {
            criteria = criteriaBuilder.and(criteria, incidentRoot.get(IncidentModelAttributes.INCIDENT_TYPE.asNamedInClass).in(filter.incidentTypes));
        }
        if (filter.accounts != null && !filter.accounts.isEmpty()) {
            criteria = criteriaBuilder.and(criteria, incidentRoot.get(IncidentModelAttributes.SUBSCRIBER.asNamedInClass).get(MobileSubscriberModelAttributes.ACCOUNT.asNamedInClass).in(filter.accounts));
        }
        if (filter.incidentPeriod != null) {
            criteria = criteriaBuilder.and(criteria, criteriaBuilder.between(incidentRoot.<DateTime>get(IncidentModelAttributes.DATE.asNamedInClass), filter.incidentPeriod.getStart(), filter.incidentPeriod.getEnd()));
        }
        if (filter.blockOutPeriod != null) {
            criteria = criteriaBuilder.and(criteria, criteriaBuilder.not(criteriaBuilder.between(incidentRoot.<DateTime>get(IncidentModelAttributes.DATE.asNamedInClass), filter.blockOutPeriod.getStart(), filter.blockOutPeriod.getEnd())));
        }
        
        if (!bounds.isEmpty()) {
            String[] mapBounds = bounds.split(",");
            
            criteria = criteriaBuilder.and(criteria, criteriaBuilder.between(incidentRoot.<Double>get("latitude"), Double.parseDouble(mapBounds[0]), Double.parseDouble(mapBounds[2])));
            criteria = criteriaBuilder.and(criteria, criteriaBuilder.between(incidentRoot.<Double>get("longitude"), Double.parseDouble(mapBounds[1]), Double.parseDouble(mapBounds[3])));
        }
        
        criteriaQuery.select(incidentRoot).where(criteria);
        
        if ("DESC".matches(orderDirection)) {
            if ("accountName".equals(orderField)) {
                criteriaQuery.orderBy(criteriaBuilder.desc(incidentRoot.get("subscriber").get("account").get("name")), criteriaBuilder.asc(incidentRoot.get("date")));
            } else if ("subscriberName".equals(orderField)) {
                criteriaQuery.orderBy(criteriaBuilder.desc(incidentRoot.get("subscriber").get("lastname")), criteriaBuilder.desc(incidentRoot.get("subscriber").get("firstname")), criteriaBuilder.asc(incidentRoot.get("date")));
            } else if ("incidentName".equals(orderField)) {
                criteriaQuery.orderBy(criteriaBuilder.desc(incidentRoot.get("incidentType").get("incidentGroup").get("name")), criteriaBuilder.desc(incidentRoot.get("incidentType").get("name")), criteriaBuilder.asc(incidentRoot.get("date")));
            } else {
                criteriaQuery.orderBy(criteriaBuilder.desc(incidentRoot.get(orderField)));
            }
        } else if ("ASC".matches(orderDirection)) {
            if ("accountName".equals(orderField)) {
                criteriaQuery.orderBy(criteriaBuilder.asc(incidentRoot.get("subscriber").get("account").get("name")), criteriaBuilder.asc(incidentRoot.get("date")));
            } else if ("subscriberName".equals(orderField)) {
                criteriaQuery.orderBy(criteriaBuilder.asc(incidentRoot.get("subscriber").get("lastname")), criteriaBuilder.asc(incidentRoot.get("subscriber").get("firstname")), criteriaBuilder.asc(incidentRoot.get("date")));
            } else if ("incidentName".equals(orderField)) {
                criteriaQuery.orderBy(criteriaBuilder.asc(incidentRoot.get("incidentType").get("incidentGroup").get("name")), criteriaBuilder.asc(incidentRoot.get("incidentType").get("name")), criteriaBuilder.asc(incidentRoot.get("date")));
            } else {
                criteriaQuery.orderBy(criteriaBuilder.asc(incidentRoot.get(orderField)));
            }
        }

        return criteriaQuery;
    }

    public void overWriteValues(Filter newFilterValues) {
        blockOutPeriod = newFilterValues.blockOutPeriod;
        cellID = newFilterValues.cellID;
        frequency = newFilterValues.frequency;
        incidentPeriod = newFilterValues.incidentPeriod;
        incidentTypes = newFilterValues.incidentTypes;
        locationTech = newFilterValues.locationTech;
        msisdn = newFilterValues.msisdn;
        name = newFilterValues.name;
        position = newFilterValues.position;
        accounts = newFilterValues.accounts;
    }

    public FilterValueObject getValueObject() {
        return new FilterValueObject(id, name);
    }

    /**
     * Return the msisdn removing any leading '+', '0' or '00' and all
     * whitespaces and dashes.
     *
     * @return
     */
     String getNormalizedMsisdn() {
        return msisdn.replaceAll("^\\s*(\\+|00?)|\\s|-", "");
    }
}
