package incidents;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import models.Account;
import models.Incident;
import org.joda.time.DateTime;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import util.BaseFunctionalTest;
import utils.gson.adaptor.JodaDateTimeGsonAdapter;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import models.IncidentType;
import models.enumerations.Position;

public class IncidentsTest extends BaseFunctionalTest {
    private static final int INCIDENT_COUNT = 15;

    @Test
    public void testFindAll() {
        Http.Response response = GET("/api/incidents.json");
        assertIsOk(response);
        Collection<Incident> incidents = parseIncidentsFromResponse(response);
        assertEquals(INCIDENT_COUNT, incidents.size());
    }

    @Test
    public void testIncidentFilter() {
        String json = "{\"incidentPeriodStart\":\"\",\"incidentPeriodEnd\":\"\",\"blackOutPeriod\":[\"\",\"\"],\"incidentTypes\":[\"voice_poor_sound\",\"data_slow_connection\"],\"locationTech\":[\"GPS\",\"NETWORK\"],\"position\":[\"INDOOR\",\"OUTDOOR\"],\"cellid\":\"\",\"msisdn\":\"\"}";
        Response response = POST("/api/incidents.json", "application/json", json);
        assertIsOk(response);
        Collection<Incident> incidents = parseIncidentsFromResponse(response);
        assertEquals(1, incidents.size());
    }

    @Test
    public void testFilterCanHandleEmptyArrays() {
        Account account = Account.find("byKey", "ACCT1").first();
        String json = "{\"incidentPeriodStart\":\"\",\"incidentPeriodEnd\":\"\",\"blackOutPeriod\":[\"\",\"\"],\"incidentTypes\":[],\"locationTech\":[],\"position\":[],\"frequency\":[],\"cellid\":\"\",\"msisdn\":\"\",\"accounts\":[\"" + account.key + "\"]}";
        Response response = POST("/api/incidents.json", "application/json", json);
        assertIsOk(response);
    }

    @Test
    public void searchByAccount() {
        Account account = Account.find("byKey", "ACCT1").first();
        String json = "{\"incidentPeriodStart\":\"\",\"incidentPeriodEnd\":\"\",\"blackOutPeriod\":[\"\",\"\"],\"accounts\":[\"" + account.key + "\"]}";
        Response response = POST("/api/incidents.json", "application/json", json);
        assertIsOk(response);
        Collection<Incident> incidents = parseIncidentsFromResponse(response);
        assertEquals(10, incidents.size());
    }

    @Test
    public void testExportFunctionality() {
        Account account = Account.find("byKey", "ACCT1").first();
        String json = "{\"incidentPeriodStart\":\"\",\"incidentPeriodEnd\":\"\",\"blackOutPeriod\":[\"\",\"\"],\"incidentTypes\":[\"\",\"\"],\"locationTech\":[\"\",\"\"],\"position\":[\"\",\"\"],\"frequency\":[\"\",\"\"],\"cellid\":\"\",\"msisdn\":\"\",\"accounts\":[\"\",\"\",\"" + account.key + "\"]}";
        double filterId = Math.floor(Math.random() * 1001);
        Response response = POST("/api/exportFilter/" + filterId, "application/json", json);
        assertIsOk(response);
        Response exportIncidentsResponse = GET("/api/exportIncidents/" + filterId);
        assertIsOk(exportIncidentsResponse);
    }

    @Test
    public void testExportDoesNotWorkWithoutFirstCallingExportFilter() {
        double filterId = Math.floor(Math.random() * 1001);
        Response exportIncidentsResponse = GET("/api/exportIncidents/" + filterId);
        assertStatus(Http.StatusCode.BAD_REQUEST, exportIncidentsResponse);
    }
    
    @Test
    public void testTotalIncidents() {
        Response totalIncidentsResponse = GET("/api/totalIncidents.json");
        assertTrue(getContent(totalIncidentsResponse).equalsIgnoreCase("15"));
    }
    
    @Test
    public void testFindPaginatedIncidents() {
        Account account = Account.find("byKey", "ACCT1").first();
        String json = "{\"incidentPeriodStart\":\"\",\"incidentPeriodEnd\":\"\",\"blackOutPeriod\":[\"\",\"\"],\"incidentTypes\":[],\"locationTech\":[],\"position\":[],\"frequency\":[],\"cellid\":\"\",\"msisdn\":\"\",\"accounts\":[\"" + account.key + "\"]}";
        Response response = POST("/api/paged-incidents.json?page=1&start=0&limit=5&sort=date&dir=DESC", "application/json", json);
        
        assertIsOk(response);
        HashMap map = parsePagedIncidentsFromResponse(response);
        Collection<Incident> incidents = (Collection<Incident>) map.get("incidents");
        assertEquals(5, incidents.size());
    }
    
