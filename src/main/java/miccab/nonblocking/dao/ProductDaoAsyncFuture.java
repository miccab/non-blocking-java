package miccab.nonblocking.dao;

import com.github.pgasync.Db;
import com.github.pgasync.ResultSet;
import com.github.pgasync.Row;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.model.ProductGroup;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static miccab.nonblocking.dao.ProductDaoAsyncCallback.consumeListOfProductGroups;
import static miccab.nonblocking.model.Product.createProduct;

/**
 * Created by michal on 09.08.15.
 */
public class ProductDaoAsyncFuture {
    private final static String SQL_FIND_BY_ID = ProductDao.SQL_FIND_BY_ID.replace(":id", "$1");
    private final static String SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID = ProductDao.SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID.replace(":id", "$1");
    private final Db database;

    public ProductDaoAsyncFuture(Db database) {
        this.database = database;
    }

    public CompletableFuture<Product> findNameById(int id) {
        final CompletableFuture<Product> futureResult = new CompletableFuture<>();
        database.query(SQL_FIND_BY_ID, Collections.singletonList(id),
                       result -> consumeFindByIdResult(result, id, futureResult),
                       futureResult::completeExceptionally);
        return futureResult;
    }


    private void consumeFindByIdResult(ResultSet resultSet, int id, CompletableFuture<Product> futureResult) {
        final Iterator<Row> sqlIterator = resultSet.iterator();
        if (sqlIterator.hasNext()) {
            final String name = sqlIterator.next().getString(0);
            final Product product = createProduct(name, id);
            futureResult.complete(product);
        } else {
            futureResult.completeExceptionally(new IllegalArgumentException("Product not found"));
        }
    }

    public CompletableFuture<List<ProductGroup>> findProductGroupsById(int id) {
        final CompletableFuture<List<ProductGroup>> futureResult = new CompletableFuture<>();
        database.query(SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID, Collections.singletonList(id),
                result -> consumeFindProductGroupsByIdResult(result, futureResult),
                futureResult::completeExceptionally);
        return futureResult;
    }

    private void consumeFindProductGroupsByIdResult(ResultSet resultSet, CompletableFuture<List<ProductGroup>> productGroupConsumer) {
        final Iterator<Row> sqlIterator = resultSet.iterator();
        if (sqlIterator.hasNext()) {
            productGroupConsumer.complete(consumeListOfProductGroups(sqlIterator));
        } else {
            productGroupConsumer.complete(Collections.emptyList());
        }
    }

}
