package miccab.nonblocking.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;

import java.util.List;

/**
 * Created by michal on 02.08.15.
 */
public interface ProductDao {
    String SQL_FIND_BY_ID = "select find_product_name(:id)";
    @SqlQuery(SQL_FIND_BY_ID)
    String findNameById(@Bind("id") int id);

    String SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID = "select id, name from product_to_group ptg join product_group pg on pg.id = ptg.product_group_id where product_id = :id";
    @SqlQuery(SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID)
    List<String> findProductGroupsById(@Bind("id") int id);
}
