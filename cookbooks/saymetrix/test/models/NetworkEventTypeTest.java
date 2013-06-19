package models;

import org.junit.Before;
import org.junit.Test;
import play.test.Fixtures;
import play.test.UnitTest;

import org.hibernate.Session;
import play.db.jpa.JPA;

public class NetworkEventTypeTest extends UnitTest {

    @Before
    public void setup() {
        ((Session) JPA.em().getDelegate()).disableFilter("manager");
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");
    }
    
    @Test
    public void testFind() {
        assertEquals(1, NetworkEventType.count());
    }
}
