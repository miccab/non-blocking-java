package miccab.nonblocking.resources;

import miccab.nonblocking.model.Product;
import miccab.nonblocking.dao.ProductDaoAsyncCallback;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        // NOTICE: DAO now uses single threaded model. So we should not need any sync
        //         however, this is implementation detail which might change.
        //         so to be safe we use synchronization to protect from race conditions
        final AtomicReference<List<ProductGroup>> productGroups = new AtomicReference<>();
        final AtomicReference<Product> product = new AtomicReference<>();
        productDao.findNameById(id,
                productFound -> {
                    consumeProduct(asyncResponse, productFound, product, productGroups);
                },
                asyncResponse::resume);
        productDao.findProductGroupsById(id,
                productGroupsFound -> {
                    consumeProductGroups(asyncResponse, productGroupsFound, product, productGroups);
                },
                asyncResponse::resume);
    }

    private void consumeProduct(AsyncResponse asyncResponse, Product productFound, AtomicReference<Product> product, AtomicReference<List<ProductGroup>> productGroups) {
        product.set(productFound);
        // this is needed if we do not want to assume that DAO is single threaded
        final List<ProductGroup> productGroupsFound = productGroups.getAndSet(null);
        if (productGroupsFound != null) {
            asyncResponse.resume(ProductWithGroups.createProductWithGroups(productFound, productGroupsFound));
        }
    }

    private void consumeProductGroups(AsyncResponse asyncResponse, List<ProductGroup> productGroupsFound, AtomicReference<Product> product, AtomicReference<List<ProductGroup>> productGroups) {
        productGroups.set(productGroupsFound);
        // this is needed if we do not want to assume that DAO is single threaded
        final Product productFound = product.getAndSet(null);
        if (productFound != null) {
            asyncResponse.resume(ProductWithGroups.createProductWithGroups(productFound, productGroupsFound));
        }
    }
}