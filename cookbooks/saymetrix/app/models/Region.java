package models;

import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Represents a geographical region, for example a county within
 * a country.
 */
@Entity
@Table(name = "rpt_region", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"scheme_id", "name"}),
        @UniqueConstraint(columnNames = {"scheme_id", "query"})
})
public class Region extends Model {
    @Column(length = 32, nullable = false)
    public String name;
    /** Query string required to find incidents within this region */
    @Column(length = 32, nullable = false)
    public String query;

    public Region(String name) {
        this.name = name;
    }

    public Region(String name, String query) {
        this.name = name;
        this.query = query;
    }
}
