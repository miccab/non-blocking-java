package miccab.nonblocking.resources;

import miccab.nonblocking.model.Product;
import miccab.nonblocking.dao.ProductDaoAsyncFuture;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsyncWithParallelFuture")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithParallelFutureResource {
    private final ProductDaoAsyncFuture productDao;

    public ProductHttpDbAsyncWithParallelFutureResource(ProductDaoAsyncFuture productDao) {
        this.productDao = productDao;
    }

    @GET
    public void findById(@QueryParam("id1") int id1, @QueryParam("id2") int id2, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        final CompletableFuture<Product> product1Found = productDao.findNameById(id1);
        final CompletableFuture<Product> product2Found = productDao.findNameById(id2);
        final CompletableFuture<List<Product>> finalResult = product1Found.thenCombine(product2Found,
                                                                                      (product1, product2) -> Arrays.asList(product1, product2));
        finalResult.whenComplete((products,error) -> {
            consumeProductsOrError(asyncResponse, products, error);
        });
    }

    private void consumeProductsOrError(AsyncResponse asyncResponse, List<Product> products, Throwable error) {
        if (!asyncResponse.isDone()) {
            if (error != null) {
                asyncResponse.resume(error);
            } else {
                asyncResponse.resume(products);
            }
        }
    }
}