package models;

import org.junit.Before;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;

import javax.persistence.PersistenceException;
import java.util.List;

public class RegionSchemeTest extends UnitTest {
    private static String SCHEME_NAME = "Test 1";
    private RegionScheme s0;

    @Before
    public void setup() {
        deleteAllRegionSchemes();

        s0 = new RegionScheme(SCHEME_NAME);
        s0.regions.add(new Region("1.3", "1.3"));
        s0.regions.add(new Region("1.2", "1.2"));
        s0.regions.add(new Region("1.1", "1.1"));

        s0.save();
    }

    @Test(expected = PersistenceException.class)
    public void testUniqueSchemeName() {
        new RegionScheme(SCHEME_NAME).save();
    }

    @Test(expected = PersistenceException.class)
    public void testRegionCannotBeOrphan() {
        new Region("Foo", "Bar").save();
    }

    @Test
    public void addRegion() {
        assertEquals(3, Region.count());
        s0.regions.add(new Region("1.4", "1.4"));
        s0.save();
        assertEquals(4, Region.count());
    }

    @Test (expected = PersistenceException.class)
    public void testRegionWithDuplicateName() {
        s0 = RegionScheme.find("byName", SCHEME_NAME).first();
        s0.regions.add(new Region("1.1", "X"));

        s0.save();
    }

    @Test (expected = PersistenceException.class)
    public void testRegionWithDuplicateQuery() {
        s0.regions.add(new Region("X", "1.1"));

        s0.save();
    }

    @Test
    public void testDeleteCascade() {
        assertEquals(1, RegionScheme.count());
        assertEquals(3, Region.count());
        s0.delete();
        assertEquals(0, RegionScheme.count());
        assertEquals(0, Region.count());
    }

    /*
     * Annoyingly Play! GenericModel.deleteAll() doesn't cascade
     * the delete operation. You wouldn't believe how long it
     * took to figure that one out.
     *
     * http://stackoverflow.com/a/9603267/1369495
     * https://groups.google.com/d/topic/play-framework/cqAjmzlDZ9Y/discussion
     */
    private void deleteAllRegionSchemes() {
        JPA.em().clear();
        List<RegionScheme> schemes = RegionScheme.findAll();
        for(RegionScheme scheme : schemes) {
            scheme.delete();
        }
    }
}
