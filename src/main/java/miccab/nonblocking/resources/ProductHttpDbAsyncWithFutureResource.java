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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsyncWithFuture")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithFutureResource {
    private final ProductDaoAsyncFuture productDao;

    public ProductHttpDbAsyncWithFutureResource(ProductDaoAsyncFuture productDao) {
        this.productDao = productDao;
    }

    @GET
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        final CompletableFuture<Product> productFound = productDao.findNameById(id);
        productFound.whenComplete( (product, error) -> {
            consumeProductOrError(asyncResponse, product, error);
        });
    }

    private void consumeProductOrError(AsyncResponse asyncResponse, Product product, Throwable error) {
        if (error != null) {
            asyncResponse.resume(error);
        } else {
            asyncResponse.resume(product);
        }
    }
}