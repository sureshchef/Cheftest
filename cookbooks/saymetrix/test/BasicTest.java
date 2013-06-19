
import models.Account;
import models.MobileSubscriber;
import models.WebUser;
import org.junit.Before;
import org.junit.Test;
import play.test.Fixtures;
import play.test.UnitTest;

public class BasicTest extends UnitTest {

    @Before
    public void setup() {
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("data.yml");
    }

    @Test
    public void testAccountsLoaded() {
        assertEquals(30, Account.count());
    }

    @Test(expected = javax.persistence.PersistenceException.class)
    public void testAccountKeyUnique() {
        WebUser u = WebUser.find("byFirstname", "Ciaran").first();
        Account a = new Account("DANU", "Dink");
        a.manager = u;

        a.save();
    }

    @Test
    public void testWebUsersLoaded() {
        assertEquals(6, WebUser.count());
    }

    @Test
    public void testMobileSubscribersLoaded() {
        assertEquals(3, MobileSubscriber.count());
    }

    @Test
    public void testAccountSubscribers() {
        Account account = Account.find("byKey", "VFIE").first();
        assertEquals(0, account.subscribers.size());
        account = Account.find("byKey", "TPZ").first();
        assertEquals(0, account.subscribers.size());
        account = Account.find("byKey", "DANU").first();
        assertEquals(3, account.subscribers.size());
    }
}
