package networkevents;

import com.google.gson.*;
import java.util.ArrayList;
import java.util.List;
import models.NetworkEvent;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import util.BaseFunctionalTest;

public class NetworkEventsTest extends BaseFunctionalTest {

    @Before
    public void extraSetUp() {
        //adds interval dates to the network events as I don't know how to add them via the yml file
        DateTime d = new DateTime().withTime(0, 0, 0, 0);
        List<Interval> intervals = new ArrayList<Interval>();
        intervals.add(new Interval(d.withDate(2013, 2, 8), d.withDate(2013, 2, 18)));
        intervals.add(new Interval(d.withDate(2013, 2, 10), d.withDate(2013, 2, 20)));
        intervals.add(new Interval(d.withDate(2013, 2, 12), d.withDate(2013, 2, 22)));
        List<NetworkEvent> list = NetworkEvent.findAll();
        for (int i = 0; i < list.size(); i++) {
            list.get(i).eventPeriod = intervals.get(i);
            list.get(i).validateAndSave();
        }
    }
    
    @Test
    public void secured() {
        clearCookies();
        Response response = GET("/events");
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/login", response);
    }
        
    @Test
    public void testIndex() {
        Response response = GET("/events");
        assertIsOk(response);
    }

    @Test
    public void testDetail() {
        //can't do because of the joda interval stuff
        NetworkEvent ne = NetworkEvent.find("bySubject", "Net Event 1").first();
        Response response = GET("/events/"+ne.id);
        assertIsOk(response);
    }
    
    @Test
    public void testList() {
        Response response = GET(
                "/api/events.json?sEcho=1&sSearch=&iDisplayStart=0&iDisplayLength=10&iSortCol_0=0&sSortDir_0=ASC");
        assertIsOk(response);
        assertEquals(3, getNumberofEventsFromListResponse(response));
    }

    @Test
    public void testGet() {
        NetworkEvent ne = NetworkEvent.find("bySubject", "Net Event 1").first();
        Response response = GET("/api/events/"+ne.id);
        assertIsOk(response);
        NetworkEvent newNe = getNetworkEventFromResponse(response);
        assertEquals(ne.description, newNe.description);
    }
    
    @Test
    public void testGetAll() {
        Response response = GET("/api/events-list/all?sort=date");
        assertIsOk(response);
        assertEquals(3, getNumberofEventsFromResponse(response));
    }
    
    @Test
    public void testGetAllForDate() {
        //with sort parameter
        Response response = GET("/api/events-list/2013-02-11T11:00:00.000Z?sort=date");
        assertIsOk(response);
        assertEquals(2, getNumberofEventsFromResponse(response));
        
        //without sort parameter
        response = GET("/api/events-list/2013-02-11T11:00:00.000Z");
        assertIsOk(response);
        assertEquals(2, getNumberofEventsFromResponse(response));
    }

    @Test
    public void testCreate() {
        String subject = "Test Event";
        DateTime date = new DateTime();
        String json = "{\"subject\":\""+subject+"\",\"description\":\"This is the description for "+subject+"\",\"event-type\":\"outage\","
                + "\"start-date\":\""+date.minusDays(30).toString() +"\",\"end-date\":\""+date.toString() +"\","
                + "\"sites\":[\"site01\"]}";
        Response response = POST("/events", "application/json", json);
        assertIsOk(response);
        NetworkEvent ne = NetworkEvent.find("bySubject", subject).first();
        assertEquals(subject, ne.subject);
    }

    @Test
    public void testEdit() {
        DateTime date = new DateTime();
        NetworkEvent ne = NetworkEvent.find("bySubject", "Net Event 2").first();
        String json = "{\"subject\":\""+ne.subject+"\",\"description\":\""+ne.description+"\",\"event-type\":\"outage\","
                + "\"start-date\":\""+date.minusDays(30).toString() +"\",\"end-date\":\""+date.toString() +"\","
                + "\"sites\":[\"site02\"]}";
        Response response = PUT("/events/"+ne.id, "application/json", json);
        assertIsOk(response);
        ne.refresh();
        assertEquals("site02", ne.sites.get(0).key);
    }

    @Test
    public void testDelete() {
        NetworkEvent ne = NetworkEvent.find("bySubject", "Net Event 1").first();
        Response response = DELETE("/events/"+ne.id);
        assertEquals(0, NetworkEvent.count("bySubject", "Net Event 1"));
    }

    private int getNumberofEventsFromListResponse(Response response) {
        String content = getContent(response);
        JsonElement jsonElement = new JsonParser().parse(content);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("aaData");
        return jsonArray.size();
    }
    
    private int getNumberofEventsFromResponse(Response response) {
        String content = getContent(response);
        JsonElement jsonElement = new JsonParser().parse(content);
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        return jsonArray.size();
    }

    private NetworkEvent getNetworkEventFromResponse(Response response) {
        String content = getContent(response);
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = new JsonParser().parse(content);
        return gson.fromJson(jsonElement, NetworkEvent.class);
    }
}
