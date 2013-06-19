package models;

import flexjson.JSONSerializer;
import org.joda.time.DateTime;
import play.Play;
import play.db.jpa.Model;
import utils.AuditEventTransformer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * AuditEvents are used to store a permanent record of important
 * system events.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent extends Model {
    public enum Type {
        // WARN Do not change the integers below
        LOGIN_SUCCESS(10, "audit.login.success"),
        LOGIN_FAIL(11, "audit.login.fail"),
        LOGOUT(12, "audit.logout"),
        MOBILE_ACTIVATE_SUCCESS(20, "audit.mob.activate.success"),
        MOBILE_ACTIVATE_FAIL(21, "audit.mob.activate.fail"),
        BAD_TOKEN(22, "audit.bad.token"),
        USER_CREATE(30, "audit.user.create"),
        USER_DELETE(31, "audit.user.delete"),
        USER_RESET(32, "audit.user.reset"),
        USER_PASSWORD(33, "audit.user.password"),
        ACCOUNT_CREATE(40, "audit.account.create"),
        ACCOUNT_DELETE(41, "audit.account.delete"),
        NETWORK_EVENT_CREATE(50, "audit.networkevent.create"),
        NETWORK_EVENT_DELETE(51, "audit.networkevent.delete"),
        MAIL_SENT(90, "audit.mail.sent");

        final int id;
        final String message;

        private Type(int id, String message) {
            this.id = id;
            this.message = message;
        }

        public String toString() {
            return message;
        }

        public static Type fromInt(int id) {
            for(Type t : values()) {
                if(t.id == id) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Illegal AuditEvent.Type (" + id + ")");
        }
    }

    /*
     * type and typeId are setup like this to allow us to use
     * Type id rather than ordinal for JPA.
     */
    @Transient
    Type type;
    @Column(name = "type_id")
    int typeId;
    @Column(name = "actor_email")
    public String actorEmail;
    public String details;
    @org.hibernate.annotations.Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    public DateTime timestamp = new DateTime();

    public AuditEvent() {
    }

    /**
     * Create an AuditEvent of the specified type with the given detail
     * message. How the detail message will be interpreted varies
     * depending on the Type.
     *
     * @param type
     * @param actorEmail the email address of the user responsible for event
     * @param details
     */
    public AuditEvent(Type type, String actorEmail, String details) {
        setType(type);
        this.actorEmail = actorEmail;
        this.details = details;
    }

    /**
     * Create an anonymous AuditEvent of the specified type with the
     * given detail message. How the detail message will be interpreted
     * varies depending on the Type.
     *
     * @param type
     * @param details
     */
    public AuditEvent(Type type, String details) {
        this(type, null, details);
    }

    /**
     * Convenience method to create an AuditEvent.
     *
     * @see #AuditEvent(models.AuditEvent.Type, String, String)
     */
    public static AuditEvent create(Type type, String actorEmail, String details) {
        return new AuditEvent(type, actorEmail, details);
    }

    /**
     * Convenience method to create an anonymous AuditEvent.
     *
     * @see #AuditEvent(models.AuditEvent.Type, String)
     */
    public static AuditEvent create(Type type, String details) {
        return create(type, null, details);
    }

    public void setType(Type t) {
        // See note at type and typeId declaration
        this.type = t;
        this.typeId = type.id;
    }

    public int getTypeId() {
        return typeId;
    }

    void setTypeId(int id) {
        // See note at type and typeId declaration
        this.typeId = id;
        for(Type t : Type.values()) {
            if(t.id == id) {
                type = t;
            }
        }
    }

    /**
     * Create a JSONSerializer suited to serializing AuditEvents into
     * a format suited for use by DataTables.
     *
     * @return
     */
    public static JSONSerializer createSerializer() {
        return new JSONSerializer().transform(new AuditEventTransformer(), AuditEvent.class).include(
                "sEcho", "iTotalRecords", "iTotalDisplayRecords", "aaData"
        ).exclude("*").prettyPrint(Play.mode == Play.Mode.DEV);
    }
}
