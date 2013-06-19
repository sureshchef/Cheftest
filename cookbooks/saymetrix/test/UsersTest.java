import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import models.ApplicationRole;
import models.WebUser;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import util.BaseFunctionalTest;

import java.lang.reflect.Type;
import java.util.List;

public class UsersTest extends BaseFunctionalTest {
    private static final Type TYPE_ROLE_LIST = new TypeToken<List<ApplicationRole>>(){}.getType();
    private static final Type TYPE_USER_LIST = new TypeToken<List<WebUser>>(){}.getType();
    private static final String USER_FIRSTNAME = "FirstnameTest";
    private static final String USER_LASTNAME = "LastnameTest";
    private static final String USER_PASSWORD = "hello";
    private static final String USER_ROLE = "admin";
    private static final String USER_EMAIL = "test@create.com";

    @Test
    public void testUsersSecured() {
        clearCookies();
        Response response = GET("/users");
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/login", response);
    }

    // FIXME maybe this should be a Selenium test
//    @Test
//    public void testLoginSuccessful() {
//        clearCookies();
//        Response response = POST(
//                "/secure/login?username=ciaran.treanor@danutech.com&password=hello");
//        assertIsOk(response);
//    }

    @Test
    public void testListUsersJson() {
        Response response = GET(
                "/api/users.json?sEcho=1&sSearch=&iDisplayStart=0&iDisplayLength=10&iSortCol_0=0&sSortDir_0=ASC");
        assertIsOk(response);
        JsonElement root = new JsonParser().parse(getContent(response));
        List<WebUser> users = new Gson().fromJson(root.getAsJsonObject().get("aaData"), TYPE_USER_LIST);

        assertEquals(6, users.size());
    }

    @Test
    public void testSortedListUsersJson() {
        Response response = GET("/api/users.json?sEcho=1&sSearch=&iDisplayStart=0&iDisplayLength=10" +
                "&iSortCol_0=2&sSortDir_0=ASC");
        assertIsOk(response);
        JsonElement root = new JsonParser().parse(getContent(response));
        List<WebUser> users = new Gson().fromJson(root.getAsJsonObject().get("aaData"), TYPE_USER_LIST);
        assertEquals(6, users.size());
        assertEquals("Admin", users.get(0).lastname);
    }

    @Test
    public void testGetAvailableRoles() {
        Response response = GET("/api/users/roles.json");
        assertIsOk(response);

        List<ApplicationRole> roles = new Gson().fromJson(response.out.toString(), TYPE_ROLE_LIST);
        assertEquals(3, roles.size());
        assertEquals("admin", roles.get(0).name);
        assertEquals("support", roles.get(1).name);
        assertEquals("kam", roles.get(2).name);
    }

    @Test
    public void testCreateUser() {
        POST(createUserCreateRequest());
        assertEquals(1, WebUser.count("byEmail", USER_EMAIL));
        WebUser user = WebUser.getByEmail("test@create.com");
        assertEquals(USER_FIRSTNAME, user.firstname);
        assertEquals(USER_LASTNAME, user.lastname);
        assertNull(user.password);
        assertNotNull(user.passwordHash);
        assertEquals(USER_ROLE, user.role.name);
    }

    private String createUserCreateRequest() {
        return createUserCreateRequest(USER_EMAIL, USER_FIRSTNAME, USER_LASTNAME,
                USER_PASSWORD, USER_ROLE);
    }

    private String createUserCreateRequest(String email, String firstname, String lastname,
                                           String password, String role) {
        return String.format("/users?user.email=%s&user.firstname=%s&user.lastname=%s" +
                "&user.password=%s&user.role.name=%s", email,firstname, lastname, password, role);
    }

    @Test
    public void testCreateUserInvalidEmail() {
        String request = createUserCreateRequest("danutech.com", USER_FIRSTNAME, USER_LASTNAME,
                USER_PASSWORD, USER_ROLE);
        Response response = POST(request);
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(response.out.toString().contains("email"));
    }

    @Test
    public void testCreateUserMissingEmail() {
        String request = createUserCreateRequest("", USER_FIRSTNAME, USER_LASTNAME,
                USER_PASSWORD, USER_ROLE);
        Response response = POST(request);
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(response.out.toString().contains("required"));
    }

    @Test
    public void testCreateUserMissingFirstname() {
        String request = createUserCreateRequest(USER_EMAIL, "", USER_LASTNAME,
                USER_PASSWORD, USER_ROLE);
        Response response = POST(request);
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(response.out.toString().contains("required"));
    }

    @Test
    public void testCreateUserMissingLastname() {
        String request = createUserCreateRequest(USER_EMAIL, USER_FIRSTNAME, "",
                USER_PASSWORD, USER_ROLE);
        Response response = POST(request);
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(response.out.toString().contains("required"));
    }

    @Test
    public void testCreateUserMissingPassword() {
        String request = createUserCreateRequest(USER_EMAIL, USER_FIRSTNAME,
                USER_LASTNAME, "", USER_ROLE);
        Response response = POST(request);
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(response.out.toString().contains("required"));
    }

