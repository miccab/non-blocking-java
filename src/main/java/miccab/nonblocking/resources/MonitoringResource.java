package miccab.nonblocking.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by michal on 25.10.15.
 */
@Path("/monitoring")
@Produces(MediaType.APPLICATION_JSON)
public class MonitoringResource {
    private static final Logger LOG = LoggerFactory.getLogger(MonitoringResource.class);
    public static final String MONITORING_SCRIPT = "./gatling/src/test/bash/monitor.sh";

    @POST
    public String executeMonitoring(@FormParam("operation")  String operation, @FormParam("name")  String name) {
        LOG.info("Request to execute monitoring operation: {}", operation);
        switch (operation.toLowerCase()) {
            case "start":
                doExecuteMonitoring("start_java", name);
                break;
            case "stop":
                doExecuteMonitoring("stop", name);
                break;
            default:
                throw new IllegalArgumentException("Unknown operation " + operation);
        }
        return "Operation OK";
    }

    private void doExecuteMonitoring(String operation, String name) {
        final String pid = getPidForCurrentProcess();
        final ProcessBuilder processBuilder = new ProcessBuilder(MONITORING_SCRIPT, operation, String.valueOf(name), pid);
        try {
            final Process process = processBuilder.start();
            process.waitFor(10, TimeUnit.SECONDS);
            final String output = new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                    .reduce((accumulator, line) -> accumulator + "\n" + line).get();
            LOG.info("Output from monitoring script: {}", output);
            final int processExitCode = process.exitValue();
            if (processExitCode != 0) {
                throw new IllegalStateException("Exit code not successful: " + processExitCode);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getPidForCurrentProcess() {
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

}

