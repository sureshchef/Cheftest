package models;

import java.util.List;
import models.Filter;
import models.Incident;
import models.WebUser;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;

public class IncidentTest extends UnitTest {

    @Before
    public void setup() {
        ((Session) JPA.em().getDelegate()).disableFilter("manager");
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");
//        Fixtures.loadModels("data.yml");
    }

    /**
     * Verify that only incidents accessible to the account
     * manager are returned when Hibernate filter is enabled.
     */
    @Test
    public void testAccountFilter() {
        WebUser user = WebUser.find("byEmail", "am1@operator.com").first();
        ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);
        assertEquals(13, Incident.count());

        user = WebUser.find("byEmail", "am2@operator.com").first();
        ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);
        assertEquals(2, Incident.count());

        user = WebUser.find("byEmail", "am3@operator.com").first();
        ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);
        assertEquals(0, Incident.count());
    }

    @Test
    public void findEmptyFilter() {
        assertEquals(15, Incident.find(new Filter()).size());
    }

    @Test
    public void findFilterPaged() {
        Filter filter = new Filter();
        assertEquals(15, Incident.find(filter, "date", "DESC", 0, 100).size());
    }

    @Test
    public void findFilterBounds() {
        Filter filter = new Filter();
        List<Incident> incidents = Incident.find(filter, "date", "DESC",
                "53.26122687305015,-6.844354248046875,53.38122687305015,-6.244354248046875");
        assertEquals(5, incidents.size());
    }

    @Test
    public void findFilterPagedBounds() {
        Filter filter = new Filter();
        List<Incident> incidents = Incident.find(filter, "date", "DESC",
                "53.26122687305015,-6.844354248046875,53.38122687305015,-6.244354248046875", 1, 3);
        assertEquals(3, incidents.size());
    }

}
