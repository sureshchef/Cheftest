package networkevents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import models.NetworkEvent;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import util.BaseFunctionalTest;

public class SitesTest extends BaseFunctionalTest {

    @Test
    public void secured() {
        clearCookies();
        Response response = GET("/events");
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/login", response);
    }
    
    @Test
    public void testFindAll() {
        Response response = GET("/api/sites.json");
        assertIsOk(response);
        assertEquals(3, getNumberofSitesFromResponse(response));
    }

    @Test
    public void testGetForEvent() {
        NetworkEvent ne1 = NetworkEvent.find("bySubject", "Net Event 1").first();
        Response response1 = GET("/api/sites/"+ne1.id);
        assertIsOk(response1);
        assertEquals(2, getNumberofSitesFromResponse(response1));
        
        NetworkEvent ne2 = NetworkEvent.find("bySubject", "Net Event 2").first();
        Response response2 = GET("/api/sites/"+ne2.id);
        assertIsOk(response2);
        assertEquals(1, getNumberofSitesFromResponse(response2));
    }
    
    @Test
    public void testFindMapSites() {
        String json = "{\"incidentPeriodStart\":\"\",\"incidentPeriodEnd\":\"\",\"blackOutPeriod\":[\"\",\"\"],\"incidentTypes\":[\"\",\"\"],\"locationTech\":[\"\",\"\"],\"position\":[\"\",\"\"],\"cellid\":\"\",\"msisdn\":\"\"}";
        Response response = POST("/api/map-sites.json?bounds=53.3,-7.1,54.3,-5.9", "application/json", json);
        assertIsOk(response);
        assertEquals(2, getNumberofSitesFromResponse(response));
    }

    private int getNumberofSitesFromResponse(Response response) {
        String content = getContent(response);
        JsonElement jsonElement = new JsonParser().parse(content);
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        return jsonArray.size();
    }
}
