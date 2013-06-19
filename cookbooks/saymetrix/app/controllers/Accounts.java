package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;
import controllers.deadbolt.RoleHolderPresent;
import controllers.error.ErrorTypes;
import flexjson.JSONSerializer;
import models.Account;
import models.AuditEvent;
import models.WebUser;
import org.hibernate.Session;
import play.Logger;
import play.data.validation.Error;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

import javax.persistence.Query;
import java.util.Iterator;
import java.util.List;

@With(Deadbolt.class)
@Restrict("admin")
public class Accounts extends Controller {
    private static final JSONSerializer SER_ACCOUNT = Account.createSerializer();

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
        renderArgs.put("tab", "accounts");
        render();
    }

    public static void get(String key) {
        Account a = Account.find("byKey", key).first();
        if (a != null) {
            renderJSON(SER_ACCOUNT.serialize(a));
        }
        response.status = Http.StatusCode.NOT_FOUND;
    }

    @RoleHolderPresent
    public static void getAll() {
        List<Account> accounts = Account.find("order by name").fetch();
        renderJSON(SER_ACCOUNT.serialize(accounts));
    }

    public static void getAllPaginatedDataTable(int sEcho, String sSearch, int iDisplayStart,
            int iDisplayLength, int iSortCol_0, String sSortDir_0) {
        if (iDisplayLength == 0) {
            iDisplayLength = 10;
        }

        // TODO validate fields (sEcho etc.)
        String sortingCol;
        switch(iSortCol_0) {
            case 2:
                sortingCol = "a.manager.lastname " + sSortDir_0 + " a.manager.firstname";
                break;
            case 3:
                sortingCol = "a.contact";
                break;
            default:
                sortingCol = "a.name";
        }
        if(!"DESC".equals(sSortDir_0)){
            sSortDir_0 = "ASC";
        }

        // Count number of Accounts (no filtering)
        long iTotalRecords = Account.count();

        // Count number of Accounts matching the filter
        Query query = JPA.em().createQuery(
                "SELECT COUNT(a) FROM Account a WHERE a.key LIKE :key OR a.name LIKE :name OR a.manager.firstname LIKE :firstname OR a.manager.lastname LIKE :lastname OR a.contact LIKE :contact");
        query.setParameter("key", "%" + sSearch + "%");
        query.setParameter("name", "%" + sSearch + "%");
        query.setParameter("firstname", "%" + sSearch + "%");
        query.setParameter("lastname", "%" + sSearch + "%");
        query.setParameter("contact", "%" + sSearch + "%");

        long iTotalDisplayRecords = (Long) query.getSingleResult();

        // Get a single page of matching results
        StringBuilder qString = new StringBuilder(
                "SELECT a FROM Account a WHERE a.key LIKE :key OR a.name LIKE :name OR a.manager.firstname LIKE :firstname OR a.manager.lastname LIKE :lastname OR a.contact LIKE :contact ORDER BY ");
        qString.append(sortingCol);
        qString.append(" ");
        qString.append(sSortDir_0);
        query = JPA.em().createQuery(qString.toString());
        query.setParameter("key", "%" + sSearch + "%");
        query.setParameter("name", "%" + sSearch + "%");
        query.setParameter("firstname", "%" + sSearch + "%");
        query.setParameter("lastname", "%" + sSearch + "%");
        query.setParameter("contact", "%" + sSearch + "%");
        query.setFirstResult(iDisplayStart);
        query.setMaxResults(iDisplayLength);

        List<Account> accounts = query.getResultList();

        // Convert results to JSON
        PaginatedResult results = new PaginatedResult(sEcho, iTotalRecords,
                iTotalDisplayRecords, accounts);
        renderJSON(SER_ACCOUNT.serialize(results));
    }

    public static void create(Account account) {

        try {
            account.validateAndCreate();
            AuditEvent.create(AuditEvent.Type.ACCOUNT_CREATE, Security.connected(), account.name + " (" + account.key + ")").save();
        } catch (Exception e) {
            Logger.error(e, "Error validating and creating an account.");
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new controllers.error.Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        } finally {
            if (Validation.hasErrors()) {
                response.status = Http.StatusCode.BAD_REQUEST;
                JsonObject jsonObj = new JsonObject();
                for (Error error : Validation.errors()) {
                    jsonObj.addProperty(error.getKey(), error.message());
                }
                renderJSON(new GsonBuilder().create().toJson(jsonObj));
            }
        }
    }

    public static void edit(String oldKey, Account account) {
        Account workAroundAccount = account;
        /**
         * Need to use this workaround account so that variable name account is
         * validated against. Play uses the variable name for any validaiton
         * errors sent back e'g "account.key":"This is a required field". this is then
         * matched up on the client to form fields - Damien
         */
        try {
            account = Account.find("byKey", oldKey).first();
            account.key = workAroundAccount.key;
            account.name = workAroundAccount.name;
            account.contact = workAroundAccount.contact;
            account.manager = workAroundAccount.manager;
            account.validateAndSave();
        } catch (Exception e) {
            Logger.error(e, "Error validating and saving account.");
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new controllers.error.Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        } finally {
            if (Validation.hasErrors()) {
                response.status = Http.StatusCode.BAD_REQUEST;
                JsonObject jsonObj = new JsonObject();
                for (Iterator<Error> it = Validation.errors().iterator(); it.hasNext();) {
                    play.data.validation.Error error = it.next();
                    jsonObj.addProperty(error.getKey(), error.message());
                }
                renderJSON(new GsonBuilder().create().toJson(jsonObj));
            }
        }
    }

    public static void delete(String key) {
        Gson gson = new GsonBuilder().create();
        try {
            Account a = Account.find("byKey", key).first();
            if (a != null && a.subscribers.size() <= 0) {
                a.delete();
                AuditEvent.create(AuditEvent.Type.ACCOUNT_DELETE, Security.connected(), a.name + " (" + a.key + ")").save();
            } else if (a == null) {
                response.status = Http.StatusCode.NOT_FOUND;
                renderJSON(gson.toJson(new controllers.error.Error(
                        ErrorTypes.ACCOUNT_NOT_FOUND)));
            } else {
                response.status = Http.StatusCode.FORBIDDEN;
                renderJSON(gson.toJson(new controllers.error.Error(
                        ErrorTypes.ACCOUNT_CONTAINS_SUBSCRIBERS)));
            }
        } catch (Exception e) {
            Logger.error("Exception: %s", e.getMessage());
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(gson.toJson(new controllers.error.Error(
                    ErrorTypes.ERRROR_DELETING_ACCOUNT)));
        }
    }
}
