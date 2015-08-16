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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsyncWithSequentialFuture")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithSequentialFutureResource {
    private final ProductDaoAsyncFuture productDao;

    public ProductHttpDbAsyncWithSequentialFutureResource(ProductDaoAsyncFuture productDao) {
        this.productDao = productDao;
    }
// debuging: Intellij issue to see variable: http://stackoverflow.com/questions/26895708/debugger-cannot-see-local-variable-in-a-lambda
    ///  vs anon class: variable visible. new instances??
// stack traces: we see "lambda" as method name. maybe meth reference? but closures are not possible ...
     ////////////////// read further: http://baddotrobot.com/blog/2014/02/18/method-references-in-java8/
    @GET
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        // NOTICE: assumption is that no synchronization is needed for List as there should be no race condition to populate it
        //         furthermore, assumption is that visibility between threads (if any) after mods to list are guaranteed by CompletableFuture (follows happens before)
        final CompletableFuture<Product> product = productDao.findNameById(id);
        final CompletableFuture<ProductWithGroups> finalResult = product.thenCompose(productFound -> {
            if (asyncResponse.isDone()) {
                return CompletableFuture.completedFuture(new ProductWithGroups());
            } else {
                final CompletableFuture<List<ProductGroup>> productGroups = productDao.findProductGroupsById(id);
                return productGroups.thenApply(productGroupsFound -> ProductWithGroups.createProductWithGroups(productFound, productGroupsFound));
            }

        });
        // TODO: can be async call and therefore not block PGASYNC single thread....
        finalResult.whenComplete((productWithGroups, error) -> {
            consumeProductWithGroupOrError(asyncResponse, productWithGroups, error);
        });
    }

    private void consumeProductWithGroupOrError(AsyncResponse asyncResponse, ProductWithGroups productWithGroups, Throwable error) {
        if (!asyncResponse.isDone()) {
            if (error != null) {
                asyncResponse.resume(error);
            } else {
                asyncResponse.resume(productWithGroups);
            }
        }
    }
}