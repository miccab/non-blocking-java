package miccab.nonblocking.resources;

import miccab.nonblocking.model.Product;
import miccab.nonblocking.dao.ProductDaoAsyncCallback;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsyncWithSequentialCallback")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithSequentialCallbackResource {
    private final ProductDaoAsyncCallback productDao;

    public ProductHttpDbAsyncWithSequentialCallbackResource(ProductDaoAsyncCallback productDao) {
        this.productDao = productDao;
    }

    @GET
    public void findById(@QueryParam("id") List<Integer> ids, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        // NOTICE: assumption is that no sync is needed for the list as there should be no race condition
        //         futhermore, if DAO uses Executors (or any other scheduling) then it must be done using happens before rules to guarantee visibility of mods to the list
        final Queue<Integer> pendingIds = new LinkedList<>(ids);
        productDao.findNameById(pendingIds.remove(),
                new ProductConsumer(pendingIds, new ArrayList<>(), asyncResponse),
                asyncResponse::resume);
    }

    class ProductConsumer implements Consumer<Product> {
        private final Queue<Integer> pendingIds;
        private final List<Product> productsFound;
        private final AsyncResponse asyncResponse;

        ProductConsumer(Queue<Integer> pendingIds, List<Product> productsFound, AsyncResponse asyncResponse) {
            this.pendingIds = pendingIds;
            this.productsFound = productsFound;
            this.asyncResponse = asyncResponse;
        }

        @Override
        public void accept(Product product) {
            if (!asyncResponse.isDone()) {
                doAccept(product);
            }
        }

        private void doAccept(Product product) {
            productsFound.add(product);
            if (pendingIds.isEmpty()) {
                asyncResponse.resume(productsFound);
            } else {
                ProductHttpDbAsyncWithSequentialCallbackResource.this.productDao.findNameById(pendingIds.remove(), this, asyncResponse::resume);
            }
        }
    }

}