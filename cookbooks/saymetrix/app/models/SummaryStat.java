package models;

import flexjson.JSONSerializer;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.javatuples.KeyValue;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import utils.KeyValueTransformer;
import utils.RegionStatHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "rpt_summary_stat", uniqueConstraints = @UniqueConstraint(columnNames = {"date", "type", "account_id"}))
@org.hibernate.annotations.Filter(name="manager", condition = "account_id IN (SELECT a.id FROM account a " +
"WHERE a.manager_id = :manager_id)")
public class SummaryStat extends Model {
    private static final String KEYS_INCIDENT_TYPE[] = {
            "voice.dropped-call", "voice.poor-sound", "voice.network-busy",
            "voice.no-coverage", "voice.other", "data.no-coverage",
            "data.no-comms", "data.slow", "data.dropped", "data.other",
            "other"
    };
    private static final String KEYS_POSITION[] = {
            "indoor", "outdoor", "onthemove"
    };
    private static final String KEYS_FREQUENCY[] = {
            "always", "often", "once", "seldom"
    };
    private static final String KEYS_LOCATION_TECH[] = {
            "gps", "manual", "network"
    };

    public static class TopN {
        private List<KeyValue<Object, Long>> elements = new ArrayList<KeyValue<Object, Long>>();
        public Long total;
        private boolean redact;

        public void setElements(List<KeyValue<Object, Long>> elements) {
            this.elements = elements;
            total = null;
        }

        public void add(KeyValue<Object, Long> element) {
            elements.add(element);
            total = null;
        }

        public List<KeyValue<Object, Long>> getElements() {
            return elements;
        }

        public boolean isRedacted() {
            return redact;
        }
    }