    @Test
    public void testFindMapIncidentsWithBounds() {
        Account account = Account.find("byKey", "ACCT1").first();
        String json = "{\"incidentPeriodStart\":\"\",\"incidentPeriodEnd\":\"\",\"blackOutPeriod\":[\"\",\"\"],\"incidentTypes\":[],\"locationTech\":[],\"position\":[],\"frequency\":[],\"cellid\":\"\",\"msisdn\":\"\",\"accounts\":[\"" + account.key + "\"]}";
        Response response = POST("/api/map-incidents.json?bounds=53.272502,-6.288342,53.289539,-6.202254", "application/json", json);
        
        assertIsOk(response);
        Collection<Incident> incidents = parseIncidentsFromResponse(response);
        assertEquals(1, incidents.size());
    }

    @Test
    public void testCreateIncident() {
        //353871111111 = existing msisdn
        IncidentType incidentType = IncidentType.findByKey("voice_no_coverage");
        String json = "{\"msisdn\":\"353871111111\",\"date\":\"2013-02-13T15:00:00.000Z\",\"incidentType\":\"voice_no_coverage\",\"frequency\":\"ONCE\",\"position\":\"INDOOR\",\"description\":\"\",\"phoneType\":\"\",\"phoneOs\":\"\",\"eventId\":0,\"lat\":53.29302818156138,\"lng\":-6.1383748054504395}";
        Response response = POST("/incidents", "application/json", json);
        assertIsOk(response);
        Incident incident = Incident.find("bySourceAndIncidentType", 2, incidentType).first();
        assertEquals("voice_no_coverage", incident.incidentType.key);
    }
    
    @Test
    public void testCreateIncidentWithInvalidMsisdn() {
        String json = "{\"msisdn\":\"087\",\"date\":\"2013-02-12T15:52:24.000Z\",\"incidentType\":\"data_no_coverage\",\"frequency\":\"ONCE\",\"position\":\"INDOOR\",\"description\":\"\",\"phoneType\":\"\",\"phoneOs\":\"\",\"eventId\":0,\"lat\":53.29302818156138,\"lng\":-6.1383748054504395}";
        Response response = POST("/incidents", "application/json", json);
        assertStatus(Http.StatusCode.INTERNAL_ERROR, response);
        assertEquals(2, Incident.count("bySource", 2)); //the incident from the above function still exists
    }
    
    @Test
    public void testGetIncident() {
        IncidentType incidentType = IncidentType.findByKey("data_no_communication");
        Incident i = Incident.find("byIncidentTypeAndSource", incidentType, 2).first();
        Response response = GET("/api/incidents/"+i.id);
        assertIsOk(response);
        Long responseIncidentId = parseIncidentIdFromResponse(response);
        assertEquals(i.id, responseIncidentId);
    }
    
    @Test
    public void testGetIncidentWithInvalidId() {
        Long id = (long)1;
        Response response = GET("/api/incidents/"+id);
        assertStatus(Http.StatusCode.NOT_FOUND, response);
    }
    
    @Test
    public void testEditIncident() {
        IncidentType incidentType = IncidentType.findByKey("data_no_communication");
        Incident originalIncident = Incident.find("byIncidentTypeAndSource", incidentType, 2).first();
        assertEquals(Position.OUTDOOR, originalIncident.position);
        String json = "{\"date\":\"2012-10-01T10:00:00Z\",\"incidentType\":\"data_no_communication\",\"frequency\":\"ALWAYS\",\"position\":\"INDOOR\",\"description\":\"\",\"phoneType\":\"iPhone\",\"phoneOs\":\"\",\"eventId\":0,\"lat\":51.179486,\"lng\":-5.032643}";
        Response response = PUT("/incidents/"+originalIncident.id, "application/json", json);
        assertIsOk(response);
        originalIncident.refresh();
        assertEquals(Position.INDOOR, originalIncident.position);
    }
    
    private Long parseIncidentIdFromResponse(Response response) {
        String content = getContent(response);
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = new JsonParser().parse(content);
        return jsonElement.getAsJsonObject().get("id").getAsLong();
    }
    
    private Collection<Incident> parseIncidentsFromResponse(Response response) {
        Type incidentCollection = new TypeToken<Collection<Incident>>() {
        }.getType();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().
                registerTypeAdapter(DateTime.class,
                new JodaDateTimeGsonAdapter()).create();
        String content = getContent(response);
        JsonElement jsonElement = new JsonParser().parse(content);
        return gson.fromJson(jsonElement, incidentCollection);

    }

    private HashMap parsePagedIncidentsFromResponse(Response response) {
        Type map = new TypeToken<HashMap>() {
        }.getType();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().
                registerTypeAdapter(DateTime.class,
                new JodaDateTimeGsonAdapter()).create();
        String content = getContent(response);
        JsonElement jsonElement = new JsonParser().parse(content);
        return gson.fromJson(jsonElement, map);

    }
}
