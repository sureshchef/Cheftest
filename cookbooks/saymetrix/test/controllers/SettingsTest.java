package controllers;

import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import util.BaseFunctionalTest;

public class SettingsTest extends BaseFunctionalTest {

    @Test
    public void index() {
        Response response = GET("/settings");
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/settings/password", response);
    }
    
    @Test
    public void password() {
        Response response = GET("/settings/password");
        assertIsOk(response);
    }
    
    @Test
    public void forgotPassword() {
        Response response = GET("/settings/password/forgot");
        assertIsOk(response);
    }
    
    @Test
    public void setPassword() {
        Response response = POST("/settings/password/change?current=hello&password=newpassword&confirm-password=newpassword");
        assertIsOk(response);
    }
    
    @Test
    public void setPasswordWithWrongCurrent() {
        Response response = POST("/settings/password/change?current=nope&password=newpassword&confirm-password=newpassword");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(response.out.toString().contains("current-password"));
    }
    
    @Test
    public void setPasswordWithInvalidPassword() {
        Response response = POST("/settings/password/change?current=hello&password=nope&confirm-password=newpassword");
        assertStatus(Http.StatusCode.BAD_REQUEST, response);
        assertTrue(response.out.toString().contains("new-password"));
    }
}
