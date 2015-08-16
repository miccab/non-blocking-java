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

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsyncWithParallelCallback")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithParallelCallbackResource {
    private final ProductDaoAsyncCallback productDao;

    public ProductHttpDbAsyncWithParallelCallbackResource(ProductDaoAsyncCallback productDao) {
        this.productDao = productDao;
    }

    @GET
    public void findById(@QueryParam("id1") int id1, @QueryParam("id2") int id2, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        // NOTICE: DAO now uses single threaded model. So we should not need any sync
        //         however, this is implementation detail which might change.
        //         so to be safe we use synchronization to protect from race conditions
        final List<Product> productsFound = Collections.synchronizedList(new ArrayList<>());
        productDao.findNameById(id1,
                productFound -> {
                    consumeProduct(asyncResponse, productsFound, productFound);
                },
                asyncResponse::resume);
        productDao.findNameById(id2,
                productFound -> {
                    consumeProduct(asyncResponse, productsFound, productFound);
                },
                asyncResponse::resume);
    }

    private void consumeProduct(AsyncResponse asyncResponse, List<Product> productsFound, Product productFound) {
        productsFound.add(productFound);
        // this is needed if we do not want to assume that DAO is single threaded
        synchronized (productsFound) {
            if (productsFound.size() == 2 && ! asyncResponse.isDone()) {
                asyncResponse.resume(productsFound);
            }
        }
    }
}