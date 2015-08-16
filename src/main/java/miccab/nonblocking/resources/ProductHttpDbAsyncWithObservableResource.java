package miccab.nonblocking.resources;

import miccab.nonblocking.model.Product;
import miccab.nonblocking.dao.ProductDaoAsyncObservable;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.TimeUnit;

/**
 * Created by michal on 02.08.15.
 */
@Path("/productHttpDbAsyncWithObservable")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithObservableResource {
    private final ProductDaoAsyncObservable productDao;

    public ProductHttpDbAsyncWithObservableResource(ProductDaoAsyncObservable productDao) {
        this.productDao = productDao;
    }

    @GET
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        final Observable<Product> productFound = productDao.findNameById(id);
        productFound.subscribe(asyncResponse::resume,
                               asyncResponse::resume);
    }
}