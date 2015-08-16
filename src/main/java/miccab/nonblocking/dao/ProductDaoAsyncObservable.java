package miccab.nonblocking.dao;

import com.github.pgasync.Db;
import com.github.pgasync.ResultSet;
import com.github.pgasync.Row;
import miccab.nonblocking.model.Product;
import rx.Observable;
import rx.Observer;
import rx.subjects.AsyncSubject;

import java.util.Collections;
import java.util.Iterator;

import static miccab.nonblocking.model.Product.createProduct;

/**
 * Created by michal on 09.08.15.
 */
public class ProductDaoAsyncObservable {
    private final static String SQL_FIND_BY_ID = ProductDao.SQL_FIND_BY_ID.replace(":id", "$1");
    private final Db database;

    public ProductDaoAsyncObservable(Db database) {
        this.database = database;
    }

    public Observable<Product> findNameById(int id) {
        AsyncSubject<Product> subject = AsyncSubject.create();
        database.query(SQL_FIND_BY_ID, Collections.singletonList(id),
                       result -> {
                           consumeFindByIdResult(result, id, subject);
                       },
                       subject::onError);
        return subject;
    }


    private void consumeFindByIdResult(ResultSet resultSet, int id, Observer<Product> subject) {
        final Iterator<Row> sqlIterator = resultSet.iterator();
        if (sqlIterator.hasNext()) {
            final String name = sqlIterator.next().getString(0);
            final Product product = createProduct(name, id);
            subject.onNext(product);
            subject.onCompleted();
        } else {
            subject.onError(new IllegalArgumentException("Product not found"));
        }
    }

}
