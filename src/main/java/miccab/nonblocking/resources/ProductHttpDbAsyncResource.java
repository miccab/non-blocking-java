package miccab.nonblocking.resources;

import com.github.pgasync.Db;
import com.github.pgasync.ResultSet;
import com.github.pgasync.Row;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.dao.ProductDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static miccab.nonblocking.model.Product.createProduct;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsync")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncResource {
    private static final Logger LOG = LoggerFactory.getLogger(ProductHttpDbAsyncResource.class);
    private final static String SQL_FIND_BY_ID = ProductDao.SQL_FIND_BY_ID.replace(":id", "$1");
    private final Db database;

    public ProductHttpDbAsyncResource(Db database) {
        this.database = database;
    }

    @GET
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        LOG.trace("Finding product by id:{}", id);
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        database.query(SQL_FIND_BY_ID, Collections.singletonList(id),
                       result -> {
                           consumeFindByIdResult(result, id, asyncResponse);
                       },
                       asyncResponse::resume);
    }

    private void consumeFindByIdResult(ResultSet resultSet, int id, AsyncResponse asyncResponse) {
        LOG.trace("Consuming DB Query result for product by id:{}", id);
        final Iterator<Row> sqlIterator = resultSet.iterator();
        if (sqlIterator.hasNext()) {
            final String name = sqlIterator.next().getString(0);
            final Product product = createProduct(name, id);
            asyncResponse.resume(product);
        } else {
            asyncResponse.resume(new IllegalArgumentException("Product not found"));
        }
    }
}