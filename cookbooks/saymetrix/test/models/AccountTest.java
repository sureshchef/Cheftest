package models;

import flexjson.JSONSerializer;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import play.Logger;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;
import java.util.List;

public class AccountTest extends UnitTest {

    @Before
    public void setup() {
        ((Session) JPA.em().getDelegate()).disableFilter("manager");
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");
    }

    /**
     * Verify all accounts are returned.
     */
    @Test
    public void testFind() {
        assertEquals(4, Account.count());
    }

    /**
     * Verify that only accounts accessible to the account
     * manager are returned when Hibernate filter is enabled.
     */
    @Test
    public void testAccountFilter() {
        long acct3Id = Account.findByKey("ACCT3").id;

        WebUser user = WebUser.find("byEmail", "am1@operator.com").first();
        ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);

        List<Account> accounts = Account.findAll();
        assertEquals(2, Account.count());
        assertEquals("ACCT1", accounts.get(0).key);
        assertEquals("ACCT2", accounts.get(1).key);

        // Now try and get ACCT3 (an account the user am1 does not have access to
        assertNull(Account.findById(acct3Id));
    }

    @Test
    public void testJsonSerialization() {
        Account a = Account.findByKey("ACCT3");

        JSONSerializer ser = Account.createSerializer().prettyPrint(false);
        Logger.info(ser.serialize(a));
        String expected = String.format("{\"contact\":\"contact@danutech.com\",\"id\":%d,\"key\":\"ACCT3\"," +
                "\"manager\":{\"firstname\":\"Account\",\"id\":%d,\"lastname\":\"Manager2\"}," +
                "\"name\":\"Account 3\"}", a.id, a.manager.id);
        assertEquals(expected, ser.serialize(a));
    }

}