    @Test
    public void testCreateUserExistingEmail() {
        String request = createUserCreateRequest("admin@danutech.com", USER_FIRSTNAME,
                USER_LASTNAME, USER_PASSWORD, USER_ROLE);
        Response response = POST(request);
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(response.out.toString().contains("in use"));
    }

//    @Test
    public void testGetUser() {
        Response response = GET(
                "/users/damien.daly@danutech.com");
        WebUser user = getWebUserFromResponse(response);
        assertEquals("damien.daly@danutech.com", user.email);
        assertEquals("Damien", user.firstname);
        assertEquals("Daly", user.lastname);
// FIXME       assertEquals("admin", user.role.key);
        assertNull(user.password);
        assertNull(user.passwordHash);
    }

    @Test
    public void testGetUnknownUser() {
        Response response = GET("/users/damien.daly@danu.com");
        assertIsNotFound(response);
        // TODO Should there be something in the JSON response to say missing resource?
    }

    @Test
    public void userDeleteAccountManager() {
        String email = "am1@operator.com";
        Response response = DELETE("/users/" + email);
        assertStatus(Http.StatusCode.FORBIDDEN, response);
        WebUser user = WebUser.find("byEmail", email).first();
        assertNotNull(user);
        assertEquals(email, user.email);
    }

    @Test
    public void testUserDelete() {
        String email = "test.daly@danutech.com";
        String request = createUserCreateRequest(email, USER_FIRSTNAME, USER_LASTNAME,
                USER_PASSWORD, USER_ROLE);
        Response response = POST(request);
        assertIsOk(response);
        assertEquals(1, WebUser.count("byEmail", email));
        response = DELETE("/users/" + email);
        assertIsOk(response);
        assertEquals(0, WebUser.count("byEmail", email));
    }

    @Test
    public void testPreventDeleteCurrentUser() {
        Response response = DELETE("/users/ciaran.treanor@danutech.com");
        assertStatus(Http.StatusCode.FORBIDDEN, response);
        // TODO Check JSON error codes
    }

    @Test
    public void testGetManagers() {
        Response response = GET("/api/managers.json");
        assertIsOk(response);
        assertNotNull(response);
        List<WebUser> managers = new Gson().fromJson(response.out.toString(), TYPE_USER_LIST);
        assertEquals(6, managers.size());
    }

//    @Test
//    public void testEditUser() {
//        String email = "damien.daly@danutech.com";
//        Response response = GET("/users/" + email);
//        assertIsOk(response);
//        WebUser user = getWebUserFromResponse(response);
//
//        String newEmail = "update.daly@danutech.com";
//
//        Response responsePut = PUT(
//                "/users/" + email + "?user.email=" + newEmail + "&user.firstname=FirstnameTest&user.lastname=LastNameTest&user.password=hello&user.role=ADMINISTRATOR",
//                "", "");
//        assertIsOk(responsePut);
//
//        assertEquals(1, WebUser.count("byEmail", newEmail));
//    }

//    @Test
//    public void testThatWebuserCannotBeEditedIfFieldMissing() {
//        String email = "damien.daly@danutech.com";
//        Response response = GET("/users/" + email);
//        assertIsOk(response);
//        WebUser webUserFromGet = getWebUserFromResponse(response);
//
//        //Edit it
//        String updateEmailAddress = "update.daly@danutech.com";
//        webUserFromGet.email = updateEmailAddress;
//
//        //Update the web user omitting the firstname
//        Response responsePut = PUT(
//                "/users/" + oldEmail + "?user.email=" + webUserFromGet.email + "&user.firstname=&user.lastname=LastNameTest&user.password=hello&user.role=ADMINISTRATOR",
//                "", "");
//        assertStatus(Http.StatusCode.BAD_REQUEST, responsePut);
//
//        //Check new emailAddress was not added to DB
//        Long result = WebUser.count("byEmail", updateEmailAddress);
//        assertSame(0L, result);
//    }
//
//    private Collection<WebUserValueObject> parseWebUsersValueObjectFromResponse(
//            Http.Response response) {
//        Type filterMetaList = new TypeToken<Collection<WebUserValueObject>>() {
//        }.getType();
//        Gson gson = new GsonBuilder().create();
//        String content = getContent(response);
//        JsonElement jsonElement = new JsonParser().parse(content);
//        return gson.fromJson(jsonElement, filterMetaList);
//
//    }
//
    private WebUser getWebUserFromResponse(Response response) {
        Gson gson = new GsonBuilder().create();
        JsonElement contentElement = new JsonParser().parse(getContent(response));
        return gson.fromJson(contentElement, WebUser.class);
    }

//    private WebUser getWebUserFromJsonElement(JsonElement contentElement) {
//        Logger.info(contentElement.getAsJsonObject().get("role").getAsJsonObject().get("name").toString());
//        Gson gson = new GsonBuilder().create();
//        WebUser user = gson.fromJson(contentElement, WebUser.class);
//
//        return user;
//    }
//
//    private String getNameOfUserRoleFromJsonElement(JsonElement contentElement) {
//        String roleName = contentElement.getAsJsonObject().get("role").getAsJsonObject().get("name").toString();
//        return roleName.substring(1, roleName.length()-1);
//    }

//    FIXME
//    private Collection<WebUserRoleValueObject> getListOfRolesFromResponse(
//            Response response) {
//        Type roleCollection = new TypeToken<Collection<WebUserRoleValueObject>>() {
//        }.getType();
//        Gson gson = new GsonBuilder().create();
//        String content = getContent(response);
//        JsonElement jsonElement = new JsonParser().parse(content);
//        return gson.fromJson(jsonElement, roleCollection);
//    }
}
