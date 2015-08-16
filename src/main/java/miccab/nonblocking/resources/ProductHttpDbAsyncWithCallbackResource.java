package miccab.nonblocking.resources;

import miccab.nonblocking.dao.ProductDaoAsyncCallback;

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
@Path("/productHttpDbAsyncWithCallback")
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpDbAsyncWithCallbackResource {
    private final ProductDaoAsyncCallback productDao;

    public ProductHttpDbAsyncWithCallbackResource(ProductDaoAsyncCallback productDao) {
        this.productDao = productDao;
    }

    @GET
    public void findById(@QueryParam("id") int id, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        productDao.findNameById(id,
                       asyncResponse::resume,
                       asyncResponse::resume);
    }
}