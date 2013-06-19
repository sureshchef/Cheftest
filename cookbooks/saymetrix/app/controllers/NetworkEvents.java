package controllers;

import com.google.gson.*;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.RoleHolderPresent;
import controllers.error.Error;
import controllers.error.ErrorTypes;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Query;
import models.*;
import org.hibernate.Session;
import org.joda.time.DateTime;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

import org.joda.time.Interval;
import play.Logger;
import play.data.binding.As;
import play.data.validation.Validation;

@With(Deadbolt.class)
@RoleHolderPresent
public class NetworkEvents extends Controller {

    @Before
    static void setConnecteduser() {
        if (Security.isConnected()) {
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            renderArgs.put("webuser", user.firstname);

            // Account Managers should only be able to see their own accounts
            if("kam".equals(user.role.name)) {
                ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);
            }
        }
    }

    public static void index() {
        renderArgs.put("tab", "networkevents");
        renderArgs.put("networkEventTypes", NetworkEventType.findAll(true));
        render();
    }

    public static void detail(Long id) {
        NetworkEvent ne = NetworkEvent.findById(id);
        ne.setNumOfIncidents();
        renderArgs.put("tab", "networkevents");
        render(ne);
    }
    
    public static void get(Long id) {
        NetworkEvent ne = NetworkEvent.findById(id);
        if (ne != null) {
            ne.setNumOfIncidents();
            renderJSON(NetworkEvent.createSerializer().serialize(ne));
        }
        response.status = Http.StatusCode.NOT_FOUND;
    }
    
    public static void create() {
        Logger.info("network events - create");
        JsonObject eventJson = new JsonParser().parse(params.get("body")).getAsJsonObject();
        try {
            NetworkEvent ne = new NetworkEvent();
            ne.subject = eventJson.get("subject").getAsString();
            ne.description = eventJson.get("description").getAsString();
            ne.eventType = NetworkEventType.findByKey(eventJson.get("event-type").getAsString());
            ne.eventPeriod = new Interval(new DateTime(eventJson.get("start-date").getAsString()), new DateTime(eventJson.get("end-date").getAsString()));
            JsonArray jsonSites = eventJson.get("sites").getAsJsonArray();
            List<Site> sites = new ArrayList<Site>();
            for (JsonElement jsonSite : jsonSites) {
                sites.add(Site.findByKey(jsonSite.getAsString()));
            }
            ne.sites = sites;
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            ne.creator = user;
            ne.createdOn = new DateTime();
            ne.validateAndCreate();
            AuditEvent.create(AuditEvent.Type.NETWORK_EVENT_CREATE, Security.connected(), ne.eventType.name + " (" + ne.subject + ")").save();
        } catch (Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        } finally {
            if (Validation.hasErrors()) {
                JsonObject jsonObj = new JsonObject();
                response.status = Http.StatusCode.BAD_REQUEST;
                for (play.data.validation.Error error : Validation.errors()) {
                    jsonObj.addProperty(error.getKey(), error.message());
                }
                Gson gson = new GsonBuilder().create();
                renderJSON(gson.toJson(jsonObj));
            }
        }
    }

    public static void list(int sEcho, String sSearch, int iDisplayStart,
            int iDisplayLength, int iSortCol_0, String sSortDir_0) {
        if (iDisplayLength == 0) {
            iDisplayLength = 10;
        }

        String sortingCol = "ne.subject";
        if (iSortCol_0 == 1) {
            sortingCol = "ne.eventType.name";
        } else if (iSortCol_0 == 3) {
            sortingCol = "ne.creator.lastname";
        } else if (iSortCol_0 == 4) {
            sortingCol = "ne.createdOn";
        }

        long iTotalRecords = NetworkEvent.count();

        //Count number of Webusers matching the filter
        Query q = JPA.em().createQuery(
                "SELECT COUNT(ne) FROM NetworkEvent ne WHERE ne.subject LIKE :subject OR ne.description LIKE :description OR ne.eventType.name LIKE :eventType OR ne.creator.firstname LIKE :firstname OR ne.creator.lastname LIKE :lastname");
        q.setParameter("subject", "%" + sSearch + "%");
        q.setParameter("description", "%" + sSearch + "%");
        q.setParameter("eventType", "%" + sSearch + "%");
        q.setParameter("firstname", "%" + sSearch + "%");
        q.setParameter("lastname", "%" + sSearch + "%");

        long iTotalDisplayRecords = (Long) q.getSingleResult();

        StringBuilder qString = new StringBuilder(
                "SELECT ne FROM NetworkEvent ne WHERE ne.subject LIKE :subject OR ne.description LIKE :description OR ne.eventType.name LIKE :eventType OR ne.creator.firstname LIKE :firstname OR ne.creator.lastname LIKE :lastname ORDER BY ");
        qString.append(sortingCol);
        qString.append(" ");
        qString.append(sSortDir_0);
        q = JPA.em().createQuery(qString.toString());
        q.setParameter("subject", "%" + sSearch + "%");
        q.setParameter("description", "%" + sSearch + "%");
        q.setParameter("eventType", "%" + sSearch + "%");
        q.setParameter("firstname", "%" + sSearch + "%");
        q.setParameter("lastname", "%" + sSearch + "%");
        q.setFirstResult(iDisplayStart);
        q.setMaxResults(iDisplayLength);

        List<NetworkEvent> events = q.getResultList();

        PaginatedResult results = new PaginatedResult(sEcho, iTotalRecords,
                iTotalDisplayRecords, events);
        renderJSON(NetworkEvent.createSerializer().serialize(results));
    }

    public static void delete(Long id) {
        Gson gson = new GsonBuilder().create();
        try {
            NetworkEvent ne = NetworkEvent.findById(id);
            if (ne != null) {
                ne.delete();
                AuditEvent.create(AuditEvent.Type.NETWORK_EVENT_DELETE, Security.connected(), ne.eventType.name + " (" + ne.subject + ")").save();
            } else {
                response.status = Http.StatusCode.NOT_FOUND;
                renderJSON(gson.toJson(new controllers.error.Error(
                        ErrorTypes.NETWORK_EVENT_NOT_FOUND)));
            }
        } catch (Exception e) {
            Logger.error("Exception: %s", e.getMessage());
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(gson.toJson(new controllers.error.Error(
                    ErrorTypes.ERROR_DELETING_NETWORK_EVENT)));
        }
    }
    
    public static void edit(Long id) {
        JsonObject eventJson = new JsonParser().parse(params.get("body")).getAsJsonObject();
        try {
            NetworkEvent ne = NetworkEvent.findById(id);
            ne.subject = eventJson.get("subject").getAsString();
            ne.description = eventJson.get("description").getAsString();
            ne.eventType = NetworkEventType.findByKey(eventJson.get("event-type").getAsString());
            ne.eventPeriod = new Interval(new DateTime(eventJson.get("start-date").getAsString()), new DateTime(eventJson.get("end-date").getAsString()));
            JsonArray jsonSites = eventJson.get("sites").getAsJsonArray();
            List<Site> sites = new ArrayList<Site>();
            for (JsonElement jsonSite : jsonSites) {
                sites.add(Site.findByKey(jsonSite.getAsString()));
            }
            ne.sites = sites;
            ne.validateAndSave();
        } catch (Exception e) {
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        } finally {
            if (Validation.hasErrors()) {
                JsonObject jsonObj = new JsonObject();
                response.status = Http.StatusCode.BAD_REQUEST;
                for (play.data.validation.Error error : Validation.errors()) {
                    jsonObj.addProperty(error.getKey(), error.message());
                }
                Gson gson = new GsonBuilder().create();
                renderJSON(gson.toJson(jsonObj));
            }
        }
    }

    public static void getAll(String sort) {
        renderJSON(NetworkEvent.createSerializer().serialize(NetworkEvent.findAll(sort)));
    }
    
    public static void getAllForDate(@As("yyyy-MM-dd'T'HH:mm:ss.'000Z'") DateTime date, String sort) {
        if (sort == null) { //if url didn't contain ?sort=, then assign a value
            sort = "date";
        }
        renderJSON(NetworkEvent.createSerializer().serialize(NetworkEvent.findByDate(date, sort)));
    }
}
