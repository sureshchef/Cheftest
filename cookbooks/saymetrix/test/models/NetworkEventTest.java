package models;

import flexjson.JSONSerializer;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;

public class NetworkEventTest extends UnitTest {

    @Before
    public void setup() {
        ((Session) JPA.em().getDelegate()).disableFilter("manager");
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");

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
    public void testFind() {
        assertEquals(3, NetworkEvent.count());
    }

    @Test
    public void testJsonSerialization() {
        NetworkEvent ne = NetworkEvent.find("bySubject", "Net Event 1").first();

        JSONSerializer ser = NetworkEvent.createSerializer().prettyPrint(false);
        String expected = String.format("{\"createdOn\":{\"millis\":1359712800000},"
                + "\"creator\":{\"firstname\":\"Ciaran\",\"lastname\":\"Treanor\"},"
                + "\"description\":\"This is the description for Net Event 1\","
                + "\"eventPeriod\":{\"endMillis\":1361145600000,\"startMillis\":1360281600000},"
                + "\"eventType\":{\"key\":\"outage\",\"name\":\"outage\"},\"id\":%d,"
                + "\"numOfIncidents\":0,\"sites\":[{\"key\":\"site01\",\"latitude\":53.382029343015,\"longitude\":-6.934833048222},"
                + "{\"key\":\"site02\",\"latitude\":54.091893933135,\"longitude\":-6.193048230294}],\"subject\":\"Net Event 1\"}", ne.id);
        assertEquals(expected, ser.serialize(ne));
    }
    
    @Test
    public void testFindAll() {
        List<NetworkEvent> list = NetworkEvent.findAll("date");
        assertEquals(3, list.size());
    }
    
    @Test
    public void testFindAllSortedByIncidentCount() {
        List<NetworkEvent> list = NetworkEvent.findAll("incidents");
        assertEquals(3, list.size());
    }
    
    @Test
    public void testFindByDate() {
        List<NetworkEvent> list = NetworkEvent.findByDate(new DateTime("2013-02-11T00:00:00Z"), "date");
        assertEquals(2, list.size());
    }
    
    @Test
    public void testFindByDateSortedByIncidentCount() {
        List<NetworkEvent> list = NetworkEvent.findByDate(new DateTime("2013-02-11T00:00:00Z"), "incidents");
        assertEquals(2, list.size());
    }
}
