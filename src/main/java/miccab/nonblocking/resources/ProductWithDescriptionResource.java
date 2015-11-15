package miccab.nonblocking.resources;

import com.codahale.metrics.annotation.Timed;
import miccab.nonblocking.dao.ProductDao;
import miccab.nonblocking.model.Product;
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
@Path("/productWithDescription")
@Produces(MediaType.APPLICATION_JSON)
public class ProductWithDescriptionResource {
    private static final Logger LOG = LoggerFactory.getLogger(ProductWithDescriptionResource.class);
    private final ProductDao productDao;

    public ProductWithDescriptionResource(ProductDao productDao) {
        this.productDao = productDao;
    }

    @GET
    @Timed
    public Product findById(@QueryParam("id") int id) {
        LOG.trace("Finding product by id:{}", id);
        final Product product = new Product();
        product.setId(id);
        final String productDescription = productDao.findProductDescription(id);
        product.setName(productDescription);
        return product;
    }
}