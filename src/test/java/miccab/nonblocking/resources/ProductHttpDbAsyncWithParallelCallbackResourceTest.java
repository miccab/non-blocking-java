package miccab.nonblocking.resources;

import miccab.nonblocking.dao.ProductDaoAsyncCallback;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.model.ProductGroup;
import miccab.nonblocking.model.ProductWithGroups;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.unitils.reflectionassert.ReflectionAssert;

import javax.ws.rs.container.AsyncResponse;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static miccab.nonblocking.model.Product.createProduct;
import static miccab.nonblocking.model.ProductGroup.createGroup;
import static miccab.nonblocking.model.ProductWithGroups.createProductWithGroups;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

/**
 * Created by michal on 11.10.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProductHttpDbAsyncWithParallelCallbackResourceTest {
    @Mock
    ProductDaoAsyncCallback dao;

    @Mock
    AsyncResponse asyncResponse;

    ProductHttpDbAsyncWithParallelCallbackResource resource;

    @Before
    public void setUp() {
        resource = new ProductHttpDbAsyncWithParallelCallbackResource(dao);
    }

    @Test
    public void shouldResumeSuccessfullyWhenProductWithNoGroupsFound() {
        doNothing().when(dao).findNameById(eq(1), argThat(new ArgumentMatcher<Consumer<Product>>() {
            @Override
            public boolean matches(Object argument) {
                ((Consumer<Product>) argument).accept(createProduct("cola", 1));
                return true;
            }
        }), any());
        doNothing().when(dao).findProductGroupsById(eq(1), argThat(new ArgumentMatcher<Consumer<List<ProductGroup>>>() {
            @Override
            public boolean matches(Object argument) {
                ((Consumer<List<ProductGroup>>)argument).accept(emptyList());
                return false;
            }
        }), any());

        resource.findById(1, asyncResponse);

        verify(asyncResponse).resume(argThat(new ArgumentMatcher<ProductWithGroups>() {
            @Override
            public boolean matches(Object argument) {
                final ProductWithGroups expected = createProductWithGroups(createProduct("cola", 1), emptyList());
                ReflectionAssert.assertReflectionEquals(expected, argument);
                return true;
            }
        }));
    }

    @Test
    public void shouldResumeSuccessfullyWhenProductWithTwoGroupsFound() {
        doNothing().when(dao).findNameById(eq(1), argThat(new ArgumentMatcher<Consumer<Product>>() {
            @Override
            public boolean matches(Object argument) {
                ((Consumer<Product>) argument).accept(createProduct("cola", 1));
                return true;
            }
        }), any());
        doNothing().when(dao).findProductGroupsById(eq(1), argThat(new ArgumentMatcher<Consumer<List<ProductGroup>>>() {
            @Override
            public boolean matches(Object argument) {
                ((Consumer<List<ProductGroup>>)argument).accept(asList(createGroup("g1", 2), createGroup("g2", 3)));
                return false;
            }
        }), any());

        resource.findById(1, asyncResponse);

        verify(asyncResponse).resume(argThat(new ArgumentMatcher<ProductWithGroups>() {
            @Override
            public boolean matches(Object argument) {
                final ProductWithGroups expected = createProductWithGroups(createProduct("cola", 1), asList(createGroup("g1", 2), createGroup("g2", 3)));
                ReflectionAssert.assertReflectionEquals(expected, argument);
                return true;
            }
        }));
    }

    @Test
    public void shouldResumeWithErrorWhenProductNotFound() {
        doNothing().when(dao).findNameById(eq(1), any(), argThat(new ArgumentMatcher<Consumer<Throwable>>() {
            @Override
            public boolean matches(Object argument) {
                ((Consumer<Throwable>) argument).accept(new IllegalArgumentException("Your product not found"));
                return true;
            }
        }));
        doNothing().when(dao).findProductGroupsById(eq(1), argThat(new ArgumentMatcher<Consumer<List<ProductGroup>>>() {
            @Override
            public boolean matches(Object argument) {
                ((Consumer<List<ProductGroup>>)argument).accept(emptyList());
                return false;
            }
        }), any());

        resource.findById(1, asyncResponse);

        verify(asyncResponse).resume(argThat(new ArgumentMatcher<Throwable>() {
            @Override
            public boolean matches(Object argument) {
                assertTrue(argument instanceof IllegalArgumentException);
                return ((IllegalArgumentException)argument).getMessage().equals("Your product not found");
            }
        }));
    }
}
