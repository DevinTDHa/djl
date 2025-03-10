# DJL - PyTorch engine implementation

## Overview
This module contains the Deep Java Library (DJL) EngineProvider for PyTorch.

We don't recommend that developers use classes in this module directly.
Use of these classes will couple your code with PyTorch and make switching between frameworks difficult.

## Documentation

The latest javadocs can be found on [here](https://javadoc.io/doc/ai.djl.pytorch/pytorch-engine/latest/index.html).

You can also build the latest javadocs locally using the following command:

```sh
# for Linux/macOS:
./gradlew javadoc

# for Windows:
..\..\gradlew javadoc
```
The javadocs output is built in the `build/doc/javadoc` folder.

## Installation
You can pull the PyTorch engine from the central Maven repository by including the following dependency:

- ai.djl.pytorch:pytorch-engine:0.17.0

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-engine</artifactId>
    <version>0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

Since DJL 0.14.0, `pytorch-engine` can load older version of pytorch native library. There are two
ways to specify PyTorch version:

1. Explicitly specify `pytorch-native-xxx` package version to override the version in the [BOM](../../../bom/README.md).
2. Sets environment variable: `PYTORCH_VERSION` to override the package version defined by `pytorch-native-auto`.

The following table illustrates which pytorch version that DJL supports:

| PyTorch engine version | PyTorch native library version             |
|------------------------|--------------------------------------------|
| pytorch-engine:0.17.0  | 1.9.1, 1.10.0, 1.11.0                      |
| pytorch-engine:0.16.0  | 1.8.1, 1.9.1, 1.10.0                       |
| pytorch-engine:0.15.0  | pytorch-native-auto: 1.8.1, 1.9.1, 1.10.0  |
| pytorch-engine:0.14.0  | pytorch-native-auto: 1.8.1, 1.9.0, 1.9.1   |
| pytorch-engine:0.13.0  | pytorch-native-auto:1.9.0                  |
| pytorch-engine:0.12.0  | pytorch-native-auto:1.8.1                  |
| pytorch-engine:0.11.0  | pytorch-native-auto:1.8.1                  |
| pytorch-engine:0.10.0  | pytorch-native-auto:1.7.1                  |
| pytorch-engine:0.9.0   | pytorch-native-auto:1.7.0                  |
| pytorch-engine:0.8.0   | pytorch-native-auto:1.6.0                  |
| pytorch-engine:0.7.0   | pytorch-native-auto:1.6.0                  |
| pytorch-engine:0.6.0   | pytorch-native-auto:1.5.0                  |
| pytorch-engine:0.5.0   | pytorch-native-auto:1.4.0                  |
| pytorch-engine:0.4.0   | pytorch-native-auto:1.4.0                  |

We strongly recommend you to use [Bill of Materials (BOM)](../../../bom/README.md) to manage your dependencies.

By default, DJL will download the PyTorch native libraries into [cache folder](../../../docs/development/cache_management.md) the first time you run DJL.
It will automatically determine the appropriate jars for your system based on the platform and GPU support.

You can choose a native library based on your platform if you don't have network access at runtime.

### macOS
For macOS, you can use the following library:

- ai.djl.pytorch:pytorch-jni:1.11.0-0.17.0
- ai.djl.pytorch:pytorch-native-cpu:1.11.0:osx-x86_64

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cpu</artifactId>
    <classifier>osx-x86_64</classifier>
    <version>1.11.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

### Linux
For the Linux platform, you can choose between CPU, GPU. If you have NVIDIA [CUDA](https://en.wikipedia.org/wiki/CUDA)
installed on your GPU machine, you can use one of the following library:

#### Linux GPU

- ai.djl.pytorch:pytorch-jni:1.11.0-0.17.0
- ai.djl.pytorch:pytorch-native-cu113:1.11.0:linux-x86_64 - CUDA 11.3
- ai.djl.pytorch:pytorch-native-cu102:1.11.0:linux-x86_64 - CUDA 10.2

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cu113</artifactId>
    <classifier>linux-x86_64</classifier>
    <version>1.11.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cu102</artifactId>
    <classifier>linux-x86_64</classifier>
    <version>1.11.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

### Linux CPU

- ai.djl.pytorch:pytorch-jni:1.11.0-0.17.0
- ai.djl.pytorch:pytorch-native-cpu:1.11.0:linux-x86_64

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cpu</artifactId>
    <classifier>linux-x86_64</classifier>
    <scope>runtime</scope>
    <version>1.11.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

### For aarch64 build

- ai.djl.pytorch:pytorch-jni:1.11.0-0.17.0
- ai.djl.pytorch:pytorch-native-cpu-precxx11:1.11.0:linux-aarch64

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cpu-precxx11</artifactId>
    <classifier>linux-aarch64</classifier>
    <scope>runtime</scope>
    <version>1.11.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

### For Pre-CXX11 build

We also provide packages for the system like CentOS 7/Ubuntu 14.04 with GLIBC >= 2.17.
All the package were built with GCC 7, we provided a newer `libstdc++.so.6.24` in the package that contains `CXXABI_1.3.9` to use the package successfully.

- ai.djl.pytorch:pytorch-jni:1.11.0-0.17.0
- ai.djl.pytorch:pytorch-native-cu113-precxx11:1.11.0:linux-x86_64 - CUDA 11.3
- ai.djl.pytorch:pytorch-native-cpu-precxx11:1.11.0:linux-x86_64   - CPU

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cu113-precxx11</artifactId>
    <classifier>linux-x86_64</classifier>
    <version>1.11.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cpu-precxx11</artifactId>
    <classifier>linux-x86_64</classifier>
    <version>1.11.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

### Windows

PyTorch requires Visual C++ Redistributable Packages. If you encounter an UnsatisfiedLinkError while using
DJL on Windows, please download and install
[Visual C++ 2019 Redistributable Packages](https://support.microsoft.com/en-us/help/2977003/the-latest-supported-visual-c-downloads) and reboot.

For the Windows platform, you can choose between CPU and GPU.

#### Windows GPU

- ai.djl.pytorch:pytorch-jni:1.11.0-0.17.0
- ai.djl.pytorch:pytorch-native-cu113:1.11.0:win-x86_64
- ai.djl.pytorch:pytorch-native-cu102:1.11.0:win-x86_64

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cu113</artifactId>
    <classifier>win-x86_64</classifier>
    <version>1.11.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cu102</artifactId>
    <classifier>win-x86_64</classifier>
    <version>1.11.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```

### Windows CPU

- ai.djl.pytorch:pytorch-jni:1.11.0-0.17.0
- ai.djl.pytorch:pytorch-native-cpu:1.11.0:win-x86_64

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-cpu</artifactId>
    <classifier>win-x86_64</classifier>
    <scope>runtime</scope>
    <version>1.11.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-jni</artifactId>
    <version>1.11.0-0.17.0</version>
    <scope>runtime</scope>
</dependency>
```
