package notifiers;

import models.WebUser;
import org.junit.Before;
import org.junit.Test;
import play.libs.Mail;
import play.test.Fixtures;
import play.test.UnitTest;

public class MailsTest extends UnitTest {
    private static final String ADMIN_EMAIL = "admin@danutech.com";

    @Before
    public void setup() {
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");
    }
    
    @Test
    public void resetRequest() throws Exception {
        //get user and apply token
        WebUser u = WebUser.getByEmail(ADMIN_EMAIL);
        u.passwordResetToken = WebUser.generateToken();
        Mails.resetRequest(u);
        String email = Mail.Mock.getLastMessageReceivedBy(ADMIN_EMAIL);
        assertTrue(email.contains("reset your"));
        assertTrue("Expected " + u.passwordResetToken + ", but got " + email,
                email.contains("pwreset/" + u.passwordResetToken));
    }
}