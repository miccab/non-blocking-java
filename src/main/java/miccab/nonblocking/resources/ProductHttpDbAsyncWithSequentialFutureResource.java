package miccab.nonblocking.resources;

import com.google.common.collect.ImmutableList;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.dao.ProductDaoAsyncFuture;

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
    public void findById(@QueryParam("id") List<Integer> ids, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        // NOTICE: assumption is that no synchronization is needed for List as there should be no race condition to populate it
        //         furthermore, assumption is that visibility between threads (if any) after mods to list are guaranteed by CompletableFuture (follows happens before)
        CompletableFuture<List<Product>> finalResult = CompletableFuture.completedFuture(ImmutableList.of());
        for (Integer id : ids) {
            finalResult = finalResult.thenCompose(productsAccumulated -> {
                if (productsAccumulated.size() == 1) throw new IllegalArgumentException("asf");
                if (asyncResponse.isDone()) {
                    return CompletableFuture.completedFuture(ImmutableList.of());
                } else {
                    final CompletableFuture<Product> productFound = productDao.findNameById(id);
                    return productFound.thenApply(product -> ImmutableList.<Product>builder().addAll(productsAccumulated).add(product).build());
                }
            });
        }
        finalResult.whenComplete((products, error) -> {
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