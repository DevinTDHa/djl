/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.pytorch.jni;

import ai.djl.util.ClassLoaderUtils;
import ai.djl.util.Platform;
import ai.djl.util.Utils;
import ai.djl.util.cuda.CudaUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Utilities for finding the PyTorch Engine binary on the System.
 *
 * <p>The Engine will be searched for in a variety of locations in the following order:
 *
 * <ol>
 *   <li>In the path specified by the PYTORCH_LIBRARY_PATH environment variable
 *   <li>In a jar file location in the classpath. These jars can be created with the pytorch-native
 *       module.
 * </ol>
 */
@SuppressWarnings("MissingJavadocMethod")
public final class LibUtils {

    private static final Logger logger = LoggerFactory.getLogger(LibUtils.class);

    private static final String NATIVE_LIB_NAME = System.mapLibraryName("torch");
    private static final String JNI_LIB_NAME = System.mapLibraryName("djl_torch");

    private static final Pattern VERSION_PATTERN =
            Pattern.compile("(\\d+\\.\\d+\\.\\d+(-[a-z]+)?)(-SNAPSHOT)?(-\\d+)?");

    private static LibTorch libTorch;

    private LibUtils() {}

    public static synchronized void loadLibrary() {
        // TODO workaround to make it work on Android Studio
        // It should search for several places to find the native library
        if ("http://www.android.com/".equals(System.getProperty("java.vendor.url"))) {
            System.loadLibrary("djl_torch"); // NOPMD
            return;
        }
        libTorch = getLibTorch();
        loadLibTorch(libTorch);

        Path path = findJniLibrary(libTorch).toAbsolutePath();
        loadNativeLibrary(path.toString());
    }

    private static LibTorch getLibTorch() {
        LibTorch lib = findOverrideLibrary();
        if (lib != null) {
            return lib;
        }
        return findNativeLibrary();
    }

    public static String getVersion() {
        Matcher m = VERSION_PATTERN.matcher(libTorch.version);
        if (m.matches()) {
            return m.group(1);
        }
        return libTorch.version;
    }

    public static String getLibtorchPath() {
        return libTorch.dir.toString();
    }

