@FilterDef(name="manager", parameters = { @ParamDef(name="manager_id", type="long")},
        defaultCondition = ":manager_id = manager_id")
package models;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;