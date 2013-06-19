package models;

import org.junit.Test;
import play.test.UnitTest;

public class AuditEventTest extends UnitTest {

    /*
     * It's important that the event Type ids don't change over time.
     */
    @Test
    public void testIds() {
        assertEquals(10, AuditEvent.Type.LOGIN_SUCCESS.id);
        assertEquals(11, AuditEvent.Type.LOGIN_FAIL.id);
        assertEquals(12, AuditEvent.Type.LOGOUT.id);
        assertEquals(20, AuditEvent.Type.MOBILE_ACTIVATE_SUCCESS.id);
        assertEquals(21, AuditEvent.Type.MOBILE_ACTIVATE_FAIL.id);
        assertEquals(22, AuditEvent.Type.BAD_TOKEN.id);
        assertEquals(30, AuditEvent.Type.USER_CREATE.id);
        assertEquals(31, AuditEvent.Type.USER_DELETE.id);
        assertEquals(32, AuditEvent.Type.USER_RESET.id);
        assertEquals(33, AuditEvent.Type.USER_PASSWORD.id);
        assertEquals(40, AuditEvent.Type.ACCOUNT_CREATE.id);
        assertEquals(41, AuditEvent.Type.ACCOUNT_DELETE.id);
        assertEquals(90, AuditEvent.Type.MAIL_SENT.id);
    }

    @Test
    public void testConstructors() {
        AuditEvent e = new AuditEvent(AuditEvent.Type.LOGIN_SUCCESS, null, "hello");
        assertEquals(AuditEvent.Type.LOGIN_SUCCESS, e.type);
        assertNull(e.actorEmail);
        assertEquals("hello", e.details);
        assertNotNull(e.timestamp);

        e = new AuditEvent(AuditEvent.Type.LOGIN_SUCCESS, "dink@foo.com", "hello");
        assertEquals(AuditEvent.Type.LOGIN_SUCCESS, e.type);
        assertEquals("dink@foo.com", e.actorEmail);
        assertEquals("hello", e.details);
        assertNotNull(e.timestamp);

        e = AuditEvent.create(AuditEvent.Type.LOGIN_SUCCESS, null, "hello");
        assertEquals(AuditEvent.Type.LOGIN_SUCCESS, e.type);
        assertNull(e.actorEmail);
        assertEquals("hello", e.details);
        assertNotNull(e.timestamp);

        e = new AuditEvent(AuditEvent.Type.LOGIN_SUCCESS, "dink@foo.com", "hello");
        assertEquals(AuditEvent.Type.LOGIN_SUCCESS, e.type);
        assertEquals("dink@foo.com", e.actorEmail);
        assertEquals("hello", e.details);
        assertNotNull(e.timestamp);
    }
}
