import com.google.gson.*;
import models.MobileSubscriber;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import util.BaseFunctionalTest;

public class SubscribersTest extends BaseFunctionalTest {

    @Test
    public void secured() {
        clearCookies();
        Response response = GET("/subscribers");
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/login", response);
    }

    @Test
    public void index() {
        Response response = GET("/subscribers");
        assertIsOk(response);
    }

    @Test
    public void dataTablesSubscriberList() {
        Response response = GET(
                "/api/subscribers.json?sEcho=1&sSearch=&iDisplayStart=1&iDisplayLength=10&iSortCol_0=0&sSortDir_0=ASC");
        assertIsOk(response);
        JsonArray json = getArrayOfSubscribersFromResponse(
                response);
        assertEquals(MobileSubscriber.count(), json.size());
    }

    @Test
    public void dataTablesSubscriberListSortedByAccount() {
        Http.Request request = newRequest();
        request.format = "json";
        Response response = GET(request, "/api/subscribers.json?sEcho=1&sSearch=&iDisplayStart=0&iDisplayLength=10&iSortCol_0=3&sSortDir_0=ASC");
        assertIsOk(response);
        JsonArray jsonArray = getArrayOfSubscribersFromResponse(response);
        MobileSubscriber subscriber = getSubscriberFromJsonElement(jsonArray.get(jsonArray.size()-1));
        assertTrue(subscriber.account.key.equalsIgnoreCase("ACCT3"));
    }

    @Test
    public void dataTablesSubscriberListSortedByPhone() {
        Http.Request request = newRequest();
        request.format = "json";
        Response response = GET(request, "/api/subscribers.json?sEcho=1&sSearch=&iDisplayStart=0&iDisplayLength=10&iSortCol_0=2&sSortDir_0=ASC");
        assertIsOk(response);
        JsonArray json = getArrayOfSubscribersFromResponse(response);
        MobileSubscriber subscriber = getSubscriberFromJsonElement(json.get(0));
        assertEquals("353862222222", subscriber.msisdn);
    }

    @Test
    public void subscriberCreate() {
        String email = "test@create.com";
        Response response = POST(
                "/subscribers?email=" + email + "&firstname=FirstnameTest&lastname=LastNameTest&msisdn=086222223&imsi=086222223223&account=ACCT1");
        assertIsOk(response);
        assertEquals(1, MobileSubscriber.count("byEmail", email));
    }

    @Test
    public void subscriberCreateNoEmail() {
        Response response = POST(
                "/subscribers?email=&firstname=FirstnameTest&lastname=LastNameTest&msisdn=0862222226&imsi=086222223223&account=TPZ");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertEquals(0, MobileSubscriber.count("byMsisdn", "0862222226"));
    }

    @Test
    public void subscriberCreateNoNames() {
        String email = "test@createmissingname.com";
        Response response = POST(
                "/subscribers?email=" + email + "&firstname=&lastname=&msisdn=0862222227&imsi=086222223223&account=TPZ");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertEquals(0L, MobileSubscriber.count("byEmail", email));
    }

    @Test
    public void subscriberCreateNoAccount() {
        String email = "test@createmissingaccount.com";
        Response response = POST(
                "/subscribers?email=" + email + "&firstname=FirstnameTest&lastname=LastNameTest&msisdn=086222223&imsi=086222223223&account=");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertEquals(0, MobileSubscriber.count("byEmail", email));
    }

    @Test
    public void subscriberCreateNonExistentAccount() {
        String email = "test@createinvaldaccount.com";
        Response response = POST(
                "/subscribers?email=" + email + "&firstname=FirstnameTest&lastname=LastNameTest&msisdn=086222223&imsi=086222223223&account=NOODLE");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertEquals(0, MobileSubscriber.count("byEmail", email));
    }

    @Test
    public void subscriberCreateInvalidEmail() {
        String email = "invalidemaildotcom";
        Response response = POST(
                "/subscribers?email=" + email + "&firstname=FirstnameTest&lastname=LastNameTest&msisdn=086222223&imsi=086222223223&account=TPZ");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);

