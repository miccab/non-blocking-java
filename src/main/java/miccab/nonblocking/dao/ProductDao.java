package miccab.nonblocking.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;

/**
 * Created by michal on 02.08.15.
 */
public interface ProductDao {
    String SQL_FIND_BY_ID = "select find_product_name(:id)";
    @SqlQuery(SQL_FIND_BY_ID)
    String findNameById(@Bind("id") int id);
}
