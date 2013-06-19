package jobs;

import models.*;
import models.enumerations.Frequency;
import models.enumerations.LocationTech;
import models.enumerations.Position;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import play.Logger;
import play.db.jpa.JPA;
import play.jobs.On;

import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@On("0 0 0 * * ?")
public class SummarizerJob extends MultiNodeJob {

    @Override
    public void execute() {
        List<Account> accounts = Account.findAll();
        try {
            for (Account a : accounts) {
                // If necessary create SummaryStat slots
                createSummaryStatSlots(a);
                // Always update the SummaryStats to handle late arrival of incidents
                updateSummaryStats(a, Granularity.DAILY);
                updateSummaryStats(a, Granularity.WEEKLY);
                updateSummaryStats(a, Granularity.MONTHLY);
            }
        } catch (PersistenceException e) {
            Logger.info("%s aborted - already running on another node", this.getClass().getSimpleName());
        }
    }

    /**
     * Create empty daily, weekly and monthly SummaryStat records for given account up to, but not including, today.
     *
     * @param a
     */
    private void createSummaryStatSlots(Account a) {
        DateTime date;
        SummaryStat stat = SummaryStat.find("SELECT s FROM SummaryStat s WHERE s.account=? " +
                "ORDER BY s.date DESC", a).first();

        if(stat == null) {
            // No SummaryStats found, find the date of the earliest reported incident for the account
            date = Incident.find("SELECT MIN(i.date) FROM Incident i " +
                    "WHERE i.subscriber.account = ?", a).first();
            if(date != null) {
                // Only interested in the date portion of the DateTime
                MutableDateTime firstDayOfMonth = date.toDateMidnight().toMutableDateTime();
                firstDayOfMonth.setDayOfMonth(1);
                MutableDateTime firstDayOfWeek = date.toDateMidnight().toMutableDateTime();
                firstDayOfWeek.setDayOfWeek(1);
                /*
                 * Create the first slot on the first day of the oldest month, or week (whichever is oldest),
                 * where an incident was reported
                 */
                if(firstDayOfWeek.isBefore(firstDayOfMonth)) {
                    date = firstDayOfWeek.toDateTime();
                } else {
                    date = firstDayOfMonth.toDateTime();
                }
            }
        } else {
            // SummaryStats found, set date to create SummaryStats from the day after the most recent SummaryStat
            date = stat.date.plusDays(1);
        }

        // Date will be null if incidents were NOT reported for the account
        if(date != null) {
            // If needed, create SummaryStats for each day between date between 'date' and 'today'
            DateMidnight today = DateMidnight.now();
            if (date.isBefore(today)) {
                Logger.debug("Create SummaryStat slots for account '%s' from %s to %s", a.name, date, today);
                while(date.isBefore(today)) {
                    createSummaryStats(date, a);
                    date = date.plusDays(1);
                }
            }
        }
    }

    void updateSummaryStats(Account a, Granularity granularity) {

        if(granularity == Granularity.DAILY) {
            updateDailySummaryStats(a);
            return;
        }

        List<SummaryStat> stats = SummaryStat.find("SELECT s FROM SummaryStat s WHERE s.account=? " +
                "AND s.typeId=? AND s.dirty=true", a, granularity.getId()).fetch();

        for(SummaryStat stat : stats) {
            stat.recompute();
            stat.dirty = false;
            stat.save();
        }
    }

    void updateDailySummaryStats(Account a) {
        List<SummaryStat> stats = SummaryStat.find("SELECT s FROM SummaryStat s WHERE s.account=? " +
                "AND s.typeId=0 AND s.dirty=true", a).fetch();
        for(SummaryStat stat : stats) {
            computeFrequencyStats(stat);
            computePositionStats(stat);
            computeLocationStats(stat);
            computeIncidentTypeStats(stat);
            stat.dirty = false;
            stat.save();
        }
    }

    /**
     * Create daily and weekly and monthly (where appropriate) SummaryStats for the specified account on the
     * given date.
     *
     * @param date
     * @param a
     */
    void createSummaryStats(DateTime date, Account a) {
        SummaryStat stat =  new SummaryStat(a, date, Granularity.DAILY);
        stat.save();
        if(date.getDayOfWeek() == 1) {
            (new SummaryStat(a, date, Granularity.WEEKLY)).save();
        }
        if(date.getDayOfMonth() == 1) {
            (new SummaryStat(a, date, Granularity.MONTHLY)).save();
        }
    }

