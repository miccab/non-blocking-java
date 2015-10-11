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

import java.util.concurrent.Executor;
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
        final Db pgAsyncDb = createPgAsyncDb(configuration);

        environment.jersey().register(new ProductResource(productDao));
        environment.jersey().register(new ProductHttpAsyncResource(productDao,
                                                                   Executors.newFixedThreadPool(configuration.getDataSourceFactory().getMaxSize(),
                                                                                                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("MyDbExecutionThread_%d").build())));
        environment.jersey().register(new ProductHttpDbAsyncResource(pgAsyncDb));
        final Executor executorToCompleteCalls = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("MyCompletionThread_%d").build());
        final ProductDaoAsyncCallback productDaoAsyncCallback = new ProductDaoAsyncCallback(pgAsyncDb);
        final ProductDaoAsyncFuture productDaoAsyncFuture = new ProductDaoAsyncFuture(pgAsyncDb);
        final ProductDaoAsyncObservable productDaoAsyncObservable = new ProductDaoAsyncObservable(pgAsyncDb);

        environment.jersey().register(new ProductHttpDbAsyncWithCallbackResource(productDaoAsyncCallback));
        environment.jersey().register(new ProductHttpDbAsyncWithFutureResource(productDaoAsyncFuture));
        environment.jersey().register(new ProductHttpDbAsyncWithObservableResource(productDaoAsyncObservable));

        environment.jersey().register(new ProductHttpDbAsyncWithSequentialCallbackResource(productDaoAsyncCallback, executorToCompleteCalls));
        environment.jersey().register(new ProductHttpDbAsyncWithSequentialFutureResource(productDaoAsyncFuture, executorToCompleteCalls));
        environment.jersey().register(new ProductHttpDbAsyncWithSequentialObservableResource(productDaoAsyncObservable, executorToCompleteCalls));

        environment.jersey().register(new ProductHttpDbAsyncWithParallelCallbackResource(productDaoAsyncCallback));
        environment.jersey().register(new ProductHttpDbAsyncWithParallelFutureResource(productDaoAsyncFuture));
        environment.jersey().register(new ProductHttpDbAsyncWithParallelObservableResource(productDaoAsyncObservable));
    }

    private Db createPgAsyncDb(ProductConfiguration productConfiguration) {
        final DataSourceFactory dataSourceFactory = productConfiguration.getDataSourceFactory();
        final DbUrlParts dbUrlParts = createDbUrlParts(dataSourceFactory.getUrl());
        return new ConnectionPoolBuilder()
                .hostname(dbUrlParts.getHost())
                .poolSize(dataSourceFactory.getMaxSize())
                .database(dbUrlParts.getName())
                .username(dataSourceFactory.getUser())
                .password(dataSourceFactory.getPassword())
                .build();
    }

    private DbUrlParts createDbUrlParts(String url) {
        final String [] parts = url.split("/");
        return new DbUrlParts(parts[2], parts[3]);
    }
}

class DbUrlParts {
    private final String host, name;

    DbUrlParts(String host, String name) {
        this.host = host;
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }
}
