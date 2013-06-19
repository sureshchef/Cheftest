package models;

import org.junit.Before;
import org.junit.Test;
import play.test.Fixtures;
import play.test.UnitTest;

public class SystemSettingTest extends UnitTest {

    @Before
    public void setup() {
        Fixtures.deleteAllModels();
        Fixtures.loadModels("test-system-setting.yml");
    }

    @Test
    public void testBooleanSetting() {
        assertFalse(SystemSetting.getBoolean("bool_unknown_key", false));
        assertTrue(SystemSetting.getBoolean("bool_true", false));
        assertFalse(SystemSetting.getBoolean("bool_false", true));
        assertTrue(SystemSetting.getBoolean("bool_invalid", true));
        assertTrue(SystemSetting.getBoolean("bool_no_value", true));
    }

    @Test(expected = NullPointerException.class)
    public void testBooleanNull() {
        SystemSetting.getBoolean(null);
    }

    @Test
    public void testBooleanTrueValues() {
        assertTrue(SystemSetting.getBoolean("t"));
        assertTrue(SystemSetting.getBoolean("T"));
        assertTrue(SystemSetting.getBoolean("true"));
        assertTrue(SystemSetting.getBoolean("TRUE"));
        assertTrue(SystemSetting.getBoolean("1"));
        assertTrue(SystemSetting.getBoolean("y"));
        assertTrue(SystemSetting.getBoolean("Y"));
    }

    @Test
    public void testBooleanFalseValues() {
        assertFalse(SystemSetting.getBoolean("f"));
        assertFalse(SystemSetting.getBoolean("F"));
        assertFalse(SystemSetting.getBoolean("false"));
        assertFalse(SystemSetting.getBoolean("FALSE"));
        assertFalse(SystemSetting.getBoolean("0"));
        assertFalse(SystemSetting.getBoolean("n"));
        assertFalse(SystemSetting.getBoolean("N"));
    }

    @Test
    public void testIntegerSetting() {
        assertEquals(-1, SystemSetting.getInt("int_unknown_key", -1));
        assertEquals(42, SystemSetting.getInt("int_valid", -1));
        assertEquals(-1, SystemSetting.getInt("int_invalid", -1));
        assertEquals(-1, SystemSetting.getInt("int_no_value", -1));
    }

    @Test
    public void testStringSetting() {
        assertEquals("hello", SystemSetting.get("string_valid", "dink"));
        assertEquals("dink", SystemSetting.get("string_no_value", "dink"));
    }
}
