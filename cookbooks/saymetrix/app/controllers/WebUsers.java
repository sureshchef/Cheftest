package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;
import controllers.error.Error;
import controllers.error.ErrorTypes;
import flexjson.JSONSerializer;
import models.ApplicationRole;
import models.AuditEvent;
import models.WebUser;
import play.Logger;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;
import utils.gson.GSONBuilderTemp;

import javax.persistence.Query;
import java.util.List;

@With(Deadbolt.class)
@Restrict("admin")
public class WebUsers extends Controller {
    private static final JSONSerializer SER_WEB_USER = WebUser.createSerializer();

    @Before
    static void setConnecteduser() {
        if (Security.isConnected()) {
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            renderArgs.put("webuser", user.firstname);
        }
    }

    public static void index() {
        renderArgs.put("tab", "users");
        render();
    }

    public static void get(String email) {
        Gson gson = GSONBuilderTemp.INSTANCE.standardWithExpose();
        try {
            WebUser webUser = WebUser.find("byEmail", email).first();
            if (webUser != null) {
                renderJSON(SER_WEB_USER.serialize(webUser));
            }
            response.status = Http.StatusCode.NOT_FOUND;
        } catch (Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(gson.toJson(new Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        }
    }
    
    public static void find(Long id) {
        Gson gson = GSONBuilderTemp.INSTANCE.standardWithExpose();
        try {
            WebUser webUser = WebUser.findById(id);
            if (webUser != null) {
                renderJSON(SER_WEB_USER.serialize(webUser));
            }
            response.status = Http.StatusCode.NOT_FOUND;
        } catch (Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(gson.toJson(new Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        }
    }

    public static void create(WebUser user) {
        String passwordError = "";
        setRoleFromRequest(user);
        try {
            if (user.password.length() > 0) {
                user.validateAndCreate();
                AuditEvent.create(AuditEvent.Type.USER_CREATE, Security.connected(), user.email).save();
            } else {
                passwordError = "This is a required field";
            }
        } catch (Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new Error(
                    ErrorTypes.INTERNAL_SERVER_ERROR)));
        } finally {
            if (Validation.hasErrors()) {
                response.status = Http.StatusCode.BAD_REQUEST;
                JsonObject jsonObj = new JsonObject();
                for (play.data.validation.Error error : Validation.errors()) {
                    jsonObj.addProperty(error.getKey(), error.message());
                    Logger.warn("%s", error.message());
                }
                renderJSON(GSONBuilderTemp.INSTANCE.standardWithExpose().toJson(
                        jsonObj));
            }
            if (passwordError.length() > 0) {
                response.status = Http.StatusCode.BAD_REQUEST;
                JsonObject jsonObj = new JsonObject();
                jsonObj.addProperty("user.password", passwordError);
                renderJSON(GSONBuilderTemp.INSTANCE.standardWithExpose().toJson(jsonObj));
            }
        }
    }

    /**
     * We're using the role key in the request rather than the role id.
     * Play expects the role id, so we need to manually set the user.role
     * to avoid user.validateAndCreate borking
     */
    private static void setRoleFromRequest(WebUser user) {
        user.role = ApplicationRole.find("byName", request.params.get("user.role.name")).first();
    }

    public static void edit(String oldEmail, WebUser user) {
        /**
         * Need to use this workaround user so that variable name user is
         * validated against. Play uses the variable name for any validation
         * errors e'g "user.email":"This is a required field". this is then
         * matched up on the client to form fields
         */
        WebUser workAroundUser = user;
        try {
            user = WebUser.find("byEmail", oldEmail).first();
            user.email = workAroundUser.email;
            user.firstname = workAroundUser.firstname;
            user.lastname = workAroundUser.lastname;
            
            if (workAroundUser.password.length() > 0) {
                user.password = workAroundUser.password;
            }
            
            user.role = ApplicationRole.findByName(workAroundUser.role.name);
            user.validateAndSave();
        } catch (Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new Error(
                    ErrorTypes.INTERNAL_SERVER_ERROR)));
        } finally {
            if (Validation.hasErrors()) {
                response.status = Http.StatusCode.BAD_REQUEST;
                JsonObject jsonObj = new JsonObject();
                for (play.data.validation.Error error : Validation.errors()) {
                    jsonObj.addProperty(error.getKey(), error.message());
                }
                renderJSON(GSONBuilderTemp.INSTANCE.standardWithExpose().toJson(
                        jsonObj));
            }
        }
    }

