package utils;

import models.Account;
import models.SummaryStat;
import org.javatuples.KeyValue;
import org.joda.time.DateTime;
import play.db.jpa.JPA;

import javax.persistence.Query;
import java.math.BigInteger;
import java.util.List;

/**
 * Helper to service requests for regional incident statistics.
 */
public class RegionStatHelper {
    // FIXME rewrite these queries as JQL
    private static final String QUERY_ALL_ACCOUNTS = "SELECT COUNT(*),r.name " +
            "FROM incident i " +
            "INNER JOIN mobilesub m ON i.subscriber_id = m.id " +
            "INNER JOIN account a ON m.account_id = a.id " +
            "INNER JOIN rpt_region r ON i.address LIKE r.query " +
            "WHERE i.date BETWEEN ? AND ? " +
            "GROUP BY r.name " +
            "ORDER BY count(*) DESC " +
            "LIMIT 10";
    private static final String QUERY_SINGLE_ACCOUNT = "SELECT COUNT(*),r.name " +
            "FROM incident i " +
            "INNER JOIN mobilesub m ON i.subscriber_id = m.id " +
            "INNER JOIN account a ON m.account_id = a.id " +
            "INNER JOIN rpt_region r ON i.address LIKE r.query " +
            "WHERE i.date BETWEEN ? AND ? " +
            "AND a.id = ? " +
            "GROUP BY r.name " +
            "ORDER BY count(*) DESC " +
            "LIMIT 10";

    /**
     * Retrieve an ordered list of the regions from which the highest number of
     * incidents were reported during the specified interval.
     *
     * @param start
     * @param end
     * @param account the Account for which to retrieve statistics. If null,
     *                aggregate statistics across all accounts will be returned.
     * @return
     */
    public static SummaryStat.TopN getRegionStats(DateTime start, DateTime end, Account account) {
        /*
         * Here's what the query might look like if we were using IncidentDetails
         * <pre>
         * Query q = JPA.em().createQuery("SELECT COUNT(d) FROM IncidentDetails d " +
         *     "WHERE d.incident.subscriber.account = :account " +
         *     "AND d.incident.date >= :start AND d.incident.date < :end " +
         *     "AND d.addressJson LIKE :address");
         * </pre>
         *
         * Here's an attempt at a JPA query to get the required stats - only
         * returns the result for a single region. Need to figure out how to
         * JOIN and ON query syntax.
         * <pre>
         * Query q = JPA.em().createQuery("SELECT count(i),r.name FROM Incident i, Region r " +
               "WHERE i.date >= :start AND i.date < :end " +
               "AND i.subscriber.account = :account " +
               "AND i.address LIKE r.query");
         * </pre>
         *
         */

        Query q = null;

        if(account == null) {
            q = JPA.em().createNativeQuery(QUERY_ALL_ACCOUNTS);
        } else {
            q = JPA.em().createNativeQuery(QUERY_SINGLE_ACCOUNT);
            q.setParameter(3, account.id);
        }
        q.setParameter(1, start.toString());
        q.setParameter(2, end.toString());

        SummaryStat.TopN results = new SummaryStat.TopN();
        for(Object[] result : (List<Object[]>)q.getResultList()) {
            results.add(new KeyValue<Object, Long>(result[1], ((BigInteger)result[0]).longValue()));
        }

        return results;
    }
}
