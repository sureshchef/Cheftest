package controllers;

import controllers.deadbolt.DeadboltHandler;
import controllers.deadbolt.ExternalizedRestrictionsAccessor;
import controllers.deadbolt.RestrictedResourcesHandler;
import models.WebUser;
import models.deadbolt.ExternalizedRestrictions;
import models.deadbolt.RoleHolder;
import play.Logger;
import play.mvc.Controller;

public class SayMetrixDeadboltHandler extends Controller implements DeadboltHandler {

    public void beforeRoleCheck() {
        if (!Security.isConnected()) {
            try {
                if (!session.contains("username")) {
                    flash.put("url", "GET".equals(request.method) ? request.url : "/");
                    Secure.login();
                }
            } catch (Throwable t) {
                Logger.error(t, "Problem during login.");
            }
        }
    }

    public RoleHolder getRoleHolder() {
        String email = Secure.Security.connected();
        return WebUser.getByEmail(email);
    }

    public void onAccessFailure(String controllerClassName) {
        forbidden();
    }

    public ExternalizedRestrictionsAccessor getExternalizedRestrictionsAccessor() {
        return new ExternalizedRestrictionsAccessor() {
            public ExternalizedRestrictions getExternalizedRestrictions(String name) {
                return null;
            }
        };
    }

    public RestrictedResourcesHandler getRestrictedResourcesHandler() {
        return null;
    }
}