    public static void list(int sEcho, String sSearch, int iDisplayStart,
            int iDisplayLength, int iSortCol_0, String sSortDir_0) {

        if (iDisplayLength == 0) {
            iDisplayLength = 10;
        }

        String sortingCol = "u.firstname";
        if (iSortCol_0 == 1) {
            sortingCol = "u.email";
        } else if (iSortCol_0 == 2) {
            sortingCol = "u.role";
        }

        long iTotalRecords = WebUser.count();

        //Count number of Webusers matching the filter
        Query q = JPA.em().createQuery(
                "SELECT COUNT(u) FROM WebUser u WHERE u.firstname LIKE :firstname OR u.lastname LIKE :lastname OR u.email LIKE :email");
        q.setParameter("firstname", "%" + sSearch + "%");
        q.setParameter("lastname", "%" + sSearch + "%");
        q.setParameter("email", "%" + sSearch + "%");

        long iTotalDisplayRecords = (Long) q.getSingleResult();

        StringBuilder qString = new StringBuilder(
                "SELECT u FROM WebUser u WHERE u.firstname LIKE :firstname OR u.lastname LIKE :lastname OR u.email LIKE :email ORDER BY ");
        qString.append(sortingCol);
        qString.append(" ");
        qString.append(sSortDir_0);
        q = JPA.em().createQuery(qString.toString());
        q.setParameter("firstname", "%" + sSearch + "%");
        q.setParameter("lastname", "%" + sSearch + "%");
        q.setParameter("email", "%" + sSearch + "%");
        q.setFirstResult(iDisplayStart);
        q.setMaxResults(iDisplayLength);

        List<WebUser> webUsers = q.getResultList();

        PaginatedResult results = new PaginatedResult(sEcho, iTotalRecords,
                iTotalDisplayRecords, webUsers);
        renderJSON(WebUser.createSerializer().serialize(results));
    }

    public static void delete(String email) {
        Gson gson = new GsonBuilder().create();
        try {
            WebUser user = WebUser.find("byEmail", email).first();
            if (Security.connected().equals(user.email)) {
                response.status = Http.StatusCode.FORBIDDEN;
                renderJSON(gson.toJson(new Error(ErrorTypes.DELETING_YOURSELF)));
            } else if (isUserActiveAccountManager(user)) {
                response.status = Http.StatusCode.FORBIDDEN;
                renderJSON(
                        gson.toJson(new Error(
                        ErrorTypes.USER_HAS_ACTIVE_ACCOUNTS)));
            } else {
                user.delete();
                AuditEvent.create(AuditEvent.Type.USER_DELETE, Security.connected(), user.email).save();
            }
        } catch (Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new Error(
                    ErrorTypes.INTERNAL_SERVER_ERROR)));
        }
    }

    public static void getAllManagers() {
        List<WebUser> managers = WebUser.find("order by lastname,firstname").
                fetch();
        renderJSON(SER_WEB_USER.serialize(managers));
    }

    public static void getAvailableRoles() {
        renderJSON(SER_WEB_USER.serialize(ApplicationRole.findAll()));
    }

    private static boolean isUserActiveAccountManager(WebUser user) {
        Query q = JPA.em().createQuery(
                "Select COUNT(a) FROM Account a WHERE a.manager.id LIKE :id");
        q.setParameter("id", user.id);

        return ((Long) q.getSingleResult() > 0);
    }

}
