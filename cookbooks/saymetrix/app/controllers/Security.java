package controllers;

import models.AuditEvent;
import models.SystemSetting;
import models.WebUser;
import notifiers.Mails;

public class Security extends Secure.Security {

    static boolean authenticate(String username, String password) {
        boolean success = false;
        if(username.length() != 0 && password.length() != 0) {
            success = WebUser.isValidLogin(username, password);

            AuditEvent event = new AuditEvent();
            event.actorEmail = username;
            if(success) {
                event.setType(AuditEvent.Type.LOGIN_SUCCESS);
            } else {
                event.setType(AuditEvent.Type.LOGIN_FAIL);
            }
            event.save();
        }

        return success;
    }

    static void onDisconnect() {
        AuditEvent.create(AuditEvent.Type.LOGOUT, connected(), null).save();
    }

    // TODO review whether this is required
    public static WebUser getCurrentlyLoggedInUser() {
        return WebUser.find("byEmail", Security.connected()).first();
    }

    public static boolean isRedactEnabled() {
        return SystemSetting.getBoolean("redact.enabled", false) && !Security.getCurrentlyLoggedInUser().isAdmin();
    }
    
    public static void forgotPassword() {
        render();
    }
    
    public static void requestReset(String email) {
        WebUser webUser = WebUser.find("byEmail", email).first();
        if (webUser != null) {
            webUser.passwordResetToken = WebUser.generateToken();
            webUser.save();
            AuditEvent.create(AuditEvent.Type.USER_RESET, webUser.email, null).save();
            Mails.resetRequest(webUser);
        }
        //render the page, regardless of whether the webuser exists or not
        render();
    }
    
    public static void reset(String token) {
        if (isConnected()) {
            session.clear();
            response.removeCookie("rememberme");
        }
        WebUser u = WebUser.find("byPasswordResetToken", token).first();
        if (u != null) {
            flash.put("token", token);
            changePassword();
        } else {
            flash.error("Unable to verify a password reset request for this user. Please try again.");
            forgotPassword();
        }
    }
    
    public static void changePassword() {
        render();
    }
    
    public static void resetPassword(String token, String password) {
        WebUser u = WebUser.find("byPasswordResetToken", token).first();
        if (u == null) {
            flash.error("Unable to verify a password reset request for this user. Please try again.");
            forgotPassword();
        }
        
        String error = WebUser.validateNewPassword(password, params.get("confirm-password"));
        if ("".equalsIgnoreCase(error)) {
            u.setPassword(password);
            u.passwordResetToken = null;
            u.save();
            AuditEvent.create(AuditEvent.Type.USER_PASSWORD, u.email, null).save();
            render();
        } else {
            flash.error(error);
            flash.put("token", token);
            changePassword();
        }
    }
}
