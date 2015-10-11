package miccab.nonblocking.dao;

import com.github.pgasync.Db;
import com.github.pgasync.ResultSet;
import com.github.pgasync.Row;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.model.ProductGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static miccab.nonblocking.model.Product.createProduct;

/**
 * Created by michal on 09.08.15.
 */
public class ProductDaoAsyncCallback {
    private final static String SQL_FIND_BY_ID = ProductDao.SQL_FIND_BY_ID.replace(":id", "$1");
    private final static String SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID = ProductDao.SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID.replace(":id", "$1");
    private final Db database;

    public ProductDaoAsyncCallback(Db database) {
        this.database = database;
    }

    public void findNameById(int id, Consumer<Product> productConsumer, Consumer<Throwable> errorConsumer) {
        database.query(SQL_FIND_BY_ID, Collections.singletonList(id),
        result -> consumeFindByIdResult(result, id, productConsumer, errorConsumer),
        errorConsumer);
    }

    private void consumeFindByIdResult(ResultSet resultSet, int id, Consumer<Product> productConsumer, Consumer<Throwable> errorConsumer) {
        final Iterator<Row> sqlIterator = resultSet.iterator();
        if (sqlIterator.hasNext()) {
            final String name = sqlIterator.next().getString(0);
            final Product product = createProduct(name, id);
            productConsumer.accept(product);
        } else {
            errorConsumer.accept(new IllegalArgumentException("Product not found"));
        }
    }

    public void findProductGroupsById(int id, Consumer<List<ProductGroup>> productGroupConsumer, Consumer<Throwable> errorConsumer) {
        database.query(SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID, Collections.singletonList(id),
                result -> consumeFindProductGroupsByIdResult(result, productGroupConsumer, errorConsumer),
                errorConsumer);
    }

    private void consumeFindProductGroupsByIdResult(ResultSet resultSet, Consumer<List<ProductGroup>> productGroupConsumer, Consumer<Throwable> errorConsumer) {
        final Iterator<Row> sqlIterator = resultSet.iterator();
        if (sqlIterator.hasNext()) {
            productGroupConsumer.accept(consumeListOfProductGroups(sqlIterator));
        } else {
            productGroupConsumer.accept(Collections.emptyList());
        }
    }

    public static List<ProductGroup> consumeListOfProductGroups(Iterator<Row> sqlIterator) {
        final List<ProductGroup> productGroups = new ArrayList<>();
        while (sqlIterator.hasNext()) {
            productGroups.add(createProductGroupFromNextRow(sqlIterator));
        }
        return productGroups;
    }

    public static ProductGroup createProductGroupFromNextRow(Iterator<Row> sqlIterator) {
        final Row row = sqlIterator.next();
        final int id = row.getInt(0);
        final String name = row.getString(1);
        return ProductGroup.createGroup(name, id);
    }

}
