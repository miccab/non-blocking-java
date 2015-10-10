package miccab.nonblocking.resources;

import miccab.nonblocking.model.Product;
import miccab.nonblocking.dao.ProductDaoAsyncFuture;
import miccab.nonblocking.model.ProductGroup;
import miccab.nonblocking.model.ProductWithGroups;

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
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        final CompletableFuture<Product> product = productDao.findNameById(id);
        final CompletableFuture<List<ProductGroup>> productGroups = productDao.findProductGroupsById(id);
        final CompletableFuture<ProductWithGroups> finalResult = product.thenCombine(productGroups,
                                                                                     ProductWithGroups::createProductWithGroups);
        finalResult.whenComplete((productWithGroups,error) -> consumeProductsOrError(asyncResponse, productWithGroups, error));
    }

    private void consumeProductsOrError(AsyncResponse asyncResponse, ProductWithGroups productWithGroups, Throwable error) {
        if (error != null) {
            asyncResponse.resume(error);
        } else {
            asyncResponse.resume(productWithGroups);
        }
    }
}