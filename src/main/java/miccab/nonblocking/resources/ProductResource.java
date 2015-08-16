package miccab.nonblocking.resources;

import com.codahale.metrics.annotation.Timed;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.dao.ProductDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Created by michal on 02.08.15.
 */
@Path("/product")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {
    private static final Logger LOG = LoggerFactory.getLogger(ProductResource.class);
    private final ProductDao productDao;

    public ProductResource(ProductDao productDao) {
        this.productDao = productDao;
    }

    @GET
    @Timed
    public Product findById(@QueryParam("id") int id) {
        LOG.trace("Finding product by id:{}", id);
        final Product product = new Product();
        product.setId(id);
        product.setName(productDao.findNameById(id));
        return product;
    }
}