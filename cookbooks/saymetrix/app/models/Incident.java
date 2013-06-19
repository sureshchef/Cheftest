package models;

import com.google.gson.annotations.Expose;
import controllers.Security;
import models.enumerations.Frequency;
import models.enumerations.LocationTech;
import models.enumerations.Position;
import org.hibernate.annotations.Type;
import org.javatuples.KeyValue;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import play.data.validation.Required;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import play.i18n.Messages;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

@Entity
@Table(name = "incident")
// TODO This filter looks like it could do with some profiling
@org.hibernate.annotations.Filter(name="manager", condition="subscriber_id IN (SELECT m.id FROM mobilesub " +
        "AS m JOIN account AS a ON a.id=m.account_id JOIN webuser AS w ON w.id=a.manager_id " +
        "WHERE w.id = :manager_id)")
public class Incident extends GenericModel {
    @Id
    @GeneratedValue
    @Expose
    public Long id;
    @ManyToOne
    @Expose
    public MobileSubscriber subscriber;
    @Required
    @Expose
    @Column(nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    public DateTime date;
    @Expose
    public String imei;
    @Required
    @Expose
    public double latitude;
    @Required
    @Expose
    public double longitude;
    @Required
    @Expose
    @ManyToOne
    public IncidentType incidentType;
    @Lob
    @Expose
    public String comment;
    @Expose
    public String phoneType;
    @Expose
    @Column(name="phone_os")
    public String phoneOs;
    @Expose
    @Enumerated(EnumType.STRING)
    public Frequency frequency;
    @Expose
    @Enumerated(EnumType.STRING)
    public LocationTech locationTech;
    @Expose
    @Enumerated(EnumType.STRING)
    public Position position;
    @Expose
    public String cellId;
    @Expose
    public String imsi;
    public long lac;
    public int accuracy;
    @Expose
    @JoinColumn(name="webuser_id")
    @ManyToOne(optional = true)
    public WebUser reporter;
    @Required
    @Expose
    public int source;
    // An address length of zero indicates the we haven't tried to reverse geocode it yet.
    @Expose
    public String address;
    @Expose
    @ManyToOne
    @JoinColumn(name = "network_event_id")
    public NetworkEvent networkEvent;

    public static List<Incident> find(Filter filter) {
        return find(filter, "date", "DESC", "", 0, 0);
    }

    public static List<Incident> find(Filter filter, String orderField, String orderDirection, String bounds) {
        return find(filter, orderField, orderDirection, bounds, 0, 0);
    }

    public static List<Incident> find(Filter filter, String orderField, String orderDirection, int start, int limit) {
        return find(filter, orderField, orderDirection, "", start, limit);
    }

    public static List<Incident> find(Filter filter, String orderField, String orderDirection, String bounds, int start, int limit) {
        CriteriaQuery<Incident> criteria = Filter.generateCriteria(filter, orderField, orderDirection, bounds);
        TypedQuery<Incident> query;
        if (limit != 0) {
            query = em().createQuery(criteria).setFirstResult(start).setMaxResults(limit);
        } else {
            query = em().createQuery(criteria);
        }
        List<Incident> incidents = query.getResultList();

        // Don't reveal subscriber names when running in redaction mode
        if(Security.isRedactEnabled()) {
            for(Incident i : incidents) {
                i.subscriber.lastname = "******";
                i.subscriber.msisdn = i.subscriber.msisdn.substring(0, 5) + "*******";
            }
        }
        return incidents;
    }

    /**
     * Retrieve the top-n most popular device operating systems
     * incidents were reported from during the specified interval.
     *
     * @param account the Account for which to retrieve statistics. If null,
     *                aggregate statistics across all accounts will be returned.
     * @param interval
     * @return
     */
    public static SummaryStat.TopN findTopNMobiles(Account account, Interval interval, int numResultsToReturn) {
        Query query;
        if (account == null) {
            query = JPA.em().createQuery(
                    "SELECT i.phoneType,COUNT(i.phoneType) FROM Incident i where i.date BETWEEN :startdate AND :enddate GROUP BY i.phoneType HAVING COUNT(i.phoneType) > 0 ORDER BY COUNT(i.phoneType) DESC");
        } else {
            query = JPA.em().createQuery(
                    "SELECT i.phoneType,COUNT(i.phoneType) FROM Incident i where i.date BETWEEN :startdate AND :enddate  AND i.subscriber.account = :account GROUP BY i.phoneType HAVING COUNT(i.phoneType) > 0 ORDER BY COUNT(i.phoneType) DESC");
            query.setParameter("account", account);
        }
        query.setParameter("startdate", interval.getStart().withTimeAtStartOfDay());
        query.setParameter("enddate", interval.getEnd().withTime(23, 59, 59, 999));
        query.setMaxResults(numResultsToReturn);

        SummaryStat.TopN results = new SummaryStat.TopN();
        for(Object[] result : (List<Object[]>)query.getResultList()) {
            results.add(new KeyValue<Object, Long>(result[0], (Long)result[1]));
        }

        return results;

    }

    /**
     * Retrieve the top-n list of the mobile cells incidents were most
     * frequently reported from during the specified interval.
     *
     * @param account the Account for which to retrieve statistics. If null,
     *                aggregate statistics across all accounts will be returned.
     * @param interval
     * @return
     */
    public static SummaryStat.TopN findTopNCells(Account account, Interval interval, int numResultsToReturn) {
        Query query;
        if (account == null) {
            //SELECT cellid, COUNT(cellid) from incident group by cellid
            query = JPA.em().createQuery(
                    "SELECT i.cellId,COUNT(i.cellId) FROM Incident i where i.date BETWEEN :startdate AND :enddate GROUP BY i.cellId HAVING COUNT(i.cellId) > 0 ORDER BY COUNT(i.cellId) DESC");
        } else {
            query = JPA.em().createQuery(
                    "SELECT i.cellId,COUNT(i.cellId) FROM Incident i where i.date BETWEEN :startdate AND :enddate  AND i.subscriber.account = :account GROUP BY i.cellId HAVING COUNT(i.cellId) > 0 ORDER BY COUNT(i.cellId) DESC");
            query.setParameter("account", account);
        }
        query.setParameter("startdate", interval.getStart().withTimeAtStartOfDay());
        query.setParameter("enddate", interval.getEnd().withTime(23, 59, 59, 999));
        query.setMaxResults(numResultsToReturn);

        SummaryStat.TopN results = new SummaryStat.TopN();
        for(Object[] result : (List<Object[]>)query.getResultList()) {
            String key = (String)result[0];
            if("0".equals(key)) {
                key = Messages.get("na");
            }
            results.add(new KeyValue<Object, Long>(key, (Long)result[1]));
        }

        return results;
    }

    /**
     * Retrieve the top-n list of the subscribers reporting the most
     * incidents during the specified interval.
     *
     * @param account the Account for which to retrieve statistics. If null,
     *                aggregate statistics across all accounts will be returned.
     * @param interval
     * @return
     */
    public static SummaryStat.TopN findTopNSubscribers(Account account, Interval interval, int numResultsToReturn, boolean redact) {
      Query q;

        if (account == null) {
            q = JPA.em().createQuery(
                    "SELECT i.subscriber.firstname, i.subscriber.lastname,COUNT(i.subscriber) FROM Incident i " +
                            "WHERE i.date BETWEEN :startdate AND :enddate GROUP BY i.subscriber HAVING " +
                            "COUNT(i.subscriber) > 0 ORDER BY COUNT(i.subscriber) DESC");
        } else {
            q = JPA.em().createQuery(
                    "SELECT i.subscriber.firstname, i.subscriber.lastname,COUNT(i.subscriber) FROM Incident i WHERE " +
                            "i.date BETWEEN :startdate AND :enddate  AND i.subscriber.account = :account " +
                            "GROUP BY i.subscriber HAVING COUNT(i.subscriber) > 0 ORDER BY COUNT(i.subscriber) DESC");
            q.setParameter("account", account);
        }
        q.setParameter("startdate", interval.getStart().withTimeAtStartOfDay());
        q.setParameter("enddate", interval.getEnd());
        q.setMaxResults(numResultsToReturn);

        SummaryStat.TopN results = new SummaryStat.TopN();

        for(Object[] result : (List<Object[]>)q.getResultList()) {
            String name = redact ? result[0] + " ******" : result[0] + " " + result[1];
            results.add(new KeyValue<Object, Long>(name, (Long)result[2]));
        }

        return results;
    }

    @Override
    public boolean validateAndCreate() {
        boolean valid = super.validateAndCreate();
        if(valid) {
            DateMidnight today = DateMidnight.now();
            DateTime weekStart = today.withDayOfWeek(1).toDateTime();
            DateTime monthStart = today.withDayOfMonth(1).toDateTime();

            if(date.isBefore(today)) {
                SummaryStat.setDirty(date, Granularity.DAILY, subscriber.account);
            }
            if(date.isBefore(weekStart)) {
                SummaryStat.setDirty(weekStart.minusWeeks(1), Granularity.WEEKLY, subscriber.account);
            }
            if(date.isBefore(monthStart)) {
                SummaryStat.setDirty(monthStart.minusMonths(1), Granularity.MONTHLY, subscriber.account);
            }
        }

        return valid;
    }
}