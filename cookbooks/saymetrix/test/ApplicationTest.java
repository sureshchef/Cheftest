
import org.junit.Test;
import play.mvc.Http.Response;
import util.BaseFunctionalTest;

public class ApplicationTest extends BaseFunctionalTest {


    @Test
    public void testThatIndexPageWorks() {
        Response response = GET("/");
        assertIsOk(response);
        assertStatus(200, response);
        assertContentType("text/html", response);
        assertCharset(play.Play.defaultWebEncoding, response);
    }
}