    private static void loadLibTorch(LibTorch libTorch) {
        Path libDir = libTorch.dir.toAbsolutePath();
        if ("1.8.1".equals(getVersion()) && System.getProperty("os.name").startsWith("Mac")) {
            // PyTorch 1.8.1 libtorch_cpu.dylib cannot be loaded individually
            return;
        }
        boolean isCuda = libTorch.flavor.contains("cu");
        List<String> deferred =
                Arrays.asList(
                        System.mapLibraryName("fbgemm"),
                        System.mapLibraryName("caffe2_nvrtc"),
                        System.mapLibraryName("torch_cpu"),
                        System.mapLibraryName("c10_cuda"),
                        System.mapLibraryName("torch_cuda_cpp"),
                        System.mapLibraryName("torch_cuda_cu"),
                        System.mapLibraryName("torch_cuda"),
                        System.mapLibraryName("torch"));

        Set<String> loadLater = new HashSet<>(deferred);
        try (Stream<Path> paths = Files.walk(libDir)) {
            paths.filter(
                            path -> {
                                String name = path.getFileName().toString();
                                if (!isCuda
                                        && name.contains("nvrtc")
                                        && name.contains("cudart")
                                        && name.contains("nvTools")) {
                                    return false;
                                }
                                return !loadLater.contains(name)
                                        && Files.isRegularFile(path)
                                        && !name.endsWith(JNI_LIB_NAME)
                                        && !name.contains("torch_")
                                        && !name.contains("caffe2_")
                                        && !name.startsWith("cudnn");
                            })
                    .map(Path::toString)
                    .forEach(LibUtils::loadNativeLibrary);
            if (Files.exists((libDir.resolve("cudnn64_8.dll")))) {
                loadNativeLibrary(libDir.resolve("cudnn64_8.dll").toString());
                loadNativeLibrary(libDir.resolve("cudnn_ops_infer64_8.dll").toString());
                loadNativeLibrary(libDir.resolve("cudnn_ops_train64_8.dll").toString());
                loadNativeLibrary(libDir.resolve("cudnn_cnn_infer64_8.dll").toString());
                loadNativeLibrary(libDir.resolve("cudnn_cnn_train64_8.dll").toString());
                loadNativeLibrary(libDir.resolve("cudnn_adv_infer64_8.dll").toString());
                loadNativeLibrary(libDir.resolve("cudnn_adv_train64_8.dll").toString());
            } else if (Files.exists((libDir.resolve("cudnn64_7.dll")))) {
                loadNativeLibrary(libDir.resolve("cudnn64_7.dll").toString());
            }

            if (!isCuda) {
                deferred =
                        Arrays.asList(
                                System.mapLibraryName("fbgemm"),
                                System.mapLibraryName("torch_cpu"),
                                System.mapLibraryName("torch"));
            }

            for (String dep : deferred) {
                Path path = libDir.resolve(dep);
                if (Files.exists(path)) {
                    loadNativeLibrary(path.toString());
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Folder not exist! " + libDir, e);
        }
    }

    private static LibTorch findOverrideLibrary() {
        String libPath = System.getenv("PYTORCH_LIBRARY_PATH");
        if (libPath != null) {
            LibTorch lib = findLibraryInPath(libPath);
            if (lib != null) {
                return lib;
            }
        }

        libPath = System.getProperty("java.library.path");
        if (libPath != null) {
            return findLibraryInPath(libPath);
        }
        return null;
    }

    private static LibTorch findLibraryInPath(String libPath) {
        String[] paths = libPath.split(File.pathSeparator);
        for (String path : paths) {
            File p = new File(path);
            if (!p.exists()) {
                continue;
            }

            if (p.isFile() && NATIVE_LIB_NAME.equals(p.getName())) {
                return new LibTorch(p.getParentFile().toPath().toAbsolutePath());
            }

            File file = new File(path, NATIVE_LIB_NAME);
            if (file.exists() && file.isFile()) {
                return new LibTorch(p.toPath().toAbsolutePath());
            }
        }
        return null;
    }

    private static Path findJniLibrary(LibTorch libTorch) {
        String classifier = libTorch.classifier;
        String version = libTorch.version;
        String djlVersion = libTorch.apiVersion;
        String flavor = libTorch.flavor;

        // always use cache dir, cache dir might be different from libTorch.dir
        Path cacheDir = Utils.getEngineCacheDir("pytorch");
        Path dir = cacheDir.resolve(version + '-' + flavor + '-' + classifier);
        Path path = dir.resolve(djlVersion + '-' + JNI_LIB_NAME);
        if (Files.exists(path)) {
            return path;
        }

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unexpected version: " + version);
        }
        version = matcher.group(1);

        try {
            URL url = ClassLoaderUtils.getResource("jnilib/pytorch.properties");
            String jniVersion = null;
            if (url != null) {
                Properties prop = new Properties();
                try (InputStream is = url.openStream()) {
                    prop.load(is);
                }
                jniVersion = prop.getProperty("jni_version");
                if (jniVersion == null) {
                    throw new AssertionError("No PyTorch jni version found.");
                }
            }
            if (jniVersion == null) {
                downloadJniLib(dir, path, djlVersion, version, classifier, flavor);
                return path;
            } else if (!jniVersion.startsWith(version + '-' + djlVersion)) {
                logger.warn("Found mismatch PyTorch jni: {}", jniVersion);
                downloadJniLib(dir, path, djlVersion, version, classifier, flavor);
                return path;
            }
        } catch (IOException e) {
            throw new AssertionError("Failed to read PyTorch jni properties file.", e);
        }

        Path tmp = null;
        String libPath = "jnilib/" + classifier + '/' + flavor + '/' + JNI_LIB_NAME;
        logger.info("Extracting {} to cache ...", libPath);
        try (InputStream is = ClassLoaderUtils.getResourceAsStream(libPath)) {
            tmp = Files.createTempFile(dir, "jni", "tmp");
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            Utils.moveQuietly(tmp, path);
            return path;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot copy jni files", e);
        } finally {
            if (tmp != null) {
                Utils.deleteQuietly(tmp);
            }
        }
    }

    private static LibTorch findNativeLibrary() {
        Platform platform = Platform.detectPlatform("pytorch");
        String overrideVersion = System.getenv("PYTORCH_VERSION");
        if (overrideVersion == null) {
            overrideVersion = System.getProperty("PYTORCH_VERSION");
        }
        if (overrideVersion != null
                && !overrideVersion.isEmpty()
                && !platform.getVersion().startsWith(overrideVersion)) {
            // platform.version can be 1.8.1-20210421
            logger.warn("Override PyTorch version: {}.", overrideVersion);
            platform = Platform.detectPlatform("pytorch", overrideVersion);
            return downloadPyTorch(platform);
        }

        if (platform.isPlaceholder()) {
            return downloadPyTorch(platform);
        }

        return copyNativeLibraryFromClasspath(platform);
    }

    private static LibTorch copyNativeLibraryFromClasspath(Platform platform) {
        String version = platform.getVersion();
        String flavor = platform.getFlavor();
        if (!flavor.endsWith("-precxx11")
                && Arrays.asList(platform.getLibraries()).contains("libstdc++.so.6")) {
            // for PyTorch 1.9.1 and older
            flavor += "-precxx11"; // NOPMD
        }
        String classifier = platform.getClassifier();

        Path tmp = null;
        try {
            Path cacheDir = Utils.getEngineCacheDir("pytorch");
            logger.debug("Using cache dir: {}", cacheDir);
            Path dir = cacheDir.resolve(version + '-' + flavor + '-' + classifier);
            Path path = dir.resolve(NATIVE_LIB_NAME);
            if (Files.exists(path)) {
                return new LibTorch(dir.toAbsolutePath(), platform, flavor);
            }
            Utils.deleteQuietly(dir);

            Matcher m = VERSION_PATTERN.matcher(version);
            if (!m.matches()) {
                throw new IllegalArgumentException("Unexpected version: " + version);
            }
            String[] versions = m.group(1).split("\\.");
            int minorVersion = Integer.parseInt(versions[1]);
            int buildVersion = Integer.parseInt(versions[2]);
            String pathPrefix;
            if (minorVersion > 10 || (minorVersion == 10 && buildVersion == 2)) {
                pathPrefix = "pytorch/" + flavor + '/' + classifier;
            } else {
                pathPrefix = "native/lib";
            }

            Files.createDirectories(cacheDir);
            tmp = Files.createTempDirectory(cacheDir, "tmp");
            for (String file : platform.getLibraries()) {
                String libPath = pathPrefix + '/' + file;
                logger.info("Extracting {} to cache ...", libPath);
                try (InputStream is = ClassLoaderUtils.getResourceAsStream(libPath)) {
                    Files.copy(is, tmp.resolve(file), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Utils.moveQuietly(tmp, dir);
            return new LibTorch(dir.toAbsolutePath(), platform, flavor);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract PyTorch native library", e);
        } finally {
            if (tmp != null) {
                Utils.deleteQuietly(tmp);
            }
        }
    }

    private static void loadNativeLibrary(String path) {
        logger.debug("Loading native library: {}", path);
        String nativeHelper = System.getProperty("ai.djl.pytorch.native_helper");
        if (nativeHelper != null && !nativeHelper.isEmpty()) {
            try {
                Class<?> clazz = Class.forName(nativeHelper);
                Method method = clazz.getDeclaredMethod("load", String.class);
                method.invoke(null, path);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Invalid native_helper: " + nativeHelper, e);
            }
        }
        System.load(path); // NOPMD
    }

    private static LibTorch downloadPyTorch(Platform platform) {
        String version = platform.getVersion();
        String flavor = platform.getFlavor();
        String classifier = platform.getClassifier();
        String precxx11;
        if (Boolean.getBoolean("PYTORCH_PRECXX11")
                || Boolean.parseBoolean(System.getenv("PYTORCH_PRECXX11"))
                || ("aarch64".equals(platform.getOsArch())
                        && "linux".equals(platform.getOsPrefix()))) {
            precxx11 = "-precxx11";
        } else {
            precxx11 = "";
        }

        flavor += precxx11;

        Path cacheDir = Utils.getEngineCacheDir("pytorch");
        Path dir = cacheDir.resolve(version + '-' + flavor + '-' + classifier);
        Path path = dir.resolve(NATIVE_LIB_NAME);
        if (Files.exists(path)) {
            logger.debug("Using cache dir: {}", dir);
            return new LibTorch(dir.toAbsolutePath(), platform, flavor);
        }

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unexpected version: " + version);
        }
        String link = "https://publish.djl.ai/pytorch/" + matcher.group(1);
        Path tmp = null;
        try (InputStream is = new URL(link + "/files.txt").openStream()) {
            // if files not found
            Files.createDirectories(cacheDir);
            List<String> lines = Utils.readLines(is);
            if (flavor.startsWith("cu")) {
                String cudaMajor = flavor.substring(0, 4);
                Pattern pattern =
                        Pattern.compile(
                                '('
                                        + cudaMajor
                                        + "\\d"
                                        + precxx11
                                        + ")/"
                                        + classifier
                                        + "/native/lib/"
                                        + NATIVE_LIB_NAME
                                        + ".gz");
                boolean match = false;
                for (String line : lines) {
                    Matcher m = pattern.matcher(line);
                    if (m.matches()) {
                        flavor = m.group(1);
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    logger.warn("No matching cuda flavor for {} found: {}.", classifier, flavor);
                    // fallback to CPU
                    flavor = "cpu" + precxx11;
                }

                // check again
                dir = cacheDir.resolve(version + '-' + flavor + '-' + classifier);
                path = dir.resolve(NATIVE_LIB_NAME);
                if (Files.exists(path)) {
                    return new LibTorch(dir.toAbsolutePath(), platform, flavor);
                }
            }

            logger.debug("Using cache dir: {}", dir);

            tmp = Files.createTempDirectory(cacheDir, "tmp");
            boolean found = false;
            for (String line : lines) {
                if (line.startsWith(flavor + '/' + classifier + '/')) {
                    found = true;
                    URL url = new URL(link + '/' + line);
                    String fileName = line.substring(line.lastIndexOf('/') + 1, line.length() - 3);
                    fileName = URLDecoder.decode(fileName, "UTF-8");
                    logger.info("Downloading {} ...", url);
                    try (InputStream fis = new GZIPInputStream(url.openStream())) {
                        Files.copy(fis, tmp.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            if (!found) {
                throw new IllegalStateException(
                        "No PyTorch native library matches your operating system: " + platform);
            }

            Utils.moveQuietly(tmp, dir);
            return new LibTorch(dir.toAbsolutePath(), platform, flavor);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to download PyTorch native library", e);
        } finally {
            if (tmp != null) {
                Utils.deleteQuietly(tmp);
            }
        }
    }

    private static void downloadJniLib(
            Path cacheDir,
            Path path,
            String djlVersion,
            String version,
            String classifier,
            String flavor) {
        String url =
                "https://publish.djl.ai/pytorch/"
                        + version
                        + "/jnilib/"
                        + djlVersion
                        + '/'
                        + classifier
                        + '/'
                        + flavor
                        + '/'
                        + JNI_LIB_NAME;
        logger.info("Downloading jni {} to cache ...", url);
        Path tmp = null;
        try (InputStream is = new URL(url).openStream()) {
            Files.createDirectories(cacheDir);
            tmp = Files.createTempFile(cacheDir, "jni", "tmp");
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            Utils.moveQuietly(tmp, path);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot download jni files: " + url, e);
        } finally {
            if (tmp != null) {
                Utils.deleteQuietly(tmp);
            }
        }
    }

    private static final class LibTorch {

        Path dir;
        String version;
        String apiVersion;
        String flavor;
        String classifier;

        LibTorch(Path dir) {
            Platform platform = Platform.detectPlatform("pytorch");
            this.dir = dir;
            this.apiVersion = platform.getApiVersion();
            this.classifier = platform.getClassifier();
            version = System.getenv("PYTORCH_VERSION");
            if (version == null) {
                version = System.getProperty("PYTORCH_VERSION");
                if (version == null) {
                    version = platform.getVersion();
                }
            }
            flavor = System.getenv("PYTORCH_FLAVOR");
            if (flavor == null) {
                flavor = System.getProperty("PYTORCH_FLAVOR");
                if (flavor == null) {
                    if (CudaUtils.getGpuCount() > 0) {
                        flavor = "cu" + CudaUtils.getCudaVersionString() + "-precxx11";
                    } else {
                        flavor = "cpu-precxx11";
                    }
                }
            }
        }

        LibTorch(Path dir, Platform platform, String flavor) {
            this.dir = dir;
            this.version = platform.getVersion();
            this.apiVersion = platform.getApiVersion();
            this.classifier = platform.getClassifier();
            this.flavor = flavor;
        }
    }
}