    void computeFrequencyStats(SummaryStat stat) {
        Query q = JPA.em().createQuery("SELECT i.frequency,COUNT(i.frequency) FROM Incident i where " +
                "i.date >= :start AND i.date < :end AND i.subscriber.account = :account " +
                "AND i.frequency != null GROUP BY i.frequency");
        q.setParameter("start", stat.date);
        q.setParameter("end", stat.date.plusDays(1));
        q.setParameter("account", stat.account);

        Map<Frequency, Long> map = (Map<Frequency, Long>) convertToMap(q.getResultList(), Frequency.class);
        stat.f1 = ((map.get(Frequency.ALWAYS) != null) ? map.get(Frequency.ALWAYS) : 0);
        stat.f2 = ((map.get(Frequency.OFTEN) != null) ? map.get(Frequency.OFTEN) : 0);
        stat.f3 = ((map.get(Frequency.ONCE) != null) ? map.get(Frequency.ONCE) : 0);
        stat.f4 = ((map.get(Frequency.SELDOM) != null) ? map.get(Frequency.SELDOM) : 0);
    }

    void computePositionStats(SummaryStat stat) {
        Query q = JPA.em().createQuery("SELECT i.position,COUNT(i.position) FROM Incident i where " +
                "i.date >= :start AND i.date < :end AND i.subscriber.account = :account " +
                "AND i.position != null GROUP BY i.position");
        q.setParameter("start", stat.date);
        q.setParameter("end", stat.date.plusDays(1));
        q.setParameter("account", stat.account);

        Map<Position, Long> map = (Map<Position, Long>) convertToMap(q.getResultList(), Position.class);
        stat.p1 = ((map.get(Position.INDOOR) != null) ? map.get(Position.INDOOR) : 0);
        stat.p2 = ((map.get(Position.OUTDOOR) != null) ? map.get(Position.OUTDOOR) : 0);
        stat.p3 = ((map.get(Position.ONTHEMOVE) != null) ? map.get(Position.ONTHEMOVE) : 0);
    }

    void computeLocationStats(SummaryStat stat) {
        Query q = JPA.em().createQuery("SELECT i.locationTech,COUNT(i.locationTech) FROM Incident i where " +
                "i.date >= :start AND i.date < :end AND i.subscriber.account = :account " +
                "AND i.locationTech != null GROUP BY i.locationTech");
        q.setParameter("start", stat.date);
        q.setParameter("end", stat.date.plusDays(1));
        q.setParameter("account", stat.account);

        Map<LocationTech, Long> map = (Map<LocationTech, Long>) convertToMap(q.getResultList(), LocationTech.class);
        stat.l1 = ((map.get(LocationTech.GPS) != null) ? map.get(LocationTech.GPS) : 0);
        stat.l2 = ((map.get(LocationTech.MANUAL) != null) ? map.get(LocationTech.MANUAL) : 0);
        stat.l3 = ((map.get(LocationTech.NETWORK) != null) ? map.get(LocationTech.NETWORK) : 0);
    }

    void computeIncidentTypeStats(SummaryStat stat) {
        Query q = JPA.em().createQuery("SELECT i.incidentType,COUNT(i.id) FROM Incident i where " +
                "i.date >= :start AND i.date < :end AND i.subscriber.account = :account GROUP BY i.incidentType");
        q.setParameter("start", stat.date);
        q.setParameter("end", stat.date.plusDays(1));
        q.setParameter("account", stat.account);

        Map<String, Long> map = convertIncidentTypeToMap(q.getResultList());
        stat.t1 = ((map.get("voice_dropped_call") != null) ? map.get("voice_dropped_call") : 0);
        stat.t2 = ((map.get("voice_poor_sound") != null) ? map.get("voice_poor_sound") : 0);
        stat.t3 = ((map.get("voice_network_busy") != null) ? map.get("voice_network_busy") : 0);
        stat.t4 = ((map.get("voice_no_coverage") != null) ? map.get("voice_no_coverage") : 0);
        stat.t5 = ((map.get("voice_other") != null) ? map.get("voice_other") : 0);
        stat.t6 = ((map.get("data_no_coverage") != null) ? map.get("data_no_coverage") : 0);
        stat.t7 = ((map.get("data_no_communication") != null) ? map.get("data_no_communication") : 0);
        stat.t8 = ((map.get("data_slow_connection") != null) ? map.get("data_slow_connection") : 0);
        stat.t9 = ((map.get("data_dropped_connection") != null) ? map.get("data_dropped_connection") : 0);
        stat.t10 = ((map.get("data_other") != null) ? map.get("data_other") : 0);
        stat.t11 = ((map.get("other_other") != null) ? map.get("other_other") : 0);
        stat.t = stat.t1 + stat.t2 + stat.t3 + stat.t4 + stat.t5 + stat.t6 +
                stat.t7 + stat.t8 + stat.t9 + stat.t10 + stat.t11;
    }

    private Map<? extends Enum, Long> convertToMap(List<Object[]> resultList, Class enumClass) {
        Map returnMap = new EnumMap(enumClass);
        for (Object[] values : resultList) {
            returnMap.put(values[0], values[1]);
        }
        return returnMap;
    }

    private Map<String, Long> convertIncidentTypeToMap(List<Object[]> resultList) {
        Map<String, Long> returnMap = new HashMap<String, Long>(resultList.size());
        for (Object[] values : resultList) {
            returnMap.put(((IncidentType) values[0]).key, (Long) values[1]);
        }
        return returnMap;
    }

}