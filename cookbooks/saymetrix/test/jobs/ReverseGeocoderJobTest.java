package jobs;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonStreamParser;
import com.google.gson.stream.JsonReader;
import org.junit.Test;
import play.Logger;
import play.libs.WS;
import play.test.UnitTest;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;

public class ReverseGeocoderJobTest extends UnitTest {
    private JsonParser parser = new JsonParser();

    @Test
    public void testExtractAddress() throws FileNotFoundException {
        JsonElement response = parser.parse(new JsonReader(new FileReader("test/jobs/geocode1.json")));

        assertEquals("OK", ReverseGeocoderJob.extractStatus(response));
        assertEquals("Ballycarnan, Co. Laois, Ireland", ReverseGeocoderJob.extractFormattedAddress(response));
    }

    @Test
    public void testBadStatus() throws FileNotFoundException {
        JsonElement response = parser.parse(new JsonReader(new FileReader("test/jobs/geocode2.json")));

        assertEquals("ANYTHING_OTHER_THAN_OK", ReverseGeocoderJob.extractStatus(response));
    }
}
