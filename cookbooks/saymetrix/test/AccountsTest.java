import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import models.Account;
import models.WebUser;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import util.BaseFunctionalTest;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class AccountsTest extends BaseFunctionalTest {

    @Test
    public void secured() {
        clearCookies();
        Response response = GET("/accounts");
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/login", response);
    }

    @Test
    public void index() {
        Response response = GET("/accounts");
        assertIsOk(response);
    }

    @Test
    public void dataTablesAccountList() {
        Response response = GET(
                "/api/accounts.dt?sEcho=1&sSearch=&iDisplayStart=1&iDisplayLength=10&iSortCol_0=0&sSortDir_0=ASC");
        assertIsOk(response);
        assertEquals(3, getNumberofAccountsFromResponse(response));
    }

    @Test
    public void dataTablesAccountListSortedByContact() {
        Response response = GET(
                "/api/accounts.dt?sEcho=1&sSearch=&iDisplayStart=0&iDisplayLength=10&iSortCol_0=3&sSortDir_0=DESC");
        assertIsOk(response);
        Collection<Account> accountsFromResponse = getPagedAccountsFromResponse(response);
        Object[] accountsArray = accountsFromResponse.toArray();
        Account lastAccount = (Account) accountsArray[0];
        assertEquals("contact@danutech.com", lastAccount.contact);
    }

    @Test
    public void accountList() {
        Response response = GET("/api/accounts/accounts.json");
        assertIsOk(response);
        Collection<Account> accountsFromResponse = getAccountsFromResponse(
                response);
        List<Account> accounts = Account.findAll();
        assertSame(accounts.size(), accountsFromResponse.size());
    }

    @Test
    public void accountCreate() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Long managerId = u.id;
        POST("/accounts?account.key=AAAA&account.name=AAAA%20Inc&account.manager.id=" + managerId + "&account.contact=a@abcd.ie");
        assertSame(Account.count("byKey", "AAAA"), 1L);
    }

    @Test
    public void accountCreateNoContact() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Long managerId = u.id;
        POST("/accounts?account.key=BBBB&account.name=BBBB%20Inc&account.manager.id=" + managerId + "&account.contact=a@abcd.ie");
        assertSame(Account.count("byKey", "BBBB"), 1L);
    }

    @Test
    public void accountCreateShortKey() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Response response = POST(
                "/accounts?account.key=AA&account.name=AAAA%20Inc&account.manager.key=" + u.id + "&account.contact=a@abcd.ie");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
    }

    @Test
    public void accountCreateShortName() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Response response = POST(
                "/accounts?account.key=CCCC&account.name=A&account.manager.id=" + u.id + "&account.contact=a@abcd.ie");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
    }

    @Test
    public void accountCreateInvalidEmail() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Response response = POST(
                "/accounts?account.key=DDDD&account.name=DDDD%20Inc&account.manager.id=" + u.id + "&account.contact=dink");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
    }

    @Test
    public void accountCreateNoKey() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Response response = POST(
                "/accounts?account.key=&account.name=EEEE%20Inc&account.manager.id=" + u.id + "&account.contact=a@abcd.ie");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
    }

    @Test
    public void accountCreateNoName() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Response response = POST(
                "/accounts?account.key=FFFF&account.name=&account.manager.id=" + u.id + "&account.contact=a@abcd.ie");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
    }

    @Test
    public void accountCreateNoManager() {
        Response response = POST(
                "/accounts?account.key=GGGG&account.name=GGGG%20Inc&account.manager.id=&account.contact=a@abcd.ie");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
    }

    @Test
    public void accountCreateDuplicateKey() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Response response = POST(
                "/accounts?account.key=ACCT1&account.name=HHHH%20Inc&account.manager.id=" + u.id + "&account.contact=a@abcd.ie");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
    }

    @Test
    public void accountCreateDuplicateName() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Response response = POST(
                "/accounts?account.key=ACCT1&account.name=Account%201&account.manager.id=" + u.id + "&account.contact=a@abcd.ie");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
    }

    @Test
    public void accountGet() {
        Response response = GET("/accounts/ACCT1");
        assertIsOk(response);
        Account a = getAccountFromResponse(response);
        assertEquals("ACCT1", a.key);
    }

    @Test
    public void accountGetNonExistent() {
        assertIsNotFound(GET("/accounts/NOODLE"));
    }

    @Test
    public void accountDelete() {
        WebUser u = WebUser.find("byEmail", "ciaran.treanor@danutech.com").first();
        Response setUpResponse = POST(
                "/accounts?account.key=IIII&account.name=IIII%20Inc&account.manager.id=" + u.id + "&account.contact=a@abcd.ie");
        assertIsOk(setUpResponse);
        Response response = DELETE("/api/accounts/IIII");
        assertIsOk(response);
        assertEquals(0, Account.count("byKey", "IIII"));
    }

    @Test
    public void accountDeleteHasSubscribers() {
        Response response = DELETE("/api/accounts/ACCT1");
        assertStatus(Http.StatusCode.FORBIDDEN, response);
        assertEquals(1, Account.count("byKey", "ACCT1"));
    }

    @Test
    public void accountDeleteNonExistent() {
        assertIsNotFound(DELETE("/api/accounts/NOODLE"));
    }

    @Test
    public void accountEdit() {
        Response response = GET("/accounts/ACCT1");
        assertIsOk(response);
        Account account = getAccountFromResponse(response);
        response = PUT(
                "/accounts/ACCT1?account.key=NEW_KEY&account.name=testname%20Inc&account.manager.id=" + account.manager.id + "&account.contact=a@abcd.ie",
                "", "");
        assertIsOk(response);
        assertEquals(1, Account.count("byKey", "NEW_KEY"));
    }
    
     @Test
    public void accountEditMissingName() {
        Response response = GET("/accounts/ACCT1");
        assertIsOk(response);
        Account account = getAccountFromResponse(response);

        // Update the account omitting the account name
        Response responsePut = PUT(
                "/accounts/ACCT1?account.key=NEW_KEY&account.name=&account.manager.id=" + account.manager.id + "&account.contact=a@abcd.ie",
                "", "");
        assertStatus(Http.StatusCode.BAD_REQUEST, responsePut);
        
        assertEquals(0, Account.count("byKey", "NEW_KEY"));
    }

    private Collection<Account> getAccountsFromResponse(Response response) {
        Type accountCollection = new TypeToken<Collection<Account>>() {
        }.getType();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().
                create();
        String content = getContent(response);
        JsonElement jsonElement = new JsonParser().parse(content);
        return gson.fromJson(jsonElement, accountCollection);
    }
    
    private Collection<Account> getPagedAccountsFromResponse(Response response) {
        Type accountCollection = new TypeToken<Collection<Account>>() {
        }.getType();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String content = getContent(response);
        JsonElement jsonElement = new JsonParser().parse(content);
        JsonElement jsonData = jsonElement.getAsJsonObject().get("aaData").getAsJsonArray();
        return gson.fromJson(jsonData, accountCollection);
    }

    private int getNumberofAccountsFromResponse(Response response) {
        String content = getContent(response);
        JsonElement jsonElement = new JsonParser().parse(content);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("aaData");

        return jsonArray.size();
    }

    private Account getAccountFromResponse(Response response) {
        String content = getContent(response);
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = new JsonParser().parse(content);
        return gson.fromJson(jsonElement, Account.class);
    }
}
