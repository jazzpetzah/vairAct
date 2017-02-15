package com.wire.actors.v1.service.sync_engine_bridge;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.wire.actors.v1.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;
import com.waz.provision.ActorMessage.ReleaseRemotes$;

class DevicesCache {
    private static final Logger LOG = LoggerFactory.getLogger(DevicesCache.class.getSimpleName());
    private static final FiniteDuration MAX_ACTION_DURATION = new FiniteDuration(100, TimeUnit.SECONDS);

    private static final int MAX_CACHE_SIZE = Utils.getProperty("maxDevicesCount", Integer.class);
    private static final Coordinator COORDINATOR = Coordinator.getInstance();
    private static final String BACKEND_TYPE = Utils.getProperty("backendType");
    private static final int NORMAL_DEVICES_CACHE_LOAD_FACTOR = Utils.getProperty(
            "normalDevicesCacheLoadFactor", Integer.class
    );
    private static final int MIN_DEVICES_CACHE_LOAD_FACTOR = Utils.getProperty(
            "minDevicesCacheLoadFactor", Integer.class
    );
    private static final boolean OTR_ONLY = true;
    private Map<CachedProcess, Optional<CachedDevice>> cachedDevices = new ConcurrentHashMap<>();
    private final Semaphore cachedDevicesGuard = new Semaphore(1);
    private final ExecutorService processCreationPool = Executors.newFixedThreadPool(Utils.getOptimalThreadsCount());
    private final ExecutorService deviceCreationPool = Executors.newFixedThreadPool(2);

    DevicesCache() throws InterruptedException {
        cachedDevicesGuard.acquire();
        try {
            createHostProcessPromises(getNormalCacheItemsCount())
                    .forEach(x -> cachedDevices.put(x, Optional.empty()));
        } finally {
            cachedDevicesGuard.release();
        }
    }

    private static int getNormalCacheItemsCount() {
        if (MAX_CACHE_SIZE * NORMAL_DEVICES_CACHE_LOAD_FACTOR / 100 > 0) {
            return MAX_CACHE_SIZE * NORMAL_DEVICES_CACHE_LOAD_FACTOR / 100;
        }
        return 1;
    }

    private static int getMinCacheItemsCount() {
        if (MAX_CACHE_SIZE * MIN_DEVICES_CACHE_LOAD_FACTOR / 100 > 0) {
            return MAX_CACHE_SIZE * MIN_DEVICES_CACHE_LOAD_FACTOR / 100;
        }
        return 1;
    }

    CachedDevice addDevice() {
        return addDevice("Device_" + UUID.randomUUID().toString().substring(0, 8));
    }

