package controllers;

import models.WebUser;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import util.BaseFunctionalTest;

public class SecurityTest extends BaseFunctionalTest {

    @Test
    public void forgotPassword() {
        Response response = GET("/forgot");
        assertIsOk(response);
    }
    
    @Test
    public void requestReset() {
        WebUser u = WebUser.getByEmail("am1@operator.com");
        Response response = POST("/request-reset?email=" + u.email);
        assertIsOk(response);
        u.save();
        u.refresh();
        assertTrue(u.passwordResetToken != null);
    }
    
    @Test
    public void requestResetWithNullUser() {
        Response response = POST("/request-reset?email=not@email.com");
        assertIsOk(response);
    }
    
    @Test
    public void reset() {
        WebUser u = WebUser.getByEmail("am1@operator.com");
        u.passwordResetToken = WebUser.generateToken();
        u.save();
        u.refresh();
        Response response = GET("/pwreset/"+u.passwordResetToken);
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/change-password", response);
        //not sure how to check variables inside Flash - possible a problem due to its lifespan (i.e. only lives until the request is complete)
        //assertEquals(Flash.current().get("token"), u.passwordResetToken)
    }
    
    @Test
    public void resetWithBadToken() {
        Response response = GET("/pwreset/ABCDEFG");
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/forgot", response);
        //assertTrue("Unable to verify a password reset request for this user. Please try again.".equals(Flash.current().get("error")));
    }
    
    @Test
    public void changePassword() {
        Response response = GET("/change-password");
        assertIsOk(response);
    }
    
    @Test
    public void resetPassword() {
        WebUser u = WebUser.getByEmail("am1@operator.com");
        u.passwordResetToken = WebUser.generateToken();
        u.save();
        u.refresh();
        Response response = POST("/password-reset?token=" + u.passwordResetToken + "&password=newpass1&confirm-password=newpass1");
        assertIsOk(response);
        u.refresh();
        assertTrue(u.passwordResetToken == null);
    }
    
    @Test
    public void resetPasswordWithDifferentPasswords() {
        WebUser u = WebUser.getByEmail("am1@operator.com");
        u.passwordResetToken = WebUser.generateToken();
        u.save();
        u.refresh();
        Response response = POST("/password-reset?token=" + u.passwordResetToken + "&password=newpass&confirm-password=differentpass");
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/change-password", response);
        //assertEquals(Flash.current().get("token"), u.passwordResetToken);
        //assertTrue("Passwords do not match.".equals(Flash.current().get("error")));
    }
    
    @Test
    public void resetPasswordWithBadToken() {
        WebUser u = WebUser.getByEmail("am1@operator.com");
        u.passwordResetToken = WebUser.generateToken();
        u.save();
        u.refresh();
        Response response = POST("/password-reset?token=ABCDEFG&password=newpass&confirm-password=newpass");
        assertStatus(Http.StatusCode.FOUND, response);
        assertHeaderEquals("Location", "/forgot", response);
        //assertTrue("Unable to verify a password reset request for this user. Please try again.".equals(Flash.current().get("error")));
    }
}
