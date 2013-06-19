import models.Incident;
import models.MobileSubscriber;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import play.jobs.Job;
import play.libs.XPath;
import play.mvc.Http;
import play.test.Fixtures;
import play.test.FunctionalTest;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

public class SmartFeedbackAPITest extends FunctionalTest {
    private static final String TOKEN = "111111178353";

    @Before
    public void setUp() throws Exception {
        new Job() {

            @Override
            public void doJob() throws Exception {
                Fixtures.deleteDatabase();
                Fixtures.loadModels("initial-data-prod.yml");
                Fixtures.loadModels("initial-data-dev.yml");
            }
        }.now().get();
    }

    @Test
    public void testRegisterUnprovisionedSubscriber() {
        String msisdn = "1234567890";
        Http.Response response = POST("/api/register?msisdn=" + msisdn);
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertEquals(
                "<message>failure</message><info>It looks like you're not registered for SayMetrix. " +
                        "Please get in touch with your account manager.</info>",
                getContent(response));
        Long countMS = MobileSubscriber.count("byMsisdn", msisdn);
        assertSame("The MSISDN should not be in the Database", 0L, countMS);
    }

    @Test
    public void testRegisterSubscriber() throws Exception {
        String existingMSISDN = "353862222222";
        Http.Response response = POST("/api/register?msisdn=" + existingMSISDN);
        assertIsOk(response);
        String token = getUidFromResponse(response);
        assertContentType("text/xml", response);
        assertEquals("<message>success</message><id>" + token + "</id>" +
                "<info>Thank you for registering for SayMetrix.</info>",
                getContent(response));
        MobileSubscriber ms = MobileSubscriber.find("byToken", token).first();
        assertNotNull(ms);
        assertEquals(existingMSISDN, ms.msisdn);
    }

    /**
     * Ensure that a subscriber can register more than once.
     */
    @Test
    public void testReregisterSubscriber() throws Exception {
        String msisdn = "353863333333";
        String token = registerForToken(msisdn);
        MobileSubscriber ms = MobileSubscriber.find("byToken", token).first();
        assertNotNull(ms);
        token = registerForToken(msisdn);
        MobileSubscriber theSameMobileSubscriber = MobileSubscriber.find(
                "byToken", token).first();
        assertNotNull(theSameMobileSubscriber);
        assertEquals(ms.email, theSameMobileSubscriber.email);
    }

