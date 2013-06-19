package controllers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.RoleHolderPresent;
import controllers.error.ErrorTypes;
import models.AuditEvent;
import models.WebUser;
import notifiers.Mails;
import org.hibernate.Session;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;


@With(Deadbolt.class)
@RoleHolderPresent
public class Settings extends Controller {

    @Before
    static void setConnecteduser() {
        if (Security.isConnected()) {
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            renderArgs.put("webuser", user.firstname);

            // Account Managers should only be able to see their own accounts
            if("kam".equals(user.role.name)) {
                // HACK Allow all Viva users to have same privs as KAM
                ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);
            }
        }
    }

    public static void index() {
        password();
    }
    
    public static void password() {
        renderArgs.put("tab", "password");
        render();
    }
    
    public static void forgotPassword() {
        WebUser webUser = Security.getCurrentlyLoggedInUser();
        try {
            webUser.passwordResetToken = WebUser.generateToken();
            webUser.save();
            AuditEvent.create(AuditEvent.Type.USER_RESET, webUser.email, null).save();
            Mails.resetRequest(webUser);
        } catch(Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new controllers.error.Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        }
    }
    
    public static void setPassword(String current, String password) {
        JsonObject jsonObj = new JsonObject();
        WebUser webUser = Security.getCurrentlyLoggedInUser();
        if (!WebUser.isValidLogin(webUser.email, current)) {
            response.status = Http.StatusCode.BAD_REQUEST;
            jsonObj.addProperty("current-password", "The current password you've entered is incorrect.");
            renderJSON(new GsonBuilder().create().toJson(jsonObj));
        }
        String error = WebUser.validateNewPassword(password, params.get("confirm-password"));
        if ("".equalsIgnoreCase(error)) {
            webUser.setPassword(password);
            webUser.passwordResetToken = null;
            webUser.save();
            AuditEvent.create(AuditEvent.Type.USER_PASSWORD, webUser.email, null).save();
        } else {
            response.status = Http.StatusCode.BAD_REQUEST;
            jsonObj.addProperty("new-password", error);
            renderJSON(new GsonBuilder().create().toJson(jsonObj));
        }
    }
}
