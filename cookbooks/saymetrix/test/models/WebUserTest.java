package models;

import flexjson.JSONSerializer;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import play.test.Fixtures;
import play.test.UnitTest;

public class WebUserTest extends UnitTest {
    private static final String ADMIN_EMAIL = "admin@danutech.com";

    @Before
    public void setup() {
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");
    }

    @Test
    public void testJsonSerialization() {
        WebUser u = WebUser.getByEmail(ADMIN_EMAIL);

        JSONSerializer ser = WebUser.createSerializer().prettyPrint(false);
        String expected = "{\"email\":\"" + ADMIN_EMAIL + "\"," +
                "\"firstname\":\"DANU\"," +
                "\"id\":" + u.id + "," +
                "\"lastname\":\"Admin\"," +
                "\"role\":{" +
                "\"longname\":\"Administrator\"," +
                "\"name\":\"admin\"}}";
        assertEquals(expected, ser.serialize(u));
    }

    @Test
    public void testPasswordCrypt() {
        WebUser u = WebUser.getByEmail(ADMIN_EMAIL);
        assertNull(u.password); // Ensure cleartext password isn't being stored in database

        u.setPassword("foobar");
        u.save();

        assertTrue(WebUser.isValidLogin(ADMIN_EMAIL, "foobar"));
    }

    @Test
    public void testGenerateToken() {
        String token = WebUser.generateToken();
        assertSame(token.toUpperCase(), token); // Check the token is uppercase (it's prettier :-)
        UUID.fromString(token); // Ensure the token is a valid UUID
    }
    
    @Test
    public void testValidateNewPassword() {
        String password = "hiphopopotamus";
        String passwordWithSpaces = "hiph po ot mus";
        String passwordTooShort = "potamus";
        String wrongPassword = "rhymenoceros";
        
        assertTrue("Password must have a value.".equalsIgnoreCase(WebUser.validateNewPassword(null, password)));
        assertTrue("Password must be at least 8 characters long.".equalsIgnoreCase(WebUser.validateNewPassword(passwordTooShort, password)));
        assertTrue("Password cannot contain spaces.".equalsIgnoreCase(WebUser.validateNewPassword(passwordWithSpaces, password)));
        assertTrue("Password don't match.".equalsIgnoreCase(WebUser.validateNewPassword(password, wrongPassword)));
        assertTrue("".equalsIgnoreCase(WebUser.validateNewPassword(password, password)));
    }

}
