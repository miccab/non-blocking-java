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
import java.util.concurrent.atomic.AtomicInteger;

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
        final AtomicInteger pendingData = new AtomicInteger(2);
        final ProductData productData = new ProductData();
        productDao.findNameById(id,
                productFound -> consumeProduct(asyncResponse, productFound, productData, pendingData),
                asyncResponse::resume);
        productDao.findProductGroupsById(id,
                productGroupsFound -> consumeProductGroups(asyncResponse, productGroupsFound, productData, pendingData),
                asyncResponse::resume);
    }

    private void consumeProduct(AsyncResponse asyncResponse, Product productFound, ProductData productData, AtomicInteger pendingData) {
        productData.product = productFound;
        // this is needed if we do not want to assume that DAO is single threaded
        if (pendingData.decrementAndGet() == 0) {
            asyncResponse.resume(ProductWithGroups.createProductWithGroups(productFound, productData.productGroups));
        }
    }

    private void consumeProductGroups(AsyncResponse asyncResponse, List<ProductGroup> productGroupsFound, ProductData productData, AtomicInteger pendingData) {
        productData.productGroups = productGroupsFound;
        // this is needed if we do not want to assume that DAO is single threaded
        if (pendingData.decrementAndGet() == 0) {
            asyncResponse.resume(ProductWithGroups.createProductWithGroups(productData.product, productGroupsFound));
        }
    }
}

class ProductData {
    volatile Product product;
    volatile List<ProductGroup> productGroups;
}