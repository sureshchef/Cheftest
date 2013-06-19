package controllers;

import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.RoleHolderPresent;
import models.*;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.Session;
import org.javatuples.KeyValue;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import play.Play;
import play.cache.Cache;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.modules.excel.RenderExcel;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

import javax.persistence.Query;
import java.util.*;

@With(Deadbolt.class)
@RoleHolderPresent
public class Reports extends Controller {
    private static final int NUM_RESULTS_TO_RETURN = 10;
    public static final String START = "start";
    public static final String END = "end";
    public static final String ACCOUNTKEY = "accountkey";
    private static final String USER_ID = "uid";

    @Before
    static void setConnecteduser() {
        if (Security.isConnected()) {
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            renderArgs.put("webuser", user.firstname);
            renderArgs.put(USER_ID, user.id);

            // Account Managers should only be able to see their own accounts
            if("kam".equals(user.role.name)) {
                ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);
            }
        }
    }

    /**
     * Utility class to hold the report request parameters and manage the associated caches.
     */
    static class ReportParams {
        DateTime start;
        DateTime end;
        String accountKey;
        Dimension dimension;
        Granularity granularity;
        private Map<String, Object> paramMap;

        ReportParams(String startDate, String endDate, String accountKey, int dimension, String granularity) {
            if (startDate == null || endDate == null || granularity == null) {
                // Check the cache
                start = Cache.get(session.getId() + "-startperiod", DateTime.class);
                end = Cache.get(session.getId() + "-endperiod", DateTime.class);
                this.granularity = Cache.get(session.getId() + "-granularity", Granularity.class);

                // Cache miss? Use some defaults.
                if (start == null || end == null || this.granularity == null) {
                    // Midnight today (fyi - that's earlier today)
                    end = new DateTime().toDateMidnight().toDateTime();
                    start = end.minusDays(90);
                    this.granularity = Granularity.WEEKLY;
                }
            } else {
                start = new DateTime(startDate).toDateMidnight().toDateTime();
                end = new DateTime(endDate).toDateMidnight().toDateTime();
                this.granularity = Granularity.fromInt(Integer.valueOf(granularity));
            }

            try {
                this.dimension = Dimension.fromInt(dimension);
            } catch(IllegalArgumentException e) {
                // TODO Should something else be the default?
                this.dimension = Dimension.SUBSCRIBER;
            }

            // swap dates if they are in the wrong order
            if (end.isBefore(start)) {
                DateTime temp = end;
                end = start;
                start = temp;
            }

            if (accountKey == null && Cache.get(session.getId() + "-accountkey", String.class) != null) {
                accountKey = Cache.get(session.getId() + "-accountkey", String.class);
            }
            this.accountKey = accountKey;
            //after this, set cache values
            Cache.set(session.getId() + "-startperiod", start);
            Cache.set(session.getId() + "-endperiod", end);
            Cache.set(session.getId() + "-accountkey", this.accountKey);
            Cache.set(session.getId() + "-granularity", this.granularity);
        }

        boolean allAccounts() {
            return accountKey == null || accountKey.length() == 0;
        }

        Map<String, Object> asMap() {
            if(paramMap == null) {
                paramMap = new HashMap<String, Object>();
                paramMap.put("startperiod", start);
                paramMap.put("endperiod", end);
                paramMap.put(ACCOUNTKEY, accountKey);
                paramMap.put("granularity", granularity);
            }
            return paramMap;
        }
    }

    /**
     *
     * @param s start of date range
     * @param e end of date range
     * @param a a account key
     * @param d which top N dimension to display(type,position,frequecny)
     * @param g granularity (0=hourly, 1=day, 2=week, 3=month)
     */
    public static void index(String s, String e, String a, int d, String g) {
        ReportParams params = new ReportParams(s, e, a, d, g);

        Interval dateRange = new Interval(params.start, params.end.plusDays(1));

        IncidentCountStats stats = getIncidentCountSummaryStats(dateRange, a);
        Account account = Account.find("byKey", params.accountKey).first(); // TODO Cache opportunity?
        SummaryStat.TopN topN = getTopN(account, params.start, params.end, params.dimension, Security.isRedactEnabled());
        if(!params.allAccounts() && params.dimension == Dimension.ACCOUNT) {
            topN.add(new KeyValue<Object, Long>(account.name + " ( " + account.key + " )", stats.incidentCount));
        }
        topN.total = stats.incidentCount;
        IncidentSplit incidentSplit = getIncidentSplit(dateRange, account);

        Map originalParameters = params.asMap();
        render(originalParameters, stats, topN, incidentSplit);
    }

    public static void printIndex(String s, String e, String a, int d, String g) {
        ReportParams params = new ReportParams(s, e, a, d, g);

        Interval dateRange = new Interval(params.start, params.end.plusDays(1));

        IncidentCountStats stats = getIncidentCountSummaryStats(dateRange, a);
        Account account = Account.find("byKey", params.accountKey).first(); // TODO Cache opportunity?


        List<SummaryStat.TopN> allTopN = getAllTopNLists(account, params.start, params.end, stats.incidentCount, Security.isRedactEnabled());
        for(SummaryStat.TopN topN : allTopN) {
            topN.total = stats.incidentCount;
        }
        IncidentSplit incidentSplit = getIncidentSplit(dateRange, account);

        Map originalParameters = params.asMap();
        String dateString = params.start.toString("dd MMMM, yyyy") + " - " + params.end.toString("dd MMMM, yyyy");
        render(originalParameters, stats, allTopN, incidentSplit, dateString, account);
    }

    public static void download(String s, String e, String a, int d, String g) {
        ReportParams params = new ReportParams(s, e, a, d, g);

        Interval dateRange = new Interval(params.start, params.end.plusDays(1));

        IncidentCountStats stats = getIncidentCountSummaryStats(dateRange, a);
        Account account = Account.find("byKey", params.accountKey).first(); // TODO Cache opportunity?

        IncidentSplit incidentSplit = getIncidentSplit(dateRange, account);

        String title = params.start.toString("dd MMMM, yyyy") + " - " + params.end.toString("dd MMMM, yyyy");

        request.format = "xlsx";
        response.setContentTypeIfNotSet("application/vnd.ms-excel");
        renderArgs.put(RenderExcel.RA_ASYNC, true);
        renderArgs.put(RenderExcel.RA_FILENAME,
                DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmm")) + "_report.xlsm");
        renderArgs.put("title", title);
        DateTime now = DateTime.now();
        renderArgs.put("date", now);
        renderArgs.put("headerDate", DateTimeFormat.forPattern("dd/MM/y").print(now));

        renderArgs.put("stats", stats);
        renderArgs.put("split", incidentSplit);
        renderArgs.put("volumes", getIncidentVolumes(params.start, params.end.plusDays(1), params.accountKey, params.granularity.getId()));

        boolean redact = Security.isRedactEnabled();
        SummaryStat.TopN topn = getTopN(account, params.start, params.end, Dimension.ACCOUNT, redact);
        topn.total = stats.incidentCount;
        String accountTitle = Messages.get("all_accounts");
        if(account != null) {
            accountTitle = account.name;
            topn.add(new KeyValue<Object, Long>(account.name + " ( " + account.key + " )", stats.incidentCount));
        }
        renderArgs.put("account", accountTitle);
        renderArgs.put("topn_account", topn);

        renderArgs.put("topn_subscriber", getTopN(account, params.start, params.end, Dimension.SUBSCRIBER, redact));
        renderArgs.put("topn_region", getTopN(account, params.start, params.end, Dimension.REGION));
        renderArgs.put("topn_type", getTopN(account, params.start, params.end, Dimension.INCIDENT_TYPE));
        renderArgs.put("topn_position", getTopN(account, params.start, params.end, Dimension.POSITION));
        renderArgs.put("topn_frequency", getTopN(account, params.start, params.end, Dimension.FREQUENCY));
        renderArgs.put("topn_cell", getTopN(account, params.start, params.end, Dimension.CELL));
        renderArgs.put("topn_os", getTopN(account, params.start, params.end, Dimension.OS));
        renderArgs.put("topn_location", getTopN(account, params.start, params.end, Dimension.LOCATION_TECH));

        // Which brand should be shown in the Excel file
        String template = "Reports/download_saym.xlsx";
        if(Play.id.contains("vfie")) {
            template = "Reports/download_vfie.xlsx";
        }
        response.setHeader("Cache-Control", "max-age=0");
        renderTemplate(template, renderArgs);
    }

    /**
    * For the given account, retrieve the number of incidents reported during the specified interval.
    *
    * @param interval
    * @param accountKey the account to retrieve the incident count for. If <pre>null></pre> then
    *                   the incident count over all accounts is returned.
    * @param userId
    * @return
    */
    static long getIncidentCount(Interval interval, String accountKey, Long userId) {
        // Total Incidents
        String TI_ALL = "SELECT SUM(s.t) FROM SummaryStat s WHERE s.date >= :start AND s.date < :end " +
                "AND s.typeId=0";// AND s.account.manager.id = :uid";
        String TI_ACCT = "SELECT SUM(s.t) FROM SummaryStat s WHERE s.date >= :start AND s.date < :end AND " +
                "s.typeId=0 AND s.account.key = :accountkey";
        Query q;
        if(accountKey == null || accountKey.isEmpty()) {
            q = JPA.em().createQuery(TI_ALL);
//            q.setParameter(USER_ID, userId);
        } else {
            q = JPA.em().createQuery(TI_ACCT);
            q.setParameter(ACCOUNTKEY, accountKey);
        }
        q.setParameter(START, interval.getStart());
        q.setParameter(END, interval.getEnd());

        Long count = (Long)q.getSingleResult();

        return count != null ? count : 0;
    }

    /**
     * For the given account, retrieve the number of subscribers who reported incidents during the specified interval.
     *
     * @param interval
     * @param accountKey the account to retrieve the reporter count for. If <pre>null></pre> then
     *                   the reporter count over all accounts is returned.
     * @param userId
     * @return
     */
    static long getReporterCount(Interval interval, String accountKey, Long userId) {
        String TR_ALL = "SELECT COUNT(DISTINCT subscriber.id) FROM Incident i WHERE i.date >= :start " +
                "AND i.date < :end GROUP BY i.subscriber.id"; //AND i.subscriber.account.manager.id = :uid GROUP BY i.subscriber.id";
        String TR_ACCT = "SELECT COUNT(DISTINCT subscriber.id) FROM Incident i WHERE i.date >= :start " +
                "AND i.date < :end AND subscriber.account.key = :accountkey GROUP BY i.subscriber.id";

        Query q;
        if(accountKey == null || accountKey.isEmpty()) {
            q = JPA.em().createQuery(TR_ALL);
//            q.setParameter(USER_ID, userId);
        } else {
            q = JPA.em().createQuery(TR_ACCT);
            q.setParameter(ACCOUNTKEY, accountKey);
        }
        q.setParameter(START, interval.getStart());
        q.setParameter(END, interval.getEnd());

        return q.getResultList().size();
    }

    /**
     * Retrieve the number of accounts whose subscribers reported incidents during the specified interval.
     *
     * @param interval
     * @param accountKey
     * @param userId
     * @return
     */
    static long getAccountCount(Interval interval, String accountKey, Long userId) {
        // FIXME Doesn't make sense to have an account count when an account is selected
        String TA_ALL = "SELECT COUNT(DISTINCT s.account.id) FROM SummaryStat s WHERE s.date >= :start " +
                "AND s.date < :end AND s.typeId=1"; // AND d.account.manager.id = :uid
        String TA_ACCT = "SELECT COUNT(DISTINCT s.account.id) FROM SummaryStat s WHERE s.date >= :start " +
                "AND s.date < :end AND s.account.key = :accountkey AND s.typeId=1";

        Query q;
        if(accountKey == null || accountKey.isEmpty()) {
            q = JPA.em().createQuery(TA_ALL);
//            q.setParameter(USER_ID, userId);
        } else {
            q = JPA.em().createQuery(TA_ACCT);
            q.setParameter(ACCOUNTKEY, accountKey);
        }
        q.setParameter(START, interval.getStart());
        q.setParameter(END, interval.getEnd());

        Long count = (Long)q.getSingleResult();

        return count != null ? count : 0;
    }

    /**
     * For the given account, retrieve the number of subscribers who reported
     * incidents during the specified interval that had not previously done so.
     *
     * @param interval
     * @param accountKey the account to retrieve the new reporter count for. If <pre>null></pre> then
     *                   the new reporter count over all accounts is returned.
     * @param userId
     * @return
     */
    static long getNewReporterCount(Interval interval, String accountKey, Long userId) {
        String NR_ALL = "SELECT COUNT(DISTINCT subscriber.id) FROM Incident i WHERE i.date >= :start AND " +
                "i.date < :end AND i.subscriber.id NOT IN (SELECT s.subscriber.id FROM Incident s WHERE s.date < " +
                ":start)"; // AND i.subscriber.account.manager.id = :uid";
        String NR_ACCT = "SELECT COUNT(DISTINCT subscriber.id) FROM Incident i WHERE i.date >= :start " +
                "AND i.date < :end AND subscriber.account.key = :accountkey AND i.subscriber.id NOT IN " +
                "(SELECT s.subscriber.id FROM Incident s WHERE s.date < :start AND subscriber.account.key " +
                "= :accountkey)";

        Query q;
        if(accountKey == null || accountKey.isEmpty()) {
            q = JPA.em().createQuery(NR_ALL);
//            q.setParameter(USER_ID, userId);
        } else {
            q = JPA.em().createQuery(NR_ACCT);
            q.setParameter(ACCOUNTKEY, accountKey);
        }
        q.setParameter(START, interval.getStart());
        q.setParameter(END, interval.getEnd());

        Long count = (Long)q.getSingleResult();

        return count != null ? count : 0;
    }

    /**
     * Retrieve the number of accounts whose subscribers reported incidents during
     * the specified interval that had never previously done so (i.e. accounts that
     * had never previously had incidents reported).
     *
     * @param interval
     * @param userId
     * @return
     */
    static long getNewAccountCount(Interval interval, Long userId) {
        // FIXME Doesn't make sense to have a new accounts when an account is selected
        String NA_ALL = "SELECT COUNT(DISTINCT s.account.id) FROM SummaryStat s WHERE s.date >= " +
                ":start AND s.date < :end AND s.t > 0 AND s.account.id NOT IN (SELECT s1.account.id FROM " +
                "SummaryStat s1 WHERE s1.date < :start) GROUP BY s.account.id";
        String NA_ACCT = "SELECT COUNT(DISTINCT s.account.key) FROM SummaryStat s WHERE s.date >= :start " +
                "AND s.date < :end AND s.t > 0 AND s.account.key NOT IN (SELECT s1.account.key FROM " +
                "SummaryStat s1 WHERE s1.date < :start) GROUP BY s.account.key";

        Query q = JPA.em().createQuery(NA_ALL);
//        q.setParameter(USER_ID, userId);
        q.setParameter(START, interval.getStart());
        q.setParameter(END, interval.getEnd());

        return q.getResultList().size();
    }

    private static IncidentCountStats getIncidentCountSummaryStats(Interval interval, String accountKey) {
        IncidentCountStats stats = new IncidentCountStats();

        Long userId = (Long)renderArgs.get(USER_ID);

        stats.incidentCount = getIncidentCount(interval, accountKey, userId);
        stats.reporters = getReporterCount(interval, accountKey, userId);
        stats.newReporters = getNewReporterCount(interval, accountKey, userId);
        if(accountKey == null || accountKey.isEmpty()) {
            stats.newAccounts = getNewAccountCount(interval, userId);
            stats.accountCount = getAccountCount(interval, accountKey, userId);
        } else {
            stats.newAccounts = 0;
            stats.accountCount = 1;
        }

        return stats;
    }

    public static SummaryStat.TopN getTopN(Account account, DateTime start, DateTime end,
                                           Dimension dimension) {
        return getTopN(account, start, end, dimension, false);
    }

    public static SummaryStat.TopN getTopN(Account account, DateTime start, DateTime end,
                                           Dimension dimension, boolean redact) {
        SummaryStat.TopN results;

        switch (dimension) {
            case OS:
                results = Incident.findTopNMobiles(account, new Interval(start, end), NUM_RESULTS_TO_RETURN);
                break;
            case CELL:
                results = Incident.findTopNCells(account, new Interval(start, end), NUM_RESULTS_TO_RETURN);
                break;
            case SUBSCRIBER:
                results = Incident.findTopNSubscribers(account, new Interval(start, end), NUM_RESULTS_TO_RETURN, redact);
                break;
            default:
                results = SummaryStat.getTopN(account, start, end, dimension);
                break;
        }

        return results;
    }

    private static List<SummaryStat.TopN> getAllTopNLists(Account account, DateTime start, DateTime end,
                                                          Long incidentCount, boolean redact) {
        List<SummaryStat.TopN> topNLists = new ArrayList<SummaryStat.TopN>();

        SummaryStat.TopN topN = getTopN(account, start, end, Dimension.ACCOUNT, redact);
        if(account != null) {
            // Update the account topn stat with the number of incidents
            topN.add(new KeyValue<Object, Long>(account.name + " ( " + account.key + " )", incidentCount));
        }
        topNLists.add(topN);
        // The printIndex view depends on this order not changing
        topNLists.add(getTopN(account, start, end, Dimension.SUBSCRIBER, redact));
        topNLists.add(getTopN(account, start, end, Dimension.INCIDENT_TYPE));
        topNLists.add(getTopN(account, start, end, Dimension.POSITION));
        topNLists.add(getTopN(account, start, end, Dimension.FREQUENCY));
        topNLists.add(getTopN(account, start, end, Dimension.CELL));
        topNLists.add(getTopN(account, start, end, Dimension.OS));
        topNLists.add(getTopN(account, start, end, Dimension.LOCATION_TECH));
        topNLists.add(getTopN(account, start, end, Dimension.REGION));

        return topNLists;
    }

    /**
     * Retrieve the voice/data split of incidents reported during the specified interval.
     *
     * @param interval
     * @return
     */
    static IncidentSplit getIncidentSplit(Interval interval, Account account) {
        Number voice = 0;
        Number data = 0;
        Number other = 0;

        Query query;
        if(account == null) {
            query = JPA.em().createQuery("SELECT i.incidentType.incidentGroup.name, COUNT(i.incidentType" +
                ".incidentGroup) FROM Incident i WHERE i.date >= :start AND i.date < :end " +
                "GROUP BY i.incidentType.incidentGroup");
        } else {
            query = JPA.em().createQuery("SELECT i.incidentType.incidentGroup.name, COUNT(i.incidentType" +
                    ".incidentGroup) FROM Incident i WHERE i.date >= :start AND i.date < :end " +
                    "AND i.subscriber.account.key=:accountkey GROUP BY i.incidentType.incidentGroup");
            query.setParameter(ACCOUNTKEY, account.key);
        }
        query.setParameter(START, interval.getStart());
        query.setParameter(END, interval.getEnd());
        
        List<Object[]> resultList = query.getResultList();
        for (Object[] resultObject : resultList) {
            if (resultObject[0].equals("Voice")) {
                voice = (Number) resultObject[1];
            } else if (resultObject[0].equals("Data")) {
                data = (Number) resultObject[1];
            } else {
                other = (Number) resultObject[1];
            }
        }
        
        IncidentSplit split = new IncidentSplit();
        split.voice = voice.longValue();
        split.data = data.longValue();
        split.other = other.longValue();

        return split;
    }
    
    private static void orderCollection(List<DimensionDetail> topN) {
        Collections.sort(topN, new Comparator<DimensionDetail>() {
            public int compare(DimensionDetail d1, DimensionDetail d2) {
                if (d1.count > d2.count) {
                    return 1;
                } else if (d1.count < d2.count) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        Collections.reverse(topN);
    }

    private static int getTotalNumber(Map<String, Long> topNMap) {
        int total = 0;
        for (Map.Entry<String, Long> entry : topNMap.entrySet()) {
            if (entry.getValue() != null) {
                total = (int) (total + entry.getValue());
            }
        }
        return total;
    }

    public static class IncidentCountStats {
        public long incidentCount;
        long accountCount;
        public long reporters;
        long newReporters;
        long newAccounts;

        public float getIncidentsPerReporter() {
            return incidentCount == 0 || reporters == 0 ? 0 : (float)incidentCount/reporters;
        }

        public float getIncidentsPerAccount() {
            return incidentCount == 0 || accountCount == 0 ? 0 : incidentCount / accountCount;
        }

        public float getPercentageNewReporters() {
            float result = 0;
            if(reporters != 0) {
                result = (float)newReporters / reporters * 100;
            }
            return result;
        }

        public float getPercentageNewAccounts() {
            float result = 0;
            if(accountCount != 0) {
                result = (float)newAccounts / accountCount * 100;
            }
            return result;
        }
    }

    public static class DimensionDetail {
        String name;
        long count;
        double percentage;
    }

    public static class IncidentSplit {
        public long voice;
        public long data;
        public long other;

        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    /**
     *
     * @param s start of date range
     * @param e end of date range
     * @param a account key
     * @param g granularity (0=day, 1=week, 2=month)
     */
    public static void getFullStats(DateTime s, DateTime e, String a, int g) {
        if (s == null || e == null) {
            response.status = Http.StatusCode.BAD_REQUEST;
            return;
        }

        List<KeyValue<DateTime, Long>> volumes = getIncidentVolumes(s, e.plusDays(1), a, g);

        renderJSON(SummaryStat.createSerializer().serialize(volumes));
    }

    public static List<KeyValue<DateTime, Long>> getIncidentVolumes(DateTime startperiod, DateTime endperiod, String accountkey, int granularity) {

        Interval interval = new Interval(startperiod, endperiod);
        Account account = Account.findByKey(accountkey);
        Granularity g;

        // Default to WEEKLY if we get an illegal granularity
        try {
            g = Granularity.fromInt(granularity);
        } catch(IllegalArgumentException e) {
            g = Granularity.WEEKLY;
        }
        return SummaryStat.getIncidentVolumes(g, interval, account);
    }

}
