package models;

import models.valueobject.FilterValueObject;
import org.junit.Before;
import org.junit.Test;
import play.test.Fixtures;
import play.test.UnitTest;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

public class FilterTest extends UnitTest {

    @Before
    public void setup() {
        Fixtures.deleteAllModels();
        Fixtures.loadModels("initial-data-prod.yml");
        Fixtures.loadModels("data.yml");
    }
    
    @Test
    public void testIncidentsLoaded() {
        assertEquals(1, Filter.count());
    }
    
    @Test
    public void testGenerateCriteriaMethod() {
        CriteriaQuery<Incident> criteriaQuery = Filter.generateCriteria(new Filter(), "date", "DESC", "");
        TypedQuery<Incident> query = Incident.em().createQuery(criteriaQuery);
        List<Incident> resultList = query.getResultList();
        assertEquals(5, resultList.size());
    }

    @Test
    public void testoverWriteValuesMethod() {
        String filterName = "Filter One";
        String anotherFilterName = "Filter Two";
        Filter filter = new Filter();
        filter.name = filterName;
        Filter anotherFilter = new Filter();
        anotherFilter.name = anotherFilterName;
        filter.overWriteValues(anotherFilter);
        assertEquals(anotherFilterName, filter.name);
    }

    @Test
    public void testGetValueObjectMethod() {
        String filterName = "Filter Name";
        Filter filter = new Filter();
        filter.id = (long)1;
        filter.name = filterName;
        FilterValueObject valueObject = filter.getValueObject();
        assertEquals(filterName, valueObject.getName());
    }

    @Test
    public void testNormalizedMsisdn() {
        Filter filter = new Filter();
        filter.msisdn = "  +353 87-804 1429 ";
        assertEquals("353878041429", filter.getNormalizedMsisdn());
        filter.msisdn = "  087-804 1429 ";
        assertEquals("878041429", filter.getNormalizedMsisdn());
        filter.msisdn = "  0035387-804 1429 ";
        assertEquals("353878041429", filter.getNormalizedMsisdn());
    }
}
