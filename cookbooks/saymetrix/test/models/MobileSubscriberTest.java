package models;

import org.junit.Before;
import org.junit.Test;
import play.test.Fixtures;
import play.test.UnitTest;

import java.util.UUID;

public class MobileSubscriberTest extends UnitTest {

    @Before
    public void setUp() {
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");
    }

    @Test
    public void testFind() {
        assertNull(MobileSubscriber.find(null));
        assertEquals("353862222222", MobileSubscriber.find("+353862222222").msisdn);
        assertEquals("353862222222", MobileSubscriber.find("353862222222").msisdn);
        assertEquals("353862222222", MobileSubscriber.find("00353862222222").msisdn);
        assertEquals("353862222222", MobileSubscriber.find("0862222222").msisdn);
        assertEquals("353862222222", MobileSubscriber.find("086 222 2222").msisdn);
        assertEquals("353862222222", MobileSubscriber.find("086-222-2222").msisdn);
    }

    @Test
    public void testGenerateToken() {
        String token = MobileSubscriber.generateToken(false);
        assertSame(token.toUpperCase(), token); // Check the token is uppercase (it's prettier :-)
        UUID.fromString(token); // Ensure the token is a valid UUID
    }

    @Test
    public void testGenerateLegacyToken() {
        String token = MobileSubscriber.generateToken(true);
        assertEquals(8, token.length());
        Integer.parseInt(token);
    }
}
