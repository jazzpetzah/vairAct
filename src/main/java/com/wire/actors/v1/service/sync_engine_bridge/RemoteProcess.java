package com.wire.actors.v1.service.sync_engine_bridge;

import akka.actor.ActorRef;
import akka.serialization.Serialization;

import com.waz.provision.ActorMessage;
import com.waz.provision.ActorMessage.WaitUntilRegistered;

import com.wire.actors.v1.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.gracefulStop;

class RemoteProcess extends RemoteEntity implements IRemoteProcess {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteProcess.class.getSimpleName());

    private static final FiniteDuration ACTOR_DURATION = new FiniteDuration(90, TimeUnit.SECONDS);

    private final ActorRef coordinatorActorRef;
    private final String backendType;
    private final boolean otrOnly;
    private volatile Optional<ExecutorService> pinger = Optional.empty();
    private volatile Optional<Process> currentProcess = Optional.empty();
    private final File log;

    {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    // Default actor lib TTL equals to 30 seconds
    private static final int PING_INTERVAL_SECONDS = 20;

    RemoteProcess(String processName, ActorRef coordinatorActorRef, String backendType, boolean otrOnly) {
        super(processName, ACTOR_DURATION);
        this.backendType = backendType;
        this.otrOnly = otrOnly;
        this.coordinatorActorRef = coordinatorActorRef;
        this.log = generateLogFile(processName);
        try {
            restart();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public File getLog() {
        return this.log;
    }

    private static final int MAX_CPU_USAGE = Utils.getProperty("maxActorCpuUsage", Integer.class);

    private void restartProcess() throws Exception {
        currentProcess.ifPresent(Process::destroyForcibly);
        final String serialized = Serialization.serializedActorPath(this.coordinatorActorRef);
        final String[] cmd = {"java", "-jar", getActorsJarLocation(),
                this.name(), serialized, backendType, String.valueOf(otrOnly)};
        LOG.info(String.format("Executing actors using the command line: %s", Arrays.toString(cmd)));
        final ProcessBuilder pb = new ProcessBuilder(cmd);
        // ! Having a log file is mandatory
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        LOG.info(String.format("Actor logs will be redirected to %s", log.getAbsolutePath()));
        this.currentProcess = Optional.of(pb.start());
        //noinspection OptionalGetWithoutIsPresent
        Utils.throttleProcess(this.currentProcess.get(), MAX_CPU_USAGE);
    }

    @Override
    public synchronized void restart() throws Exception {
        if (this.pinger.isPresent()) {
            this.pinger.get().shutdownNow();
            this.pinger = Optional.empty();
        }

        if (this.ref() != null) {
            try {
                Future<Boolean> stopped = gracefulStop(this.ref(), Duration.create(5, TimeUnit.SECONDS),
                        null);
                Await.result(stopped, Duration.create(6, TimeUnit.SECONDS));
                // the actor has been stopped
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.setRef(null);
        }

        restartProcess();

        try {
            final Object resp = askActor(this.coordinatorActorRef, new WaitUntilRegistered(this.name()));
            if (resp instanceof ActorRef) {
                this.setRef((ActorRef) resp);
                this.pinger = Optional.of(Executors.newSingleThreadExecutor());
                LOG.debug(String.format(
                        "Starting ping thread with %s-seconds interval for the remote process '%s'...",
                        PING_INTERVAL_SECONDS, name()));
                //noinspection OptionalGetWithoutIsPresent
                this.pinger.get().submit(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(PING_INTERVAL_SECONDS * 1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                        if (!isConnected()) {
                            break;
                        }
                    }
                    LOG.debug(String.format("Stopped ping thread for the remote process '%s'", name()));
                });
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalStateException(
                String.format("The coordinator actor failed to establish a connection with the remote "
                                + "process: %s. Please check the log file %s for more details.", name(),
                        this.log.getAbsolutePath()));
    }

    private String getActorsJarLocation() throws URISyntaxException {
        final File file = new File(ActorMessage.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath());
        return file.toString();
    }

    static synchronized File generateLogFile(String processName) {
        final File outputFile = new File(String.format("target/logcat/%s.log", processName));
        if (!Files.exists(Paths.get(outputFile.getParent()))) {
            //noinspection ResultOfMethodCallIgnored
            outputFile.getParentFile().mkdirs();
        }
        return outputFile;
    }

    @Override
    public boolean isOtrOnly() {
        return this.otrOnly;
    }

    @Override
    public void shutdown() {
        if (!pinger.isPresent()) {
            return;
        }

        try {
            this.pinger.get().shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.pinger = Optional.empty();
        currentProcess.ifPresent(Process::destroyForcibly);
        currentProcess = Optional.empty();

        this.setRef(null);
    }
}
