package notifiers;

import models.AuditEvent;
import models.WebUser;
import play.Play;
import play.mvc.Mailer;
 
public class Mails extends Mailer {

    public static void resetRequest(WebUser user) {
        String productName = "SayMetrix";
        String templateSuffix = "saym";

        if(Play.id.contains("vfie")) {
            templateSuffix = "vfie";
        }

        setFrom(productName + " <noreply@danutech.com>");
        setSubject("Reset your " + productName + " password");
        addRecipient(user.email);
        
        /*
         * A template can be specified if the first parameter is a string,
         * i.e. send("foo"); (don't include the .html part)
         * 
         * However, the following does not work:
         * String template = "foo";
         * send(template);
         * 
         * Not sure why, as it should really, so I've had to come up with
         * a work around.
         */
        send("Mails/password_reset_" + templateSuffix, user);
        AuditEvent.create(AuditEvent.Type.MAIL_SENT, user.email, "Password reset").save();
    }

}