        assertTrue(checkResponseContainsErrorFor("s.email", response));
        assertEquals(0, MobileSubscriber.count("byEmail", email));
    }

    @Test
    public void subscriberCreateDuplicateEmail() {
        String email = "sub.one@foo.com";
        Response response = POST(
                "/subscribers?email=" + email + "&firstname=FirstnameTest&lastname=LastNameTest&msisdn=086222223&imsi=086222223223&account=ACCT1");
        assertIsOk(response);
        assertEquals(2, MobileSubscriber.count("byEmail", email));
    }

    @Test
    public void subscriberCreateDuplicatePhone() {
        String email = "sub.test@foo.com";
        Response response = POST(
                "/subscribers?email=" + email + "&firstname=FirstnameTest&lastname=LastNameTest&msisdn=353863333333&imsi=086222223223&account=ACCT1");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(checkResponseContainsErrorFor("s.msisdn", response));
        assertEquals(0, MobileSubscriber.count("byEmail", email));
    }

    @Test
    public void subscriberCreateInvalidPhone() {
        String emailAddress = "sub.test@foo.com";
        Response response = POST(
                "/subscribers?email=" + emailAddress + "&firstname=FirstnameTest&lastname=LastNameTest&msisdn=08622qwer&imsi=086222223223&account=ACCT1");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(checkResponseContainsErrorFor("s.msisdn", response));
        assertEquals(0, MobileSubscriber.count("byEmail", emailAddress));
    }

    @Test
    public void subscriberCreateDuplicateImsi() {
        String email = "sub.test@foo.com";
        Response response = POST(
                "/subscribers?email=" + email + "&firstname=FirstnameTest&lastname=LastNameTest&msisdn=08633333234&imsi=22222222&account=TPZ");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(checkResponseContainsErrorFor("s.imsi", response));
        assertSame(0L, MobileSubscriber.count("byEmail", email));
    }

    @Test
    public void subscriberCreateInvalidImsi() {
        String email = "test@foo.com";
        Response response = POST(
                "/subscribers?email=" + email + "&firstname=FirstnameTest&lastname=LastNameTest&msisdn=08633333234&imsi=2222qwee&account=ACCT1");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(checkResponseContainsErrorFor("s.imsi", response));
        assertEquals(0, MobileSubscriber.count("byEmail", email));
    }

    @Test
    public void subscriberEdit() {
        Response response = GET("/subscribers/353871111111");
        assertIsOk(response);
        response = PUT(
                "/subscribers/353871111111?email=sub.one.newemail@foo.com&firstname=FirstnameTest&lastname=LastNameTest&msisdn=353871111111&imsi=086222223223&account=ACCT1",
                "", "");
        assertIsOk(response);
        assertEquals(1, MobileSubscriber.count("byMsisdn", "353871111111"));
    }

    @Test
    public void subscriberEditMissingField() {
        Response response = GET("/subscribers/353871111111");
        assertIsOk(response);
        getSubscriberFromResponse(response);

        // Update the subscriber omitting the first name
        response = PUT(
                "/subscribers/353871111111?email=sub.one.newemail@foo.com&firstname=&lastname=LastNameTest&msisdn=086222223&imsi=086222223223&account=ACCT1",
                "", "");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertEquals(0, MobileSubscriber.count("byMsisdn", "086222223"));
    }

    @Test
    public void subscriberEditInvalidField() {
        Response response = GET("/subscribers/353871111111");
        assertIsOk(response);
        getSubscriberFromResponse(response);

        // Update the subscriber
        response = PUT(
                "/subscribers/353871111111?email=sub.one.newemail@foo.com&firstname=&lastname=LastNameTest&msisdn=&imsi=086222223223&account=",
                "", "");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(checkResponseContainsErrorFor("s.firstname", response));
        assertTrue(checkResponseContainsErrorFor("s.msisdn", response));
        assertTrue(checkResponseContainsErrorFor("s.account", response));
        assertEquals(0, MobileSubscriber.count("byMsisdn", "086222223"));
    }

    @Test
    public void subscriberDelete() {
        String msisdn = "353865555555";
        POST("/subscribers?email=sub.five@foo.com&firstname=FirstnameTest&lastname=LastNameTest&msisdn=086222223&imsi=086222223223&account=TPZ");
        Response response = DELETE("/subscribers/" + msisdn);
        assertIsOk(response);
        assertEquals(0, MobileSubscriber.count("byMsisdn", msisdn));
    }
    
    @Test
    public void subscriberDeleteNonExistent() {
        Response response = DELETE("/subscribers/NOODLE");
        assertIsNotFound(response);
    }

    @Test
    public void subscriberDeleteHasIncidents() {
        String msisdn = "353871111111";
        Response response = DELETE("/subscribers/" + msisdn);
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertEquals(1, MobileSubscriber.count("byMsisdn", msisdn));
    }

    private JsonArray getArrayOfSubscribersFromResponse(Http.Response response) {
        String content = getContent(response);
        JsonElement jsonElement = new JsonParser().parse(content);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        return jsonObject.getAsJsonArray("aaData");
    }

    private MobileSubscriber getSubscriberFromResponse(Response response) {
        String content = getContent(response);
        Gson gson = new GsonBuilder().create();
        JsonElement contentElement = new JsonParser().parse(content);
        return gson.fromJson(contentElement, MobileSubscriber.class);
    }

    private MobileSubscriber getSubscriberFromJsonElement(JsonElement contentElement) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(contentElement, MobileSubscriber.class);
    }

    private boolean checkResponseContainsErrorFor(String errorName,
                                                  Response response) {
        String content = getContent(response);
        JsonObject jsonObject = new JsonParser().parse(content).getAsJsonObject();

        return jsonObject.has(errorName);
    }
}