    private static final String PARAM_START = "start";
    private static final String PARAM_END = "end";
    private static final String PARAM_ACCOUNT = "account";
    private static final String PARAM_TYPE = "type";
    // Comparator to sort KeyValue by value in descending order.
    private static final Comparator REVERSE_COMPARATOR = new Comparator<KeyValue<Object, Comparable>> () {

        @Override
        public int compare(KeyValue<Object, Comparable> kv1, KeyValue<Object, Comparable> kv2) {
            return  kv2.getValue() == null ? 1 :
                    kv2.getValue().compareTo(kv1.getValue());
        }
    };
    public boolean dirty = true;
    @ManyToOne
    public Account account;
    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = "utils.PersistentDateTimeUTC")
    public DateTime date;
    /*
    * type and typeId are setup like this to allow us to use
    * Type id rather than ordinal for JPA.
    */
    @Transient
    Granularity type;
    @Column(name="type")
    public int typeId;
    public long t;  // Total incident count
    public long t1,t2,t3,t4,t5,t6,t7,t8,t9,t10,t11; // Incident type stats
    public long f1,f2,f3,f4; // Frequency stats
    public long p1,p2,p3; // Position stats
    public long l1,l2,l3; // Location tech stats

    public SummaryStat(Account a, DateTime date, Granularity g) {
        this.date = date;
        this.account = a;
        setType(g);
    }

    /**
     * Retrieve summary statistics for the specified Account over the given Interval.
     *
     * @param granularity
     * @param interval
     * @param account the Account for which to retrieve statistics. If null,
     *                aggregate statistics across all accounts will be returned.
     */
    public static List<KeyValue<DateTime, Long>> getIncidentVolumes(Granularity granularity, Interval interval,
                                                                    Account account) {
        int type = granularity.id;
        Query q;
        if(account == null) {
            q = JPA.em().createQuery("SELECT s.date, SUM(s.t) FROM SummaryStat s WHERE s.date >= :start AND " +
                    "s.date < :end AND type = :type GROUP BY s.date ORDER BY s.date");
        } else {
            q = JPA.em().createQuery("SELECT s.date, s.t FROM SummaryStat s WHERE s.date >= :start AND " +
                    "s.date < :end AND s.account = :account AND type = :type ORDER BY s.date");
            q.setParameter(PARAM_ACCOUNT, account);
        }
        q.setParameter(PARAM_START, interval.getStart());
        q.setParameter(PARAM_END, interval.getEnd());
        q.setParameter(PARAM_TYPE, type);

        return (toKeyValueList(q.getResultList()));
    }

    static List<KeyValue<DateTime, Long>> toKeyValueList(List<Object[]> stats) {
        List<KeyValue<DateTime, Long>> results = new ArrayList<KeyValue<DateTime, Long>>(stats.size());

        for(Object[] stat : stats) {
            results.add(new KeyValue<DateTime, Long>((DateTime)stat[0], (Long)stat[1]));
        }

        return results;
    }

    /**
     * Recompute metrics.
     */
    public void recompute() {
        // TODO Always using daily stats when recomputing weekly and monthly stats. Consider using weekly when recomputing monthly.
        Query q = JPA.em().createQuery("SELECT SUM(s.f1), SUM(s.f2), SUM(s.f3), SUM(s.f4), SUM(s.l1), " +
                "SUM(s.l2), SUM(s.l3), SUM(s.p1), SUM(s.p2), SUM(s.p3), SUM(s.t), SUM(s.t1), SUM(s.t2), " +
                "SUM(s.t3), SUM(s.t4), SUM(s.t5), SUM(s.t6), SUM(s.t7), SUM(s.t8), SUM(s.t9), SUM(s.t10), " + "" +
                "SUM(s.t11) FROM SummaryStat s WHERE s.account=:account AND " +
                "s.date >= :start AND s.date < :end AND s.typeId=0");
        q.setParameter(PARAM_START, date);
        DateTime end;
        // TODO Use Granularity here
        switch(typeId) {
            case 1:
                end = date.plusWeeks(1);
                break;
            case 2:
                end = date.plusMonths(1);
                break;
            default:
                Logger.error("SummaryStat.recompute invoked on daily SummaryStat");
                return;
        }
        q.setParameter(PARAM_END, end);
        q.setParameter(PARAM_ACCOUNT, account);

        Object[] x = (Object[]) q.getResultList().get(0);
        f1 = (Long)x[0];
        f2 = (Long)x[1];
        f3 = (Long)x[2];
        f4 = (Long)x[3];
        l1 = (Long)x[4];
        l2 = (Long)x[5];
        l3 = (Long)x[6];
        p1 = (Long)x[7];
        p2 = (Long)x[8];
        p3 = (Long)x[9];
        t = (Long)x[10];
        t1 = (Long)x[11];
        t2 = (Long)x[12];
        t3 = (Long)x[13];
        t4 = (Long)x[14];
        t5 = (Long)x[15];
        t6 = (Long)x[16];
        t7 = (Long)x[17];
        t8 = (Long)x[18];
        t9 = (Long)x[19];
        t10 = (Long)x[20];
        t11 = (Long)x[21];
    }

    public static void setDirty(DateTime date, Granularity granularity, Account account) {
        DateTime d = date.toDateMidnight().toDateTime();
        SummaryStat stat = SummaryStat.find("byDateAndTypeAndAccount", d, granularity.id, account).first();
        if(stat != null) {
            stat.dirty = true;
            stat.save();
        }
    }

    /**
     * Retrieve an ordered list of statistics for the specified Dimension
     * and Account of incidents reported during the specified interval.
     *
     * @param a the Account for which to retrieve statistics. If null,
     *          aggregate statistics across all accounts will be returned.
     * @param start
     * @param end
     * @param d
     * @return
     */
    public static TopN getTopN(Account a, DateTime start, DateTime end, Dimension d) {
        TopN stats;

        switch(d) {
            case ACCOUNT:
                if(a != null) {
                    stats = new TopN();
                } else {
                    stats = getTopAccounts(start, end);
                }
                break;
            case INCIDENT_TYPE:
                stats = getTopIncidentTypes(start, end, a);
                break;
            case POSITION:
                stats = getTopPositions(start, end, a);
                break;
            case FREQUENCY:
                stats = getTopFrequencies(start, end, a);
                break;
            case LOCATION_TECH:
                stats = getTopLocationTech(start, end, a);
                break;
            case REGION:
                stats = RegionStatHelper.getRegionStats(start, end, a);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Dimension: " + d.toString());
        }

        return stats;
    }

    /**
     * Retrieve an ordered list of the Accounts reporting the most
     * incidents during the specified interval.
     *
     * @param start
     * @param end
     * @return
     */
    static TopN getTopAccounts(DateTime start, DateTime end) {
        if(end == null) {
            end = start;
        }
        Query q = JPA.em().createQuery("SELECT s.account.name, s.account.key, SUM(s.t) FROM SummaryStat s " +
                "WHERE s.date BETWEEN :start AND :end AND s.typeId=0 GROUP BY s.account " +
                "HAVING SUM(s.t) > 0 ORDER BY SUM(s.t) DESC");
        q.setParameter(PARAM_START, start);
        q.setParameter(PARAM_END, end);
        q.setMaxResults(10);

        SummaryStat.TopN results = new SummaryStat.TopN();
        for(Object[] result : (List<Object[]>)q.getResultList()) {
            results.add(new KeyValue<Object, Long>(result[0] + " ( " + result[1] + " )", (Long)result[2]));
        }

        return results;
    }

    /**
     * Retrieve an ordered list of the most popular incident types
     * reported during the specified interval.
     *
     * @param start
     * @param end
     * @param account the Account for which to retrieve statistics. If null,
     *                aggregate statistics across all accounts will be returned.
     * @return
     */
    static TopN getTopIncidentTypes(DateTime start, DateTime end, Account account) {
        Query q;
        if(end == null) {
            end = start;
        }
        if(account == null) {
            q = JPA.em().createQuery("SELECT SUM(s.t1),SUM(s.t2),SUM(s.t3),SUM(s.t4),SUM(s.t5),SUM(s.t6),SUM(s.t7),SUM(s.t8),SUM(s.t9),SUM(s.t10),SUM(s.t11) FROM SummaryStat s where s.date BETWEEN :start AND :end AND type=0");
        } else {
            q = JPA.em().createQuery("SELECT SUM(s.t1),SUM(s.t2),SUM(s.t3),SUM(s.t4),SUM(s.t5),SUM(s.t6),SUM(s.t7),SUM(s.t8),SUM(s.t9),SUM(s.t10),SUM(s.t11) FROM SummaryStat s where s.date BETWEEN :start AND :end AND type=0 AND s.account = :account");
            q.setParameter(PARAM_ACCOUNT, account);
        }
        q.setParameter(PARAM_START, start);
        q.setParameter(PARAM_END, end);

        Object[] results = (Object[]) q.getSingleResult();

        List<KeyValue<Object, Long>> stats = new ArrayList<KeyValue<Object, Long>>(KEYS_INCIDENT_TYPE.length);
        for(int i=0; i < KEYS_INCIDENT_TYPE.length; i++) {
            stats.add(new KeyValue(play.i18n.Messages.get(KEYS_INCIDENT_TYPE[i]), results[i]));
        }

        Collections.sort(stats, REVERSE_COMPARATOR);

        TopN result = new TopN();
        result.setElements(stats);

        return result;
    }

    /**
     * Retrieve an ordered list of the most popular positions incidents
     * were reported from during the specified interval.
     *
     * @param start
     * @param end
     * @param account the Account for which to retrieve statistics. If null,
     *                aggregate statistics across all accounts will be returned.
     * @return
     */
    static TopN getTopPositions(DateTime start, DateTime end, Account account) {
            Query q;
        if(end == null) {
            end = start;
        }
        if(account == null) {
            q = JPA.em().createQuery("SELECT SUM(s.p1),SUM(s.p2),SUM(s.p3) FROM SummaryStat s where s.date BETWEEN :start AND :end AND s.typeId=0");
        } else {
            q = JPA.em().createQuery("SELECT SUM(s.p1),SUM(s.p2),SUM(s.p3) FROM SummaryStat s where s.date BETWEEN :start AND :end AND s.account = :account AND s.typeId=0");
            q.setParameter(PARAM_ACCOUNT, account);
        }
        q.setParameter(PARAM_START, start);
        q.setParameter(PARAM_END, end);

        Object[] results = (Object[]) q.getSingleResult();

        List<KeyValue<Object, Long>> stats = new ArrayList<KeyValue<Object, Long>>(KEYS_POSITION.length);
        for(int i=0; i < KEYS_POSITION.length; i++) {
            stats.add(new KeyValue(play.i18n.Messages.get(KEYS_POSITION[i]), results[i]));
        }

        Collections.sort(stats, REVERSE_COMPARATOR);

        TopN result = new TopN();
        result.setElements(stats);

        return result;
    }

    /**
     * Retrieve an ordered list of the most popular incident frequencies
     * reported during the specified interval.
     *
     * @param start
     * @param end
     * @param account the Account for which to retrieve statistics. If null,
     *                aggregate statistics across all accounts will be returned.
     * @return
     */
    static TopN getTopFrequencies(DateTime start, DateTime end, Account account) {
            Query q;
        if(end == null) {
            end = start;
        }
        if(account == null) {
            q = JPA.em().createQuery("SELECT SUM(s.f1),SUM(s.f2),SUM(s.f3),SUM(s.f4) FROM SummaryStat s where s.date BETWEEN :start AND :end AND s.typeId=0");
        } else {
            q = JPA.em().createQuery("SELECT SUM(s.f1),SUM(s.f2),SUM(s.f3),SUM(s.f4) FROM SummaryStat s where s.date BETWEEN :start AND :end AND s.account = :account AND s.typeId=0");
            q.setParameter(PARAM_ACCOUNT, account);
        }
        q.setParameter(PARAM_START, start);
        q.setParameter(PARAM_END, end);

        Object[] results = (Object[]) q.getSingleResult();

        List<KeyValue<Object, Long>> stats = new ArrayList<KeyValue<Object, Long>>(KEYS_FREQUENCY.length);
        for(int i=0; i < KEYS_FREQUENCY.length; i++) {
            stats.add(new KeyValue(play.i18n.Messages.get(KEYS_FREQUENCY[i]), results[i]));
        }

        Collections.sort(stats, REVERSE_COMPARATOR);

        TopN result = new TopN();
        result.setElements(stats);

        return result;
    }

    /**
     * Retrieve an ordered list of the most popular location technologies
     * used for incidents reported during the specified interval.
     *
     * @param start
     * @param end
     * @param account the Account for which to retrieve statistics. If null,
     *                aggregate statistics across all accounts will be returned.
     * @return
     */
    static TopN getTopLocationTech(DateTime start, DateTime end, Account account) {
        Query q;
        if(end == null) {
            end = start;
        }
        if(account == null) {
            q = JPA.em().createQuery("SELECT SUM(s.l1),SUM(s.l2),SUM(s.l3) FROM SummaryStat s where s.date BETWEEN :start AND :end AND s.typeId=0");
        } else {
            q = JPA.em().createQuery("SELECT SUM(s.l1),SUM(s.l2),SUM(s.l3) FROM SummaryStat s where s.date BETWEEN :start AND :end AND s.account = :account AND s.typeId=0");
            q.setParameter(PARAM_ACCOUNT, account);
        }
        q.setParameter(PARAM_START, start);
        q.setParameter(PARAM_END, end);

        Object[] results = (Object[]) q.getSingleResult();

        List<KeyValue<Object, Long>> stats = new ArrayList<KeyValue<Object, Long>>(KEYS_LOCATION_TECH.length);
        for(int i=0; i < KEYS_LOCATION_TECH.length; i++) {
            stats.add(new KeyValue(play.i18n.Messages.get(KEYS_LOCATION_TECH[i]), results[i]));
        }

        Collections.sort(stats, REVERSE_COMPARATOR);

        TopN result = new TopN();
        result.setElements(stats);

        return result;
    }

    public void setType(Granularity g) {
        // See note at type and typeId declaration
        this.type = g;
        this.typeId = type.id;
    }

    public void setTypeId(int id) {
        // See note at type and typeId declaration
        this.typeId = id;
        for(Granularity g : Granularity.values()) {
            if(g.id == id) {
                type = g;
            }
        }
    }

    public String toString() {
        return new ToStringBuilder(this).append(date).append(account.name).toString();
    }

    public static JSONSerializer createSerializer() {
        JSONSerializer ser = new JSONSerializer().transform(new KeyValueTransformer(), KeyValue.class);
//        ser.include("value").transform(new FieldNameTransformer(), "value");

        return ser.exclude("*").prettyPrint(Play.mode == Play.Mode.DEV);
    }
}
