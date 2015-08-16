package miccab.nonblocking;

import com.github.pgasync.ConnectionPoolBuilder;
import com.github.pgasync.Db;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import miccab.nonblocking.dao.ProductDao;
import miccab.nonblocking.dao.ProductDaoAsyncCallback;
import miccab.nonblocking.dao.ProductDaoAsyncFuture;
import miccab.nonblocking.dao.ProductDaoAsyncObservable;
import miccab.nonblocking.resources.*;
import org.skife.jdbi.v2.DBI;

import java.util.concurrent.Executors;

/**
 * Created by michal on 26.07.15.
 */
public class ProductApplication extends Application<ProductConfiguration> {

    public static void main(String[] args) throws Exception {
        new ProductApplication().run(args);
    }

    @Override
    public String getName() {
        return "product";
    }

    @Override
    public void initialize(Bootstrap<ProductConfiguration> bootstrap) {
    }

    @Override
    public void run(ProductConfiguration configuration,
                    Environment environment) {
        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");
        final ProductDao productDao = jdbi.onDemand(ProductDao.class);
        final Db pgAsyncDb = createPgAsyncDb(configuration.getDataSourceFactory());

        environment.jersey().register(new ProductResource(productDao));
        environment.jersey().register(new ProductHttpAsyncResource(productDao, Executors.newFixedThreadPool(32, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("mythread_%d").build())));
        environment.jersey().register(new ProductHttpDbAsyncResource(pgAsyncDb));
        environment.jersey().register(new ProductHttpDbAsyncWithCallbackResource(new ProductDaoAsyncCallback(pgAsyncDb)));
        environment.jersey().register(new ProductHttpDbAsyncWithSequentialCallbackResource(new ProductDaoAsyncCallback(pgAsyncDb)));
        environment.jersey().register(new ProductHttpDbAsyncWithParallelCallbackResource(new ProductDaoAsyncCallback(pgAsyncDb)));
        environment.jersey().register(new ProductHttpDbAsyncWithFutureResource(new ProductDaoAsyncFuture(pgAsyncDb)));
        environment.jersey().register(new ProductHttpDbAsyncWithSequentialFutureResource(new ProductDaoAsyncFuture(pgAsyncDb)));
        environment.jersey().register(new ProductHttpDbAsyncWithParallelFutureResource(new ProductDaoAsyncFuture(pgAsyncDb)));
        environment.jersey().register(new ProductHttpDbAsyncWithObservableResource(new ProductDaoAsyncObservable(pgAsyncDb)));
    }

    private Db createPgAsyncDb(DataSourceFactory dataSourceFactory) {
        return new ConnectionPoolBuilder()
                .hostname("localhost")
                .poolSize(dataSourceFactory.getMaxSize())
                .database("dropwizard")
                .username(dataSourceFactory.getUser())
                .password(dataSourceFactory.getPassword())
                .build();
    }
}
