package miccab.nonblocking.resources;

import miccab.nonblocking.dao.ProductDaoAsyncObservable;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.model.ProductGroup;
import miccab.nonblocking.model.ProductWithGroups;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsyncWithParallelObservable")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithParallelObservableResource {
    private final ProductDaoAsyncObservable productDao;

    public ProductHttpDbAsyncWithParallelObservableResource(ProductDaoAsyncObservable productDao) {
        this.productDao = productDao;
    }

    @GET
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        final Observable<Product> product = productDao.findNameById(id);
        final Observable<ProductGroup> productGroupsIndividual = productDao.findProductGroupsById(id);
        final Observable<List<ProductGroup>> productGroups = productGroupsIndividual.collect(ArrayList::new, List::add);
        final Observable<ProductWithGroups> finalResult = product.zipWith(productGroups, ProductWithGroups::createProductWithGroups);
        finalResult.subscribe(asyncResponse::resume, asyncResponse::resume);
    }
}