    CachedDevice addDevice(String deviceName) {
        LOG.info("Adding new device having device name " + deviceName);
        try {
            return putDeviceInCache(deviceName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized void shutdown() {
        if (COORDINATOR.getActorRef() == null) {
            return;
        }
        LOG.info("Shutting down device pool...");
        this.cachedDevices = null;
        COORDINATOR.getActorRef().tell(ReleaseRemotes$.MODULE$, null);
    }

    Optional<ExecutorService> releaseDevicesAsync(String... uuids) throws InterruptedException {
        if (uuids.length == 0) {
            return Optional.empty();
        }
        final ExecutorService pool = Executors.newFixedThreadPool(uuids.length);
        for (String uuid : uuids) {
            pool.submit(() -> {
                try {
                    releaseDevice(uuid);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        pool.shutdown();
        return Optional.of(pool);
    }

    void releaseDevice(String uuid) throws InterruptedException {
        CachedProcess key;
        cachedDevicesGuard.acquire();
        try {
            key = cachedDevices.entrySet().stream()
                    .filter(x -> x.getValue().isPresent() && x.getValue().get().getUUID().equals(uuid))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            if (key == null) {
                return;
            }
            cachedDevices.remove(key);
            if (cachedDevices.size() < getMinCacheItemsCount()) {
                LOG.info(String.format("Creating %d new processes to be acquired by new devices...",
                        getNormalCacheItemsCount()));
                createHostProcessPromises(getNormalCacheItemsCount() - cachedDevices.size()).forEach(
                        x -> cachedDevices.put(x, Optional.empty())
                );
            } else {
                LOG.info("No new processes are going to be created since cache size has not reached %d%% load factor yet",
                        NORMAL_DEVICES_CACHE_LOAD_FACTOR);
            }
            LOG.info(String.format("Current cache size: %d item(s)", cachedDevices.size()));
        } finally {
            cachedDevicesGuard.release();
        }

        try {
            final IRemoteProcess dstProcess = key.getProcess();
            LOG.info(String.format("Destroying host process '%s' for the device '%s'...", dstProcess.name(), uuid));
            dstProcess.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.info(String.format("The device with uuid '%s' has been successfully removed from the cache", uuid));
    }

    private List<CachedProcess> createHostProcessPromises(int count) {
        final List<CachedProcess> result = new CopyOnWriteArrayList<>();
        for (int i = 0; i < count; i++) {
            final String processName = generateUniqueName();
            final Future<IRemoteProcess> newProcessPromise = processCreationPool.submit(
                    () -> this.createNewProcess(processName)
            );
            result.add(new CachedProcess(processName, newProcessPromise));
        }
        return result;
    }

    private CachedDevice putDeviceInCache(final String deviceName) throws Exception {
        CachedProcess hostProcess;
        final CachedDevice resultDevice = new CachedDevice(deviceName);
        cachedDevicesGuard.acquire();
        try {
            final List<CachedProcess> hostProcessCandidates = cachedDevices.entrySet().stream()
                    .filter(x -> !x.getValue().isPresent())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            hostProcess = hostProcessCandidates.stream()
                    .filter(x -> x.getProcessPromise().isDone())
                    .findFirst().orElse(null);
            if (hostProcess == null && !hostProcessCandidates.isEmpty()) {
                hostProcess = hostProcessCandidates.get(0);
            }
            if (hostProcess == null) {
                if (cachedDevices.size() < MAX_CACHE_SIZE) {
                    final String processName = generateUniqueName();
                    LOG.info(String.format("Attaching device '%s' to a new process '%s'...", deviceName, processName));
                    final Future<IRemoteProcess> newProcessPromise = processCreationPool.submit(
                            () -> this.createNewProcess(processName)
                    );
                    hostProcess = new CachedProcess(processName, newProcessPromise);
                } else {
                    throw new IllegalStateException(String.format("Cannot create more than %s devices.",
                            MAX_CACHE_SIZE));
                }
            } else {
                LOG.info(String.format("Attaching device '%s' to the existing process '%s'...", deviceName,
                        hostProcess.getName()));
            }
            final CachedProcess keyProcess = hostProcess;
            final Future<IDevice> targetDevice = deviceCreationPool.submit(
                    () -> createNewDevice(keyProcess, deviceName)
            );
            resultDevice.setDevicePromise(targetDevice);
            resultDevice.setHostProcess(keyProcess);
            cachedDevices.put(keyProcess, Optional.of(resultDevice));
            LOG.info(String.format("The device with name '%s' and uuid '%s' has been successfully added into the cache.",
                    deviceName, resultDevice.getUUID()));
            LOG.info(String.format("Current cache size: %d item(s)", cachedDevices.size()));
        } finally {
            cachedDevicesGuard.release();
        }

        return resultDevice;
    }

    private static String generateUniqueName() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private IRemoteProcess createNewProcess(String name) {
        return new RemoteProcess(name, COORDINATOR.getActorRef(), BACKEND_TYPE, OTR_ONLY);
    }

    private IDevice createNewDevice(CachedProcess hostProcess, String name) {
        final IRemoteProcess process;
        try {
            process = hostProcess.getProcessPromise().get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return new Device(process, name, COORDINATOR.getActorRef(), MAX_ACTION_DURATION);
    }
}
