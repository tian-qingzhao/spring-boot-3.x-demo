### Spring Boot 2.7 新自动装配

SpringBoot2.7引入了新的自动装配方式 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` ， 原来的写法
spring.factories 在 3.0 版本以下还能兼容，3.0 新版本之后，老的写法 spring.factories 不能使用了。详见：{@link ImportCandidates}

### Spring Framework 6 两个重要特性：

#### 1.Http Interface

作为 Spring Boot 3.x 版本的新特性，Spring 框架支持将远程 HTTP 服务代理成带有特定注解的 Java http interface。 类似的库，如 OpenFeign 和 Retrofit 仍然可以使用，但
http interface 为 Spring 框架添加内置支持。 通过声明式 http 客户端实现我们就可以在 java 中像调用一个本地方法一样完成一次 http 请求，大大减少了编码成本，同时提高了代码可读性。 该方式依赖于
Spring5 WebFlux 实现的 WebClient，也就是 Reactive 响应式编程。

#### 2.AOT

Ahead-Of-Time，即预先编译，指在程序运行前编译，这样可以避免程序在运行时的编译性能消耗和内存消耗，可以加快程序的启动。 AOT的引入， 意味着 Spring 生态正式引入了提前编译技术，有助于优化 Spring
框架启动慢、内存占用多的问题。 比如 Spring 在启动时候需要扫描、解析、加载、定义、实例化、注入这几个步骤，使用 AOT 可以在启动前把扫描、解析、加载、定义给做了。 也就是会在指定目录生成 Bean Definition
。详见：GenerationContext、SpringApplicationAotProcessor。

#### 3.Spring Native

Spring Native 是Spring 团队和 GraalVM 团队合作的成果，有了 Spring Native 可以让应用不再依赖于Java虚拟机，可以将 Spring 应用通过
AOT（Ahead-of-Time，预先编译）技术编译为 Native Image（本地可执行程序，不是指容器镜像），从而获得快速启动、低内存消耗、即时峰值性能等特性， 这样的特性在云原生时代显得尤为重要，但相应代价是编译构建时间更长。

(1) 首先需要安装 GraalVM 和 native-image，下载地址 <a href="https://github.com/graalvm/graalvm-ce-builds/releases">
https://github.com/graalvm/graalvm-ce-builds/releases, 因为 Spring Boot 3 最低支持jdk17版本，所以 GraalVM
选择17或以上的版本下载，现在完解压并配置环境变量，把该环境变量放到第一个， 防止其他jdk版本优先级更高会覆盖。配置完通过 `java -version`
命令查看jdk版本，显示出 OpenJDK 64-Bit Server VM GraalVM CE 22.3.0 即可。 继续下载同版本的 native-image，例如这里我下载的是
native-image-installable-svm-java17-windows-amd64-22.3.0.jar，
然后在该jar包所在的目录下执行 `gu install -L native-image-installable-svm-java17-windows-amd64-22.3.0.jar` 命令， 再通过 gu list 查看安装情况，显示出
graalvm 和 native-image 即可。

(2) 在Window操作系统开发环境下基于GraalVM构建原生镜像依赖 Microsoft Visual C++ (MSVC) ，所以编译为二进制可执行文件.exe还需要安装C语言环境， 这里使用 VS Code 工具插件安装，下载
VS Code 地址 <a href="https://visualstudio.microsoft.com/zh-hans/downloads/">
https://visualstudio.microsoft.com/zh-hans/downloads.

(3) window系统在x64窗口(非cmd打开的窗口)里面使用 `mvn -Pnative clean native:compile` 命令编译，编译完之后直接执行项目的target目录下的.exe文件。
docker环境使用 `mvn -Pnative spring-boot:build-image` 命令构建镜像。同时在 pom.xml 的properties标签里面添加

```xml

<properties>
    <spring-boot.build-image.imageName>springboot3demo</spring-boot.build-image.imageName>
</properties>
```

(4) 解决native-image反射、代理、类序列化等问题。
`java -agentlib:native-image-agent=config-output-dir=d:/idea_workspace/spring-boot3.x-demo/src/main/resources/META-INF/native-image -jar d:/idea_workspace/spring-boot3.x-demo/target/spring-boot3.x-demo-1.0.0.jar`
，执行完该命令会在 `resources/META-INF/native-image` 文件夹下面生成 `reflect-config.json` 、 `proxy-config.json`
、 `serialization-config.json` 等文件。