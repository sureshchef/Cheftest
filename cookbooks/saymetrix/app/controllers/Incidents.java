package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.RoleHolderPresent;
import controllers.error.Error;
import controllers.error.ErrorTypes;
import models.*;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.Play;
import play.cache.Cache;
import play.db.jpa.JPA;
import play.modules.excel.RenderExcel;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;
import utils.gson.adaptor.JodaDateTimeGsonAdapter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import models.enumerations.Frequency;
import models.enumerations.LocationTech;
import models.enumerations.Position;
import play.Logger;
import play.data.validation.Validation;

@With(Deadbolt.class)
@RoleHolderPresent
public class Incidents extends Controller {

    @Before
    static void setConnecteduser() {
        if (Security.isConnected()) {
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            renderArgs.put("webuser", user.firstname);
            // HACK only let viva users see viva overlays
            if(user.email.endsWith("viva.com.bh")) {
                renderArgs.put("account", "viva");
            }

            // Account Managers should only be able to see their own accounts
            if("kam".equals(user.role.name)) {
                ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);
            }
        }
    }

    public static void index() {
    	renderArgs.put("accounts", Account.find("order by name").fetch());
    	renderArgs.put("filters", Filter.findAll());
    	renderArgs.put("incidentTypes", IncidentType.findAll(true));
        renderArgs.put("networkEventTypes", NetworkEventType.findAll(true));
        DateTime endDate = new DateTime();
        DateTime startDate = endDate.minusMonths(3);
    	render(endDate, startDate);
    }
    
    public static void redirectToIndex() {
        index();
    }
    
    public static void deeplinkFilter(String filterId, String from) {
        //filterId is taken as a string so that any value can be excepted.
        renderArgs.put("filterId", filterId);
        renderArgs.put("filterFromDate", from);
    	renderArgs.put("accounts", Account.find("order by name").fetch());
    	renderArgs.put("filters", Filter.findAll());
    	renderArgs.put("incidentTypes", IncidentType.findAll(true));
        renderArgs.put("networkEventTypes", NetworkEventType.findAll(true));
        DateTime endDate = new DateTime();
        DateTime startDate = endDate.minusMonths(3);
    	renderTemplate("Incidents/index.html", endDate, startDate);
    }

    public static void findAll() {
        List<Incident> incidentList = Incident.findAll();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().
                registerTypeAdapter(DateTime.class,
                        new JodaDateTimeGsonAdapter()).create();
        renderJSON(gson.toJson(incidentList));
    }

    public static void find() {
        Filter filter = Filters.parseFilterFromRequestBody();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().
                registerTypeAdapter(DateTime.class,
                        new JodaDateTimeGsonAdapter()).create();
        renderJSON(gson.toJson(Incident.find(filter, "date", "DESC", "")));
    }

    public static void findPaginatedIncidents(int page, int start, int limit, String sort, String dir) {
        //page is the page of incidents to return
        //start is the id to start on
        //limit is the no. of items to return
        //sort is the name of the attribute to sort by
        //dir is the direction to sort by
        
        Filter filter = Filters.parseFilterFromRequestBody();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().
                registerTypeAdapter(DateTime.class,
                new JodaDateTimeGsonAdapter()).create();
        
        //Ext expects a particular pattern to the response.
        HashMap all = new HashMap();
        all.put("success", true);
        all.put("totalCount", Incident.find(filter).size());
        all.put("incidents", Incident.find(filter, sort, dir, start, limit));
        renderJSON(gson.toJson(all));
        
    }
    
    public static void findPageNumberWithIncident(int limit, String sort, String dir, Incident incident) {
        Filter filter = Filters.parseFilterFromRequestBody();
        List<Incident> allIncidents = Incident.find(filter, sort, dir, "");
        int incidentPosition = allIncidents.indexOf(incident);
        int pageNumber = new BigDecimal(incidentPosition).divideToIntegralValue(new BigDecimal(limit)).intValue();
        pageNumber += 1;//page number is one based
        HashMap pageNumMap = new HashMap();
        pageNumMap.put("pagenum", pageNumber);
        renderJSON(new GsonBuilder().create().toJson(pageNumMap));
    }
    
    public static void findMapIncidents(String bounds) {
        Filter filter = Filters.parseFilterFromRequestBody();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().
                registerTypeAdapter(DateTime.class,
                new JodaDateTimeGsonAdapter()).create();
        
        List<Incident> requestedIncidents = Incident.find(filter, "date", "DESC", bounds);
        renderJSON(gson.toJson(requestedIncidents));
    }

    /**
     * 2 Steps are required to export the Incidents to Excel as the Ajax request
     * was overriding the default browser behaviour. STEP 1: The filter is saved
     * in the Cache.
     */
    public static void exportIncidentsFilterPost(String filterId) {
        Filter filter = Filters.parseFilterFromRequestBody();
        Cache.set("filterData_" + filterId, filter, "1mn");
    }

    /**
     * STEP 2: The filter is taken from the Cache, the incidents are gathered
     * and are rendered in the Excel file
     */
    public static void exportIncidentsFileDownload(String filterId) {
        Filter filter = Cache.get("filterData_" + filterId, Filter.class);
        if (filter != null) {
            Cache.delete("filterData_" + filterId);
            request.format = "xlsx";
            response.setContentTypeIfNotSet("application/vnd.ms-excel");
            renderArgs.put(RenderExcel.RA_ASYNC, true);
            DateTime now = DateTime.now();
            renderArgs.put(RenderExcel.RA_FILENAME,
                    now.toString(DateTimeFormat.forPattern("yyyyMMddHHmm")) + "_incidents.xlsx");
            List<Incident> incidents = Incident.find(filter, "date", "DESC", "");
            renderArgs.put("date", now);
            renderArgs.put("headerDate", DateTimeFormat.forPattern("dd/MM/y").print(now));
            renderArgs.put("incidents", incidents);
            // dateFormat enables the DateTime from the incident to be evaluated in the template
            DateTimeFormatter dateFormat = DateTimeFormat.forPattern(
                    "dd/MM/y HH:mm");
            renderArgs.put("dateFormat", dateFormat);

            // Which brand should be shown in the Excel file
            String template = "Incidents/incidents_saym.xlsx";
            if(Play.id.contains("vfie")) {
                template = "Incidents/incidents_vfie.xlsx";
            }
            response.setHeader("Cache-Control", "max-age=0");
            renderTemplate(template, renderArgs);
        } else {
            response.status = Http.StatusCode.BAD_REQUEST;
            Gson gson = new GsonBuilder().create();
            renderJSON(gson.toJson(new Error(
                    ErrorTypes.ERROR_GENERATING_INCIDENT_REPORT)));
        }
    }

    public static void totalIncidents() {
        Gson gson = new GsonBuilder().create();
        renderJSON(gson.toJson(Incident.count()));
        //renderJSON(gson.toJson(783980));  //Leaving this line in so its easy to quickly see if the counter still works
    }
    
    public static void create() {
        JsonObject incidentJson = new JsonParser().parse(params.get("body")).getAsJsonObject();
        Incident i = new Incident();
        try {
            i.subscriber = MobileSubscriber.find(incidentJson.get("msisdn").getAsString());
            i.date = DateTime.parse(incidentJson.get("date").getAsString());
            i.incidentType = IncidentType.findByKey(incidentJson.get("incidentType").getAsString());
            i.frequency = Frequency.valueOf(incidentJson.get("frequency").getAsString());
            i.position = Position.valueOf(incidentJson.get("position").getAsString());
            i.latitude = incidentJson.get("lat").getAsDouble();
            i.longitude = incidentJson.get("lng").getAsDouble();
            
            //optional fields
            i.comment = incidentJson.get("description").getAsString();
            i.phoneType = incidentJson.get("phoneType").getAsString();
            i.phoneOs = incidentJson.get("phoneOs").getAsString();
            i.networkEvent = NetworkEvent.findById(incidentJson.get("eventId").getAsLong());
            
            //default values that we'll set (not needed for this incident type)
            //lac and accuracy can't be null values
            i.lac = 0;
            i.accuracy = 0;
            //imsi, imei, cellId can be null
            
            i.locationTech = LocationTech.MANUAL;
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            i.reporter = user;
            i.source = 2;   //2 = "web"     1 = "SayMetrix/SmartFeedback App"
            
            i.validateAndCreate();
            renderJSON(i.id);
        } catch (Exception e) {
            Logger.info(e.toString());
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

    public static void get(long id) {
        Incident i = Incident.findById(id);
        if (i != null) {
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().registerTypeAdapter(DateTime.class, new JodaDateTimeGsonAdapter()).create();
            renderJSON(gson.toJson(i));
        }
        response.status = Http.StatusCode.NOT_FOUND;
    }
    
    public static void edit(long oldId) {
        JsonObject incidentJson = new JsonParser().parse(params.get("body")).getAsJsonObject();
        try {
            Incident i = Incident.findById(oldId);
            DateTime newDate = DateTime.parse(incidentJson.get("date").getAsString());
            if (!i.date.equals(newDate) && i.networkEvent != null) {
                if (!i.networkEvent.eventPeriod.contains(newDate)) {
                    i.networkEvent = null;
                }
            }
            i.date = newDate;
            i.incidentType = IncidentType.findByKey(incidentJson.get("incidentType").getAsString());
            i.frequency = Frequency.valueOf(incidentJson.get("frequency").getAsString());
            i.position = Position.valueOf(incidentJson.get("position").getAsString());
            i.latitude = incidentJson.get("lat").getAsDouble();
            i.longitude = incidentJson.get("lng").getAsDouble();
            
            //optional fields
            i.comment = incidentJson.get("description").getAsString();
            i.phoneType = incidentJson.get("phoneType").getAsString();
            i.phoneOs = incidentJson.get("phoneOs").getAsString();
            i.networkEvent = NetworkEvent.findById(incidentJson.get("eventId").getAsLong());
            
            i.validateAndSave();
        } catch (Exception e) {
            Logger.error(e, "Error validating and saving incident.");
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new controllers.error.Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        } finally {
            if (Validation.hasErrors()) {
                response.status = Http.StatusCode.BAD_REQUEST;
                JsonObject jsonObj = new JsonObject();
                for (play.data.validation.Error error : Validation.errors()) {
                    jsonObj.addProperty(error.getKey(), error.message());
                }
                renderJSON(new GsonBuilder().create().toJson(jsonObj));
            }
        }
    }

    public static void associateWithNetworkEvent() {
        JsonObject json = new JsonParser().parse(params.get("body")).getAsJsonObject();
        if (json.get("eventId") != null) {
            try {
                Incident i = Incident.findById(json.get("incidentId").getAsLong());
                i.networkEvent = NetworkEvent.findById(json.get("eventId").getAsLong());
                i.validateAndSave();
            } catch (Exception e) {
                Logger.info(e.toString());
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
    }
}
