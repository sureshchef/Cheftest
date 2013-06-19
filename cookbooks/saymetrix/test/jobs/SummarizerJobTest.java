package jobs;

import org.junit.BeforeClass;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;

public class SummarizerJobTest extends UnitTest {
    private static final Long ZERO = 0L;

    @BeforeClass
    public static void oneTimeSetup() {
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");
    }

    @Test
    public void summarize() {
        new SummarizerJob().doJob();

        // Must have some daily SummaryStats with t > 0
        Long count = (Long)JPA.em().createQuery("SELECT COUNT(s.id) FROM SummaryStat s " +
                "WHERE s.typeId=0 AND s.t > 0").getSingleResult();
        assertTrue(count.intValue() > 0);

        // Must have some with SummaryStats with t > 0
        count = (Long)JPA.em().createQuery("SELECT COUNT(s.id) FROM SummaryStat s " +
                "WHERE s.typeId=1 AND s.t > 0").getSingleResult();
        assertTrue(count.intValue() > 0);

        // Must have some monthly SummaryStats with t > 0
        count = (Long)JPA.em().createQuery("SELECT COUNT(s.id) FROM SummaryStat s " +
                "WHERE s.typeId=2 AND s.t > 0").getSingleResult();
        assertTrue(count.intValue() > 0);

        // All LocationTechs must sum to t
        count = (Long)JPA.em().createQuery("select count(s.id) FROM SummaryStat s where s.t <> s.l1 " +
                        "+ s.l2 + s.l3").getSingleResult();
        assertEquals(ZERO, count);

        // All Frequencies must sum to t
        count = (Long)JPA.em().createQuery("select count(s.id) FROM SummaryStat s where s.t <> s.f1 " +
                        "+ s.f2 + s.f3 + s.f4").getSingleResult();
        assertEquals(ZERO, count);

        // All IncidentTypes must sum to t
        count = (Long)JPA.em().createQuery("select count(s.id) FROM SummaryStat s where s.t <> s.t1 " +
                        "+ s.t2 + s.t3 + s.t4 + s.t5 + s.t6 + s.t7 + s.t8 + s.t9 + s.t10 + s.t11").getSingleResult();
        assertEquals(ZERO, count);

        // All Positions must sum to t
        count = (Long)JPA.em().createQuery("select count(s.id) FROM SummaryStat s where s.t <> s.p1 " +
                        "+ s.p2 + s.p3").getSingleResult();
        assertEquals(ZERO, count);
    }
}
