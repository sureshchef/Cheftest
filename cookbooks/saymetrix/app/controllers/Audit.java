package controllers;

import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;
import flexjson.JSONSerializer;
import models.AuditEvent;
import models.WebUser;
import org.hibernate.Session;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import javax.persistence.Query;
import java.util.List;

@With(Deadbolt.class)
@Restrict("admin")
public class Audit extends Controller {
    private static final JSONSerializer SERIALIZER = AuditEvent.createSerializer();

    @Before
    static void setConnecteduser() {
        if (Security.isConnected()) {
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            renderArgs.put("webuser", user.firstname);

            // Account Managers should only be able to see their own accounts
            if("kam".equals(user.role.name)) {
                ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);
            }
        }
    }

    public static void index() {
        renderArgs.put("tab", "audit");
        render();
    }

    /**
     * Render list of events matching the search criteria in DataTables-compatible JSON format.
     *
     * @param sEcho
     * @param sSearch
     * @param iDisplayStart
     * @param iDisplayLength
     * @param iSortCol_0
     * @param sSortDir_0
     */
    public static void getEventsDataTables(int sEcho, String sSearch, int iDisplayStart,
            int iDisplayLength, int iSortCol_0, String sSortDir_0) {

        if(iDisplayLength < 10 || iDisplayLength > 100) {
            iDisplayLength = 10;
        }
        // Count number of Events (no filtering)
        long iTotalRecords = AuditEvent.count();

        // Count number of Events matching the filter
        Query query;
        if(sSearch == null) {
            query = JPA.em().createQuery("SELECT COUNT(e) FROM AuditEvent e");
        } else {
            query = JPA.em().createQuery("SELECT COUNT(e) FROM AuditEvent e WHERE e.actorEmail LIKE :actor OR e.details LIKE :details");
            query.setParameter("actor", "%" + sSearch + "%");
            query.setParameter("details", "%" + sSearch + "%");
        }

        long iTotalDisplayRecords = (Long) query.getSingleResult();

        // Get a single page of matching results
        if(sSearch == null) {
            query = JPA.em().createQuery("SELECT e FROM AuditEvent e");
        } else {
            query = JPA.em().createQuery("SELECT e FROM AuditEvent e WHERE e.actorEmail LIKE :actor OR e.details LIKE :details ORDER BY e.timestamp DESC");
            query.setParameter("actor", "%" + sSearch + "%");
            query.setParameter("details", "%" + sSearch + "%");
        }
        query.setFirstResult(iDisplayStart);
        query.setMaxResults(iDisplayLength);

        List<AuditEvent> events = query.getResultList();

        PaginatedResult results = new PaginatedResult(sEcho, iTotalRecords,
                iTotalDisplayRecords, events);
        renderJSON(SERIALIZER.serialize(results));
    }
}
