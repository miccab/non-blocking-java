package miccab.nonblocking.resources;

import miccab.nonblocking.dao.ProductDaoAsyncObservable;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.model.ProductGroup;
import miccab.nonblocking.model.ProductWithGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsyncWithSequentialObservable")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithSequentialObservableResource {
    private static final Logger LOG = LoggerFactory.getLogger(ProductHttpDbAsyncWithSequentialObservableResource.class);
    private final ProductDaoAsyncObservable productDao;
    private final Scheduler schedulerToCompleteCalls;

    public ProductHttpDbAsyncWithSequentialObservableResource(ProductDaoAsyncObservable productDao, Executor executorToCompleteCalls) {
        this.productDao = productDao;
        this.schedulerToCompleteCalls = Schedulers.from(executorToCompleteCalls);
    }

    @GET
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        final Observable<Product> product = productDao.findNameById(id);
        final Observable<ProductWithGroups> finalResult = product.flatMap(productFound -> {
            if (asyncResponse.isDone()) {
                return Observable.error(new IllegalStateException("Response already done"));
            } else {
                final Observable<ProductGroup> productGroupsIndividual = productDao.findProductGroupsById(id);
                final Observable<List<ProductGroup>> productGroups = productGroupsIndividual.collect(ArrayList::new, List::add);
                return productGroups.map(productGroupsFound -> ProductWithGroups.createProductWithGroups(productFound, productGroupsFound));
            }
        });
        finalResult.observeOn(schedulerToCompleteCalls).subscribe(asyncResponse::resume, asyncResponse::resume);
    }
}