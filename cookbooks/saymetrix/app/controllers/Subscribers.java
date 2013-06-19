package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;
import controllers.error.Error;
import controllers.error.ErrorTypes;
import models.Account;
import models.MobileSubscriber;
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
public class Subscribers extends Controller {

    @Before
    static void setConnectedUser() {
        if (Security.isConnected()) {
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            renderArgs.put("webuser", user.firstname);
        }
    }

    public static void index() {
        renderArgs.put("tab", "subscribers");
        render();
    }
    
    public static void list(int sEcho, String sSearch, int iDisplayStart,
            int iDisplayLength, int iSortCol_0, String sSortDir_0,
            String mDataProp_4) {
        
        if (iDisplayLength == 0) {
            iDisplayLength = 10;
        }


        if ("json".equals(request.format)) {
            String sortingCol = "u.firstname";
            if (iSortCol_0 == 1) {
                sortingCol = "u.email";
            } else if (iSortCol_0 == 2) {
                sortingCol = "u.msisdn";
            } else if (iSortCol_0 == 3) {
                sortingCol = "u.account.key";
            }

            long iTotalRecords = MobileSubscriber.count();

            //Count number of Subscribers matching the filter
            String searchString = "%" + sSearch + "%";
            Query q = JPA.em().createQuery(
                    "SELECT COUNT(u) FROM MobileSubscriber u WHERE u.email LIKE :email OR u.firstname LIKE :firstname OR u.lastname LIKE :lastname OR u.account.name Like :account OR u.msisdn Like :msisdn ");
            q.setParameter("email", searchString);
            q.setParameter("firstname", searchString);
            q.setParameter("lastname", searchString);
            q.setParameter("account", searchString);
            q.setParameter("msisdn", searchString);
            long iTotalDisplayRecords = (Long) q.getSingleResult();

            StringBuilder qString = new StringBuilder(
                    "SELECT u FROM MobileSubscriber u WHERE u.email LIKE :email OR u.firstname LIKE :firstname OR u.lastname LIKE :lastname OR u.account.name Like :account OR u.msisdn Like :msisdn ORDER BY ");

            qString.append(sortingCol);
            qString.append(" ");
            qString.append(sSortDir_0);
            q = JPA.em().createQuery(qString.toString());
            q.setParameter("email", searchString);
            q.setParameter("firstname", searchString);
            q.setParameter("lastname", searchString);
            q.setParameter("account", searchString);
            q.setParameter("msisdn", searchString);
            q.setFirstResult(iDisplayStart);
            q.setMaxResults(iDisplayLength);

            List<MobileSubscriber> mobileSubscribers = q.getResultList();
            // Convert results to JSON
            Gson gson = GSONBuilderTemp.INSTANCE.fromSubscriber();
            PaginatedResult results = new PaginatedResult(sEcho, iTotalRecords,
                    iTotalDisplayRecords, mobileSubscribers);
            String toJsonResult = gson.toJson(results);

            renderJSON(toJsonResult);
        }

        List<MobileSubscriber> mobileSubscribers = MobileSubscriber.findAll();
        Gson gson = GSONBuilderTemp.INSTANCE.fromSubscriber();
        PaginatedResult results = new PaginatedResult(sEcho,
                MobileSubscriber.count(),
                10, mobileSubscribers);
        renderJSON(gson.toJson(results));

    }

    public static void create(String firstname, String lastname, String email,
            String msisdn, String imsi, String account) {

        if(msisdn.startsWith("0")) {
            msisdn = "353" + msisdn.substring(1);
        }
        try {
            MobileSubscriber s = new MobileSubscriber();
            s.firstname = firstname;
            s.lastname = lastname;
            s.email = email;
            s.msisdn = msisdn;
            s.imsi = imsi;

            try {
                s.account = Account.find("byKey", account).first();
            } catch (IllegalArgumentException e) {
                Validation.addError("s.account", "An Account must be selected");
            }
            s.validateAndCreate();
        } catch (Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        } finally {
            if (Validation.hasErrors()) {
                response.status = Http.StatusCode.BAD_REQUEST;
                JsonObject jsonObj = new JsonObject();
                for (play.data.validation.Error error : Validation.errors()) {
                    jsonObj.addProperty(error.getKey(), error.message());
                }
                Gson gson = new GsonBuilder().create();

                renderJSON(gson.toJson(jsonObj));
            }
        }

    }

    public static void get(String msisdn) {
        MobileSubscriber mobileSubscriber = MobileSubscriber.find(msisdn);
        if (mobileSubscriber != null) {
            Gson gson = GSONBuilderTemp.INSTANCE.fromSubscriber();
            renderJSON(gson.toJson(mobileSubscriber));
        } else {
            response.status = Http.StatusCode.NOT_FOUND;
            JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("message", "Subscriber doesn't exist");
            Gson gson = new GsonBuilder().create();

            renderJSON(gson.toJson(jsonObj));
        }

    }

    public static void edit(String oldMsisdn, String firstname, String lastname,
            String email,
            String msisdn, String imsi, String account) {
        try {
            MobileSubscriber s = MobileSubscriber.find("byMsisdn",
                    oldMsisdn).first();
            s.email = email;
            s.firstname = firstname;
            s.lastname = lastname;
            s.msisdn = msisdn;
            s.imsi = imsi;
            try {
                s.account = Account.find("byKey", account).first();
            } catch (IllegalArgumentException e) {
                Validation.addError("s.account", "An Account must be selected");
            }
            s.validateAndSave();
        } catch (Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        } finally {
            if (Validation.hasErrors()) {
                response.status = Http.StatusCode.BAD_REQUEST;
                JsonObject jsonObj = new JsonObject();
                for (play.data.validation.Error error : Validation.errors()) {
                    jsonObj.addProperty(error.getKey(), error.message());
                }

                Gson gson = new GsonBuilder().create();

                renderJSON(gson.toJson(jsonObj));
            }
        }

    }

    public static void delete(String msisdn) {
        try {
            MobileSubscriber mobileSubscriber = MobileSubscriber.find("byMsisdn",
                    msisdn).first();
            if (mobileSubscriber == null) {
                response.status = Http.StatusCode.NOT_FOUND;
                return;
            }
            mobileSubscriber.delete();
        } catch (Exception e) {
            Logger.error(e.getMessage());
            response.status = Http.StatusCode.BAD_REQUEST;
            Gson gson = new GsonBuilder().create();
            renderJSON(gson.toJson(new Error(
                    ErrorTypes.ERROR_DELETING_SUBSCRIBER)));
        }
    }
}
