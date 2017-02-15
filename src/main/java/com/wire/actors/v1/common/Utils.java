package com.wire.actors.v1.common;

import org.apache.commons.codec.binary.Base64;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import static org.apache.commons.codec.binary.Base64.isArrayByteBase64;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class.getSimpleName());

    private static final String CONFIG_NAME = "main.properties";

    public static String getProperty(String name) {
        return getProperty(name, String.class);
    }

    public static <T> T getProperty(String name, Class<T> expectedType) {
        final Properties prop = new Properties();
        try (InputStream in = Utils.class.getClassLoader().getResourceAsStream(CONFIG_NAME)) {
            prop.load(in);
            final Object result = prop.get(name);
            if (result == null) {
                throw new IllegalStateException(String.format(
                        "Cannot find '%s' property in the main configuration file '%s'", name, CONFIG_NAME
                ));
            }
            if (expectedType.isAssignableFrom(Integer.class)) {
                return expectedType.cast(Integer.parseInt((String) result));
            } else if (expectedType.isAssignableFrom(Long.class)) {
                return expectedType.cast(Long.parseLong((String) result));
            }
            return expectedType.cast(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method creates a temporary folder containing the given fileName if it is set
     * or just a single temporary file if it is not set
     */
    public static Optional<File> base64StringToFile(String s, String mimeType, Optional<String> fileName)
            throws Exception {
        final byte[] bytes = s.getBytes();
        if (!isArrayByteBase64(bytes)) {
            return Optional.empty();
        }
        final File tmpFile;
        if (fileName.isPresent()) {
            final Path tmpRoot = Files.createTempDirectory("tmp_");
            tmpFile = new File(String.format("%s/%s", tmpRoot.toFile().getAbsolutePath(), fileName.get()));
        } else {
            final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            final MimeType mt = allTypes.forName(mimeType);
            final String extension = mt.getExtension();
            tmpFile = File.createTempFile("file_", extension.isEmpty() ? ".bin" : extension);
        }
        try (OutputStream stream = new FileOutputStream(tmpFile)) {
            stream.write(Base64.decodeBase64(bytes));
        }
        return Optional.of(tmpFile);
    }

    public static int getOptimalThreadsCount() {
        final int coresCount = Runtime.getRuntime().availableProcessors();
        return coresCount > 3 ? coresCount - 2 : 2;
    }

    private static final File CPUTHROTTLE_TOOL = new File("/usr/local/bin/cputhrottle");
    private static final String SUDO_PATH = "/usr/bin/sudo";

    /**
     * @param maxCpuUsage max CPU usage value in range 1..100
     */
    public static void throttleProcess(Process proc, int maxCpuUsage) throws Exception {
        if (!CPUTHROTTLE_TOOL.exists()) {
            throw new IllegalStateException(String.format(
                    "cputhrottle tool is expected to be installed on host %s at %s (brew install cputhrottle)",
                    InetAddress.getLocalHost().getHostName(), CPUTHROTTLE_TOOL.getAbsolutePath()));
        }
        if (!proc.isAlive()) {
            return;
        }
        if (proc.getClass().getName().equals("java.lang.UNIXProcess")) {
            Field f = proc.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            final String pid = "" + f.getInt(proc);
            final String[] cmd = new String[]{
                    SUDO_PATH,
                    CPUTHROTTLE_TOOL.getAbsolutePath(), pid, "" + maxCpuUsage
            };
            LOG.info(String.format("Throttling remote process with PID %s by executing %s", pid, Arrays.toString(cmd)));
            new ProcessBuilder(cmd).start();
        } else {
            throw new IllegalStateException("Process throttling is only supported on *nix machines");
        }
    }
}
