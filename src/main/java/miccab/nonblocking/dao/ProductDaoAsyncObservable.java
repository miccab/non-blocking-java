package miccab.nonblocking.dao;

import com.github.pgasync.Db;
import com.github.pgasync.ResultSet;
import com.github.pgasync.Row;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.model.ProductGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observer;
import rx.subjects.AsyncSubject;
import rx.subjects.ReplaySubject;

import java.util.Collections;
import java.util.Iterator;

import static miccab.nonblocking.dao.ProductDaoAsyncCallback.createProductGroupFromNextRow;
import static miccab.nonblocking.model.Product.createProduct;

/**
 * Created by michal on 09.08.15.
 */
public class ProductDaoAsyncObservable {
    private static final Logger LOG = LoggerFactory.getLogger(ProductDaoAsyncObservable.class);
    private final static String SQL_FIND_BY_ID = ProductDao.SQL_FIND_BY_ID.replace(":id", "$1");
    private final static String SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID = ProductDao.SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID.replace(":id", "$1");
    private final Db database;

    public ProductDaoAsyncObservable(Db database) {
        this.database = database;
    }

    public Observable<Product> findNameById(int id) {
        LOG.trace("findNameById start");
        final AsyncSubject<Product> subject = AsyncSubject.create();
        database.query(SQL_FIND_BY_ID, Collections.singletonList(id),
                       result -> consumeFindByIdResult(result, id, subject),
                       subject::onError);
        LOG.trace("findNameById end");
        return subject.single();
    }


    private void consumeFindByIdResult(ResultSet resultSet, int id, Observer<Product> productObserver) {
        LOG.trace("consumeFindByIdResult start");
        final Iterator<Row> sqlIterator = resultSet.iterator();
        if (sqlIterator.hasNext()) {
            final String name = sqlIterator.next().getString(0);
            final Product product = createProduct(name, id);
            productObserver.onNext(product);
            productObserver.onCompleted();
        } else {
            productObserver.onError(new IllegalArgumentException("Product not found"));
        }
        LOG.trace("consumeFindByIdResult end");
    }


    public Observable<ProductGroup> findProductGroupsById(int id) {
        final ReplaySubject<ProductGroup> subject = ReplaySubject.create();
        database.query(SQL_FIND_PRODUCT_GROUPS_BY_PRODUCT_ID, Collections.singletonList(id),
                result -> consumeFindProductGroupsByIdResult(result, subject),
                subject::onError);
        return subject;
    }

    private void consumeFindProductGroupsByIdResult(ResultSet resultSet, Observer<ProductGroup> productGroupsObserver) {
        LOG.trace("consumeFindProductGroupsByIdResult start");
        final Iterator<Row> sqlIterator = resultSet.iterator();
        while (sqlIterator.hasNext()) {
            productGroupsObserver.onNext(createProductGroupFromNextRow(sqlIterator));
        }
        productGroupsObserver.onCompleted();
        LOG.trace("consumeFindProductGroupsByIdResult end");
    }

}
