package miccab.nonblocking.dao;

import com.github.pgasync.Db;
import com.github.pgasync.ResultSet;
import com.github.pgasync.Row;
import miccab.nonblocking.model.Product;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

import static miccab.nonblocking.model.Product.createProduct;

/**
 * Created by michal on 09.08.15.
 */
public class ProductDaoAsyncCallback {
    private final static String SQL_FIND_BY_ID = ProductDao.SQL_FIND_BY_ID.replace(":id", "$1");
    private final Db database;

    public ProductDaoAsyncCallback(Db database) {
        this.database = database;
    }

    public void findNameById(int id, Consumer<Product> productConsumer, Consumer<Throwable> errorConsumer) {
        database.query(SQL_FIND_BY_ID, Collections.singletonList(id),
        result -> {
            consumeFindByIdResult(result, id, productConsumer, errorConsumer);
        },
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
}
