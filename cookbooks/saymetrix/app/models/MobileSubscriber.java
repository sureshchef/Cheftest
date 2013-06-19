package models;

import com.google.gson.annotations.Expose;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import play.data.validation.*;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Entity
@Table(name = "mobilesub")
public class MobileSubscriber extends Model {

    @Expose
    @MinSize(12)
    @Phone(message = "Not a valid IMSI - 12 to 15 digits")
    @Unique(message = "This imsi is already in use")
    public String imsi;
    @Expose
    @Required(message = "This is a required field")
    @Unique(message = "This msisdn is already in use")
    @Column(unique = true)
    @Phone(message = "Not a valid phone number")
    @MinSize(8)
    public String msisdn;
    @Expose
    @Required(message = "This is a required field")
    @Column(nullable = false)
    public String firstname;
    @Expose
    @Required(message = "This is a required field")
    @Column(nullable = false)
    public String lastname;
    @Expose
    @Email
//    @Column(nullable = false)
//    @Unique(message = "This email is already in use")
    public String email;
    public String token;
    @OneToMany(mappedBy = "subscriber")
    public List<Incident> incidents = new ArrayList<Incident>();
    @Required(message = "The Subscriber must have an account")
    @Expose
    @ManyToOne
    public Account account;
    public boolean suspended;
    public boolean vip;
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    public DateTime provisioned;
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    public DateTime activated;

    public static boolean isValidSubscriber(String token) {
        return (count("token=?", token) == 1);
    }

    public void register(boolean oldStyleToken) throws IllegalStateException {
        if (token != null) {
            throw new IllegalStateException("Subscriber already registered");
        }

        token = generateToken(oldStyleToken);
        save();
    }

    /**
     * Generate a unique token to be used to identify REST API invocations from this MobileSubscriber.
     *
     * @return
     */
    static String generateToken(boolean oldStyleToken) {
        String token;

        if(oldStyleToken) {
            char[] digits = new char[8];
            for (int i = 0; i < 8; i++) {
                digits[i] = (char) ((new Random()).nextInt(10) + '0');
            }
            token = new String(digits);
        } else {
            token = UUID.randomUUID().toString().toUpperCase();
        }

        return token;
    }

    /**
     * Find the MobileSubscriber with the specified phone number.
     * We store phone numbers as 353878041429, i.e no leading '+' or
     * international access code (e.g. '00'). This method will
     * attempt to find a match for phone numbers not supplied in
     * that format.
     */
    public static MobileSubscriber find(String msisdn) {
        if(msisdn == null) {
            return null;
        }
        // Remove spaces and dashes
        msisdn = msisdn.replace(" ", "");
        msisdn = msisdn.replace("-", "");
        // Strip leading '+' or '00' ('00' doesn't work in all countries)
        if(msisdn.startsWith("+")) {
            msisdn = msisdn.substring(1);
        } else if(msisdn.startsWith("00")) {
            msisdn = msisdn.substring(2);
        }

        MobileSubscriber subscriber = MobileSubscriber.find("byMsisdn", msisdn).first();
        // Not found? Prepend Ireland country code - TODO should be configurable
        if(subscriber == null && msisdn.startsWith("0")) {
            msisdn = "353" + msisdn.substring(1);
        }
        return MobileSubscriber.find("byMsisdn", msisdn).first();
    }

    public String toString() {
        return new ToStringBuilder(this).append("msisdn", msisdn).
                append("name", firstname + " " + lastname).
                append("email", email).
                append("token", token).
                toString();
    }
}
