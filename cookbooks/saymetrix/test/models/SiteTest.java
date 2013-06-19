package models;

import java.util.List;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;

public class SiteTest extends UnitTest {

    @Before
    public void setup() {
        ((Session) JPA.em().getDelegate()).disableFilter("manager");
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");
    }
    
    @Test
    public void testFind() {
        assertEquals(3, Site.count());
    }
    
    @Test
    public void testFindByKey() {
        Site s = Site.findByKey("site01");
        assertEquals("street 1", s.street_address);
    }
    
    @Test
    public void testFindAllFor() {
        Interval period = new Interval(new DateTime(), new DateTime());
        List<Site> list = Site.findAllFor(period, "50.3,-7.1,53.8,-5.9");
        assertEquals(2, list.size());
    }
}
