package util;

import org.junit.Before;
import play.Logger;
import play.cache.Cache;
import play.mvc.Http;
import play.test.Fixtures;
import play.test.FunctionalTest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseFunctionalTest extends FunctionalTest {
    private static final String COOKIE_NAME = "SAYM_SESSION";
    private static final String USER_COOKIE = "00a3da1c83c79f0954a560fdc71a66ced6ed7d56-%00username%3Ad" +
            "damien.daly%40danutech.com%00%00___AT%3A8c257b76078394e2c31af88cba8902f8efde834c%00";
    private static final String ADMIN_COOKIE = "da9a38d0cd29fdab102d2d920500b5f1872595be-%00username%3A" +
            "ciaran.treanor%40danutech.com%00%00___AT%3A4024bcd8409b6fa561e9e0e0a90a9fc69aa95d93%00";
    Map<String, Http.Cookie> cookies = new HashMap<String, Http.Cookie>();
    Http.Cookie cookie = new Http.Cookie();

    protected BaseFunctionalTest() {
        cookie.name = COOKIE_NAME;
    }

    @Before
    public void setUp() throws Exception {
        Fixtures.deleteAllModels();
        Cache.clear();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("initial-data-dev.yml");
        loadAdminCookie();
    }

    protected void loadAdminCookie() {
        setSessionCookie(ADMIN_COOKIE);
    }

    protected void loadUserCookie() {
        setSessionCookie(USER_COOKIE);
    }

    private void setSessionCookie(String value) {
        cookies.clear();
        cookie.value = value;
        cookies.put(COOKIE_NAME, cookie);
        Field field;
        try {
            field = FunctionalTest.class.getDeclaredField("savedCookies");
            field.setAccessible(true);
            field.set(this, cookies);
        } catch (IllegalAccessException e) {
            Logger.fatal(e, "");
        } catch (NoSuchFieldException e) {
            Logger.fatal(e, "");
        }
    }
}