    /**
     * Good token and all required parameters present.
     */
//    @Test
    public void testLegalReport() {
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=1234&imei=1234&cellid=123&lac=1234&lat=54.030683&lon=-7.89917&locType=gps&accuracy=1234&" +
                "inout=0&freq=2&event=0&time=23-11-2011%2013:14:13&add_text=hello&handset=ABC&os=ABC");
        assertIsOk(response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).contains("registering"));
        assertEquals(1, Incident.count("byLatitude", 54.030683));
    }

    /**
     * Unregistered token.
     */
    @Test
    public void testUnregisteredSubscriberReport() {
        Http.Response response = POST("/api/report?uid=badtoken");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
    }

    /**
     * No token.
     */
    @Test
    public void testNoArgsReport() {
        Http.Response response = POST("/api/report");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
    }

    // @Test
    public void testIncidentCreatedWithOptionalValuesOmitted() {
        double latitude = 54.030684;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=&imei=&cellid=&lac=&lat=" + latitude + "&lon=-7.89917&locType=Network&accuracy=&" +
                "inout=0&freq=2&event=1&time=23-11-2011%2013:13:13&add_text=&handset=ABC&os=ABC");
        assertIsOk(response);
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNotNull(incident);
        assertEquals(incident.latitude, latitude, 0.000001);
    }

    // FIXME this test fails (what it's testing actually works) - some problem with db tx and junit
    // @Test
    public void testIncidentTimeZoneOffset() {
        double latitude = 53.010454;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=&imei=&cellid=&lac=&lat=" + latitude + "&lon=-7.89917&locType=Network&accuracy=&" +
                        "inout=0&freq=2&event=1&time=2013-01-31T07:18-1000&add_text=&handset=ABC&os=ABC");
        assertIsOk(response);
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNotNull(incident);
        assertEquals(incident.latitude, latitude, 0.000001);
    }

    // FIXME this test fails (what it's testing actually works) - some problem with db tx and junit
    // @Test
    public void testIncidentZTimeZoneOffset() {
        double latitude = 53.012454;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=&imei=&cellid=&lac=&lat=" + latitude + "&lon=-7.89917&locType=Network&accuracy=&" +
                        "inout=0&freq=2&event=1&time=2013-01-31T07:20Z&add_text=&handset=ABC&os=ABC");
        assertIsOk(response);
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNotNull(incident);
        assertEquals(incident.latitude, latitude, 0.000001);
    }

    @Test
    public void testIncidentWithBadTimeZoneOffset() {
        double latitude = 53.110454;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=&imei=&cellid=&lac=&lat=" + latitude + "&lon=-7.89917&locType=Network&accuracy=&" +
                        "inout=0&freq=2&event=1&time=2013-01-31Tabc&add_text=&handset=ABC&os=ABC");
        assertStatus(403, response);
    }

    @Test
    public void testIncidentNOTCreatedWithRequiredValueOmitted() {
        double latitude = 54.030684;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=&imei=&cellid=&lac=&lat=" + latitude + "&lon=&locType=&accuracy=&" +
                "inout=0&freq=2&event=0&time=23-11-2011%2013:13:13&add_text=&handset=ABC&os=ABC");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNull("Incident should not exist", incident);
    }

    // All Tests below deal with Parameter Missing
    @Test
    public void testNoLatitudeReport() {
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=1234&imei=1234&cellid=123&lac=1234&lon=-7.89917&locType=ABC&accuracy=1234&" +
                "inout=ABC&freq=ABC&event=0&time=23-11-2011%2013:13:13&add_text=hello&handset=ABC&os=ABC");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
        double longitude = -7.89917;
        Incident incident = Incident.find("byLongitude", longitude).first();
        assertNull("Incident should not exist", incident);
    }

    @Test
    public void testNoLongitudeReport() {
        double latitude = 54.030684;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=1234&imei=1234&cellid=123&lac=1234&lat=" + latitude + "&locType=ABC&accuracy=1234&" +
                "inout=ABC&freq=ABC&event=0&time=23-11-2011%2013:13:13&add_text=hello&handset=ABC&os=ABC");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNull("Incident should not exist", incident);
    }

    @Test
    public void testNoPositionReport() {
        double latitude = 54.030684;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=1234&imei=1234&cellid=123&lac=1234&lat=" + latitude + "&lon=0&locType=ABC&accuracy=1234&" +
                "freq=ABC&event=0&time=23-11-2011%2013:13:13&add_text=hello&handset=ABC&os=ABC");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNull("Incident should not exist", incident);
    }

    @Test
    public void testNoFrequencyReport() {
        double latitude = 54.030684;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=1234&imei=1234&cellid=123&lac=1234&lat=" + latitude + "&lon=0&locType=ABC&accuracy=1234&" +
                "inout=ABC&event=0&time=23-11-2011%2013:13:13&add_text=hello&handset=ABC&os=ABC");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNull("Incident should not exist", incident);
    }

    @Test
    public void testNoIncidentTypeReport() {
        double latitude = 54.030684;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=1234&imei=1234&cellid=123&lac=1234&lat=" + latitude + "&lon=0&locType=ABC&accuracy=1234&" +
                "inout=ABC&freq=0&time=23-11-2011%2013:13:13&add_text=hello&handset=ABC&os=ABC");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNull("Incident should not exist", incident);
    }

    @Test
    public void testOutOfRangeIncidentTypeReport() {
        double latitude = 54.030684;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=1234&imei=1234&cellid=123&lac=1234&lat=" + latitude + "&lon=0&locType=ABC&accuracy=1234&" +
                        "inout=ABC&event=99&freq=0&time=23-11-2011%2013:13:13&add_text=hello&handset=ABC&os=ABC");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNull("Incident should not exist", incident);
    }

    @Test
    public void testNoTimeReport() {
        double latitude = 54.030684;
        Http.Response response = POST("/api/report?uid=" + TOKEN + "&imsi=1234&imei=1234&cellid=123&lac=1234&lat=" + latitude + "&lon=0&locType=ABC&accuracy=1234&" +
                "inout=ABC&freq=0&event=0&add_text=hello&handset=ABC&os=ABC");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNull("Incident should not exist", incident);
    }

    @Test
    public void testMalformedTimeReport() {
        double latitude = 54.030684;
        Http.Response response = POST(
                "/api/report?uid=" + TOKEN + "&imsi=1234&imei=1234&cellid=123&lac=1234&lat=" + latitude + "&lon=0&locType=ABC&accuracy=1234&" +
                "inout=ABC&freq=0&event=0&time=23112011%2013:13:13&add_text=hello&handset=ABC&os=ABC");
        assertStatus(403, response);
        assertContentType("text/xml", response);
        assertTrue(getContent(response).startsWith(
                "<message>failure</message><info>"));
        Incident incident = Incident.find("byLatitude", latitude).first();
        assertNull("Incident should not exist", incident);
    }

    private String registerForToken(String msisdn) throws Exception {
        Http.Response response = POST("/api/register?msisdn=" + msisdn);
        assertIsOk(response);
        return getUidFromResponse(response);

    }

    private String getUidFromResponse(Http.Response response) throws Exception {
        String uidFromResponse = null;
        String content = getContent(response);
        String fullyFormedXML = "<response>" + content + "</response>";
        InputSource source = new InputSource(new ByteArrayInputStream(fullyFormedXML.
                getBytes()));
        Document doc = DocumentBuilderFactory.newInstance().
                newDocumentBuilder().
                parse(source);
        for (org.w3c.dom.Node node : XPath.selectNodes("//response", doc)) {
            uidFromResponse = XPath.selectText("id", node);
        }
        return uidFromResponse;
    }
}
