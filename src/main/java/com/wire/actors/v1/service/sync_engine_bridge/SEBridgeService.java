package com.wire.actors.v1.service.sync_engine_bridge;

import com.wire.actors.v1.common.Utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SEBridgeService {
    private final DevicesCache devicesCache;
    private static SEBridgeService instance = null;
    static final long TTL_INFINITE = Long.MAX_VALUE;
    public static final long TTL_DEFAULT = Utils.getProperty("defaultDeviceTTL", Long.class);

    private static class DeviceWithTTL {
        private long msTTL;
        private long msTTLRefreshTimestamp;
        private CachedDevice cachedDevice;

        DeviceWithTTL(CachedDevice cachedDevice, long msTTL) {
            this.cachedDevice = cachedDevice;
            refreshExpiration(msTTL);
        }

        DeviceWithTTL refreshExpiration(long newMsTTL) {
            this.msTTL = newMsTTL;
            this.msTTLRefreshTimestamp = System.currentTimeMillis();
            return this;
        }

        long getTTL() {
            return this.msTTL;
        }

        long getMsToExpire() {
            if (this.msTTL == TTL_INFINITE) {
                return TTL_INFINITE;
            }
            return this.msTTL - (System.currentTimeMillis() - this.msTTLRefreshTimestamp);
        }

        CachedDevice getCachedDevice() {
            return this.cachedDevice;
        }
    }

    private final Map<String, DeviceWithTTL> registeredDevices = new HashMap<>();
    private final Semaphore devicesMappingGuard = new Semaphore(1);
    private final ScheduledExecutorService outdatedDevicesChecker = Executors.newScheduledThreadPool(1);
    private static final long EXPIRED_DEVICES_CHECK_INTERVAL_SEC = 120;

    {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private SEBridgeService() throws Exception {
        this.devicesCache = new DevicesCache();
        outdatedDevicesChecker.scheduleAtFixedRate(
                () -> {
                    try {
                        this.releaseExpiredDevices();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, 60, EXPIRED_DEVICES_CHECK_INTERVAL_SEC, TimeUnit.SECONDS
        );
    }

    public static synchronized SEBridgeService getInstance() {
        if (instance == null) {
            try {
                instance = new SEBridgeService();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    private void shutdown() {
        try {
            outdatedDevicesChecker.shutdownNow();
            getDevicesCache().shutdown();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private CachedDevice registerDevice(CachedDevice device, long msTTL) throws InterruptedException {
        this.devicesMappingGuard.acquire();
        try {
            registeredDevices.put(device.getUUID(), new DeviceWithTTL(device, msTTL));
            return device;
        } finally {
            this.devicesMappingGuard.release();
        }
    }

    public List<CachedDevice> getRegisteredDevices() throws InterruptedException {
        this.devicesMappingGuard.acquire();
        try {
            return registeredDevices.values().stream()
                    .map(DeviceWithTTL::getCachedDevice)
                    .collect(Collectors.toList());
        } finally {
            this.devicesMappingGuard.release();
        }
    }

    public List<CachedDevice> getRegisteredDevices(String... uuids) throws InterruptedException {
        this.devicesMappingGuard.acquire();
        try {
            return Stream.of(uuids)
                    .filter(registeredDevices::containsKey)
                    .map(registeredDevices::get)
                    .map(x -> x.refreshExpiration(x.getTTL()))
                    .map(DeviceWithTTL::getCachedDevice)
                    .collect(Collectors.toList());
        } finally {
            this.devicesMappingGuard.release();
        }
    }

    private void unregisterDevices(String... uuids) throws InterruptedException {
        this.devicesMappingGuard.acquire();
        try {
            final List<String> matchingIds = Stream.of(uuids)
                    .filter(registeredDevices::containsKey)
                    .collect(Collectors.toList());
            matchingIds.forEach(registeredDevices::remove);
        } finally {
            this.devicesMappingGuard.release();
        }
    }

    public CachedDevice addDevice(long msTTL) throws Exception {
        return this.registerDevice(getDevicesCache().addDevice(), msTTL);
    }

    public CachedDevice addDevice(String name, long msTTL) throws Exception {
        return this.registerDevice(getDevicesCache().addDevice(name), msTTL);
    }

    private static final int DEVICE_RELEASE_TIMEOUT_SEC = 100;

    public void removeDevices(String... uuids) throws InterruptedException {
        try {
            unregisterDevices(uuids);
        } finally {
            if (uuids.length == 1) {
                getDevicesCache().releaseDevice(uuids[0]);
            } else if (uuids.length > 1) {
                getDevicesCache().releaseDevicesAsync(uuids).ifPresent(x -> {
                    try {
                        x.awaitTermination(DEVICE_RELEASE_TIMEOUT_SEC, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private DevicesCache getDevicesCache() {
        return this.devicesCache;
    }

    private void releaseExpiredDevices() throws InterruptedException {
        final List<String> matchingDeviceIds;
        this.devicesMappingGuard.acquire();
        try {
            matchingDeviceIds = registeredDevices.entrySet().stream()
                    .map(Map.Entry::getValue)
                    .filter(x -> x.getMsToExpire() <= 0)
                    .map(x -> x.getCachedDevice().getUUID())
                    .collect(Collectors.toList());
            if (matchingDeviceIds.isEmpty()) {
                return;
            }
            matchingDeviceIds.forEach(registeredDevices::remove);
        } finally {
            this.devicesMappingGuard.release();
        }
        getDevicesCache().releaseDevicesAsync(matchingDeviceIds.toArray(new String[0]));
    }
}
