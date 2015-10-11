package miccab.nonblocking.resources;

import miccab.nonblocking.dao.ProductDaoAsyncObservable;
import miccab.nonblocking.model.ProductGroup;
import miccab.nonblocking.model.ProductWithGroups;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.unitils.reflectionassert.ReflectionAssert;
import rx.Observable;

import javax.ws.rs.container.AsyncResponse;


import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static miccab.nonblocking.model.Product.createProduct;
import static miccab.nonblocking.model.ProductGroup.createGroup;
import static miccab.nonblocking.model.ProductWithGroups.createProductWithGroups;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by michal on 11.10.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProductHttpDbAsyncWithParallelObservableResourceTest {
    @Mock
    ProductDaoAsyncObservable dao;

    @Mock
    AsyncResponse asyncResponse;

    ProductHttpDbAsyncWithParallelObservableResource resource;

    @Before
    public void setUp() {
        resource = new ProductHttpDbAsyncWithParallelObservableResource(dao);
    }

    @Test
    public void shouldResumeSuccessfullyWhenProductWithNoGroupsFound() {
        when(dao.findNameById(1)).thenReturn(Observable.just(createProduct("cola", 1)));
        when(dao.findProductGroupsById(1)).thenReturn(Observable.<ProductGroup>empty());

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
        when(dao.findNameById(1)).thenReturn(Observable.just(createProduct("cola", 1)));
        when(dao.findProductGroupsById(1)).thenReturn(Observable.just(createGroup("g1", 2), createGroup("g2", 3)));

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
        when(dao.findNameById(1)).thenReturn(Observable.error(new IllegalArgumentException("Your product not found")));
        when(dao.findProductGroupsById(1)).thenReturn(Observable.empty());

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
