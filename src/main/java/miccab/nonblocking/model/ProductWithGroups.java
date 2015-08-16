package miccab.nonblocking.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by michal on 16.08.15.
 */
public class ProductWithGroups {
    private Product product;
    private List<ProductGroup> productGroups;

    @JsonProperty
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @JsonProperty
    public List<ProductGroup> getProductGroups() {
        return productGroups;
    }

    public void setProductGroups(List<ProductGroup> productGroups) {
        this.productGroups = productGroups;
    }

    public static ProductWithGroups createProductWithGroups(Product product, List<ProductGroup> productGroups) {
        final ProductWithGroups productWithGroups = new ProductWithGroups();
        productWithGroups.setProduct(product);
        productWithGroups.setProductGroups(productGroups);
        return productWithGroups;
    }
}
