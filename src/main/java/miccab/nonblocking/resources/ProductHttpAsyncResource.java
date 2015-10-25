package miccab.nonblocking.resources;

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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpAsync")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpAsyncResource {
    private static final Logger LOG = LoggerFactory.getLogger(ProductHttpAsyncResource.class);
    private final ProductDao productDao;
    private final Executor executorService;

    public ProductHttpAsyncResource(ProductDao productDao, ExecutorService executorService) {
        this.productDao = productDao;
        this.executorService = executorService;
    }

    @GET
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        LOG.trace("Finding product by id:{}", id);
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        executorService.execute(new AsyncFindById(id, asyncResponse));
    }

    class AsyncFindById implements Runnable {
        private final int id;
        private final AsyncResponse asyncResponse;

        AsyncFindById(int id, AsyncResponse asyncResponse) {
            this.id = id;
            this.asyncResponse = asyncResponse;
        }

        public void run() {
            if (! asyncResponse.isDone()) {
                try {
                    asyncResponse.resume(doRun());
                } catch (RuntimeException e) {
                    asyncResponse.resume(e);
                }
            }
        }

        public Product doRun() {
            LOG.trace("Calling DAO for product by id:{}", id);
            final Product product = new Product();
            product.setId(id);
            product.setName(productDao.findNameById(id));
            return product;
        }
    }
}