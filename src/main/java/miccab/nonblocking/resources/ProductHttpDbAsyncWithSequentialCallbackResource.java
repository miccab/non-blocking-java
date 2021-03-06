package miccab.nonblocking.resources;

import miccab.nonblocking.model.Product;
import miccab.nonblocking.dao.ProductDaoAsyncCallback;
import miccab.nonblocking.model.ProductGroup;
import miccab.nonblocking.model.ProductWithGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsyncWithSequentialCallback")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithSequentialCallbackResource {
    private static final Logger LOG = LoggerFactory.getLogger(ProductHttpDbAsyncWithSequentialCallbackResource.class);
    private final ProductDaoAsyncCallback productDao;
    private final Executor executorToCompleteCalls;

    public ProductHttpDbAsyncWithSequentialCallbackResource(ProductDaoAsyncCallback productDao, Executor executorToCompleteCalls) {
        this.productDao = productDao;
        this.executorToCompleteCalls = executorToCompleteCalls;
    }

    @GET
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        productDao.findNameById(id,
                                new ProductConsumer(asyncResponse),
                                asyncResponse::resume);
    }

    class ProductConsumer implements Consumer<Product> {
        private final AsyncResponse asyncResponse;

        public ProductConsumer(AsyncResponse asyncResponse) {
            this.asyncResponse = asyncResponse;
        }

        @Override
        public void accept(Product product) {
            if (asyncResponse.isDone()) {
                LOG.info("Response already done");
            } else {
                doAccept(product);
            }
        }

        private void doAccept(Product product) {
            productDao.findProductGroupsById(product.getId(),
                                             new ProductGroupsConsumer(product, asyncResponse),
                                             asyncResponse::resume);
        }
    }

    class ProductGroupsConsumer implements Consumer<List<ProductGroup>> {
        private final Product product;
        private final AsyncResponse asyncResponse;

        public ProductGroupsConsumer(Product product, AsyncResponse asyncResponse) {
            this.product = product;
            this.asyncResponse = asyncResponse;
        }

        @Override
        public void accept(List<ProductGroup> productGroups) {
            executorToCompleteCalls.execute(() -> asyncResponse.resume(ProductWithGroups.createProductWithGroups(product, productGroups)));
        }
    }
}