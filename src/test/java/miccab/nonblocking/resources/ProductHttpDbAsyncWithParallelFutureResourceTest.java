package miccab.nonblocking.resources;

import miccab.nonblocking.dao.ProductDaoAsyncFuture;
import miccab.nonblocking.model.Product;
import miccab.nonblocking.model.ProductWithGroups;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.unitils.reflectionassert.ReflectionAssert;

import javax.ws.rs.container.AsyncResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
public class ProductHttpDbAsyncWithParallelFutureResourceTest {
    @Mock
    ProductDaoAsyncFuture dao;

    @Mock
    AsyncResponse asyncResponse;

    ProductHttpDbAsyncWithParallelFutureResource resource;

    @Before
    public void setUp() {
        resource = new ProductHttpDbAsyncWithParallelFutureResource(dao);
    }

    @Test
    public void shouldResumeSuccessfullyWhenProductWithNoGroupsFound() {
        when(dao.findNameById(1)).thenReturn(CompletableFuture.completedFuture(createProduct("cola", 1)));
        when(dao.findProductGroupsById(1)).thenReturn(CompletableFuture.completedFuture(emptyList()));

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
        when(dao.findNameById(1)).thenReturn(CompletableFuture.completedFuture(createProduct("cola", 1)));
        when(dao.findProductGroupsById(1)).thenReturn(CompletableFuture.completedFuture(asList(createGroup("g1", 2), createGroup("g2", 3))));

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
        CompletableFuture<Product> errorForProductNotFound = new CompletableFuture<>();
        errorForProductNotFound.completeExceptionally(new IllegalArgumentException("Your product not found"));

        when(dao.findNameById(1)).thenReturn(errorForProductNotFound);
        when(dao.findProductGroupsById(1)).thenReturn(CompletableFuture.completedFuture(emptyList()));

        resource.findById(1, asyncResponse);

        verify(asyncResponse).resume(argThat(new ArgumentMatcher<Throwable>() {
            @Override
            public boolean matches(Object argument) {
                // NOTICE: cause exception is wrapped
                assertTrue(argument instanceof CompletionException);
                final CompletionException exception = (CompletionException) argument;
                assertTrue(exception.getCause() instanceof IllegalArgumentException);
                return (exception.getCause()).getMessage().equals("Your product not found");
            }
        }));
    }
}
