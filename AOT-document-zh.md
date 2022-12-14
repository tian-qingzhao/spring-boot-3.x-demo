# 10. Ahead of Time Optimizations

本章将介绍Spring的提前(AOT)优化。

有关特定于集成测试的AOT支持，请参阅测试的提前支持。

### 10.1. 提前优化导论

Spring对AOT优化的支持意味着在构建时检查ApplicationContext，并应用通常在运行时发生的决策和发现逻辑。这样做可以构建一个更直接的应用程序启动安排，并且主要基于类路径和环境专注于一组固定的特性。

早期应用这样的优化意味着以下限制:

* 类路径是固定的，并且在构建时完全定义。

* 应用程序中定义的bean不能在运行时更改，这意味着:

@Profile，特别是特定于概要文件的配置需要在构建时选择。

影响bean存在的环境属性(@Conditional)只在构建时考虑。

当这些限制到位时，就有可能在构建时执行提前处理并生成额外的资产。Spring AOT处理的应用程序通常生成:

* Java源代码
* 字节码(通常用于动态代理)
* 用于使用反射、资源加载、序列化和JDK代理的RuntimeHints。

```目前，AOT的重点是允许使用GraalVM将Spring应用程序部署为本机映像。我们打算在未来支持更多基于jvm的用例```。

### 10.2. AOT引擎概述

AOT引擎用于处理ApplicationContext安排的入口点是ApplicationContextAotGenerator。它根据GenericApplicationContext(表示要优化的应用程序)
和GenerationContext负责以下步骤:

为AOT处理刷新ApplicationContext。与传统的刷新不同，这个版本只创建bean定义，而不创建bean实例。

调用可用的BeanFactoryInitializationAotProcessor实现，并对GenerationContext应用它们的贡献。例如，核心实现遍历所有候选bean定义，并生成恢复BeanFactory状态所需的代码。

一旦这个过程完成，GenerationContext将被更新为生成的代码、资源和应用程序运行所必需的类。RuntimeHints实例还可以用于生成相关的GraalVM本机映像配置文件。

ApplicationContextInitializer入口点的类名，该入口点允许使用AOT优化启动上下文。

### 10.3. 刷新AOT处理

所有GenericApplicationContext实现都支持AOT处理的刷新。应用程序上下文是用任意数量的入口点创建的，通常以@ configuration注释类的形式。

让我们来看一个基本的例子:

```java@Configuration(proxyBeanMethods=false)
@ComponentScan
@Import({DataSourceConfiguration.class, ContainerConfiguration.class})
public class MyApplication {
}
```

使用常规运行时启动此应用程序涉及许多步骤，包括类路径扫描、配置类解析、bean实例化和生命周期回调处理。用于AOT处理的刷新仅应用常规刷新所发生的事情的子集。触发AOT处理的情况如下:

```java
RuntimeHints hints=new RuntimeHints();
AnnotationConfigApplicationContext context=new AnnotationConfigApplicationContext();
context.register(MyApplication.class);
context.refreshForAotProcessing(hints);
```

在这种模式下，像往常一样调用BeanFactoryPostProcessor实现。这包括配置类解析、导入选择器、类路径扫描等。这些步骤确保BeanRegistry包含应用程序的相关bean定义。如果bean定义由条件(如@Profile)
保护，则在此阶段将丢弃这些条件。

因为这种模式并不实际创建bean实例，所以除了与AOT处理相关的特定变量外，不会调用BeanPostProcessor实现。这些都是:

* MergedBeanDefinitionPostProcessor实现了后处理bean定义，以提取额外的设置，例如init和destroy方法。
* SmartInstantiationAwareBeanPostProcessor实现在必要时确定更精确的bean类型。这样可以确保在运行时创建所需的任何代理。

这一部分完成后，BeanFactory包含了应用程序运行所必需的bean定义。它不会触发bean实例化，但允许AOT引擎检查将在运行时创建的bean。

### 10.4. Bean工厂初始化AOT贡献

希望参与此步骤的组件可以实现BeanFactoryInitializationAotProcessor接口。每个实现都可以根据bean工厂的状态返回一个AOT贡献。

AOT贡献是一个组件，它贡献生成的代码来再现特定的行为。它还可以提供RuntimeHints来指示反射、资源加载、序列化或JDK代理的需要。

BeanFactoryInitializationAotProcessor实现可以在META-INF/spring/aot中注册。键值等于接口的完全限定名的工厂。

BeanFactoryInitializationAotProcessor也可以由bean直接实现。在这种模式下，bean提供的AOT贡献等价于它在常规运行时提供的特性。因此，这样的bean将自动从aot优化的上下文中排除。

如果一个bean实现了BeanFactoryInitializationAotProcessor接口，那么该bean及其所有依赖项将在AOT处理期间被初始化。
我们通常建议该接口仅由BeanFactoryPostProcessor等基础设施bean实现，这些bean的依赖关系有限，并且已经在bean工厂生命周期的早期进行了初始化。
如果这样的bean是使用@Bean工厂方法注册的，请确保该方法是静态的，以便不必初始化其外围的@Configuration类。

#### 10.4.1. Bean注册AOT贡献

一个核心BeanFactoryInitializationAotProcessor实现负责为每个候选BeanDefinition收集必要的贡献。它使用专用的BeanRegistrationAotProcessor来实现这一点。

接口功能:

* 由BeanPostProcessor bean实现，以替换其运行时行为。例如，AutowiredAnnotationBeanPostProcessor实现了这个接口来生成注入带有@Autowired注释的成员的代码。

* 由在META-INF/spring/aot中注册的类型实现。键值等于接口的完全限定名的工厂。通常用于bean定义需要针对核心框架的特定特性进行调优时。

如果一个bean实现了BeanRegistrationAotProcessor接口，那么该bean及其所有依赖项将在AOT处理期间初始化。
我们通常建议该接口仅由BeanFactoryPostProcessor等基础设施bean实现，这些bean的依赖关系有限，并且已经在bean工厂生命周期的早期进行了初始化。
如果这样的bean是使用@Bean工厂方法注册的，请确保该方法是静态的，以便不必初始化其外围的@Configuration类。

如果没有BeanRegistrationAotProcessor处理特定的已注册bean，则由默认实现处理它。这是默认的行为，因为为bean定义优化生成的代码应该限制在极端情况下。

以我们前面的例子为例，让我们假设datasourcecconfiguration如下所示:

```java

@Configuration(proxyBeanMethods = false)
public class DataSourceConfiguration {
    
    @Bean
    public SimpleDataSource dataSource() {
        return new SimpleDataSource();
    }
}
```

由于这个类上没有任何特殊条件，dataSourceConfiguration和dataSource被标识为候选类。AOT引擎将上面的配置类转换为类似于下面的代码:

```java
/**
 * Bean definitions for {@link DataSourceConfiguration}
 */
public class DataSourceConfiguration__BeanDefinitions {
    
    /**
     * Get the bean definition for 'dataSourceConfiguration'
     */
    public static BeanDefinition getDataSourceConfigurationBeanDefinition() {
        Class<?> beanType = DataSourceConfiguration.class;
        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
        beanDefinition.setInstanceSupplier(DataSourceConfiguration::new);
        return beanDefinition;
    }
    
    /**
     * Get the bean instance supplier for 'dataSource'.
     */
    private static BeanInstanceSupplier<SimpleDataSource> getDataSourceInstanceSupplier() {
        return BeanInstanceSupplier.<SimpleDataSource>forFactoryMethod(DataSourceConfiguration.class, "dataSource")
                .withGenerator(
                        (registeredBean) -> registeredBean.getBeanFactory().getBean(DataSourceConfiguration.class)
                                .dataSource());
    }
    
    /**
     * Get the bean definition for 'dataSource'
     */
    public static BeanDefinition getDataSourceBeanDefinition() {
        Class<?> beanType = SimpleDataSource.class;
        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
        beanDefinition.setInstanceSupplier(getDataSourceInstanceSupplier());
        return beanDefinition;
    }
}
```

生成的确切代码可能不同，这取决于bean定义的确切性质。

### 10.5. Runtime Hints

与常规JVM运行时相比，将应用程序作为本机映像运行需要额外的信息。例如，GraalVM需要提前知道组件是否使用反射。类似地，除非显式指定，否则不会在本机映像中提供类路径资源。因此，如果应用程序需要加载资源，则必须从相应的GraalVM本机映像配置文件引用该资源。

RuntimeHints API收集运行时对反射、资源加载、序列化和JDK代理的需求。下面的例子确保config/app. conf属性可以在原生映像的运行时从类路径加载:

```java
runtimeHints.resources().registerPattern("config/app.properties");
```

在AOT处理期间，会自动处理许多合同。例如，会检查@Controller方法的返回类型，如果Spring检测到该类型应该被序列化(通常是JSON)，就会添加相关的反射提示。
对于核心容器无法推断的情况，可以通过编程方式注册这些提示。还为常见用例提供了许多方便的注释。

#### 10.5.1. @ImportRuntimeHints

RuntimeHintsRegistrar实现允许您获得AOT引擎管理的RuntimeHints实例的回调。这个接口的实现可以在任何Spring
bean或@Bean工厂方法上使用@ImportRuntimeHints注册。RuntimeHintsRegistrar实现在构建时被检测和调用。

```java

@Component
@ImportRuntimeHints(SpellCheckService.SpellCheckServiceRuntimeHints.class)
public class SpellCheckService {
    
    public void loadDictionary(Locale locale) {
        ClassPathResource resource = new ClassPathResource("dicts/" + locale.getLanguage() + ".txt");
        //...
    }
    
    static class SpellCheckServiceRuntimeHints implements RuntimeHintsRegistrar {
        
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("dicts/*");
        }
    }
}
```

如果可能的话，@ImportRuntimeHints应该尽可能靠近需要提示的组件使用。这样，如果组件没有被贡献给BeanFactory，提示也不会被贡献。
也可以通过在META-INF/spring/aot中添加一个条目来静态注册一个实现。工厂的键值等于RuntimeHintsRegistrar接口的完全限定名。

#### 10.5.2. @Reflective

@Reflective提供了一种惯用的方式来标记在带注释的元素上需要反射。例如，@EventListener使用@Reflective进行元注释，因为底层实现使用反射调用带注释的方法。

默认情况下，只考虑Spring bean，并为带注释的元素注册调用提示。这可以通过@Reflective注释指定一个自定义的ReflectiveProcessor实现来调优。

标准库作者可以出于自己的目的重用此注释。如果需要处理Spring
bean以外的组件，BeanFactoryInitializationAotProcessor可以检测相关类型并使用ReflectiveRuntimeHintsRegistrar来处理它们。

#### 10.5.3. @RegisterReflectionForBinding

@RegisterReflectionForBinding是@Reflective的专门化，用于注册任意类型的序列化需求。一个典型的用例是使用容器无法推断的dto，例如在方法体中使用web客户端。

@RegisterReflectionForBinding可以应用于类级别的任何Spring bean，但它也可以直接应用于方法、字段或构造函数，以便更好地指出实际需要提示的位置。下面的示例为序列化注册Account。

```java

@Component
public class OrderService {
    
    @RegisterReflectionForBinding(Account.class)
    public void process(Order order) {
        // ...
    }

}
```

#### 10.5.4. Testing Runtime Hints

Spring Core 还提供了runtimehintpredicates，这是一个用于检查现有提示是否与特定用例匹配的实用程序。
这可以在您自己的测试中使用，以验证RuntimeHintsRegistrar是否包含预期的结果。我们可以为SpellCheckService写一个测试，确保我们能够在运行时加载一个字典:

```java
@Test
void shouldRegisterResourceHints(){
    RuntimeHints hints=new RuntimeHints();
    new SpellCheckServiceRuntimeHints().registerHints(hints,getClass().getClassLoader());
    assertThat(RuntimeHintsPredicates.resource().forResource("dicts/en.txt"))
    .accepts(hints);
}
```

使用runtimehintpredicates，我们可以检查反射、资源、序列化或代理生成提示。这种方法适用于单元测试，但意味着组件的运行时行为是众所周知的。

通过使用GraalVM跟踪代理运行应用程序的测试套件(或应用程序本身)，您可以更多地了解应用程序的全局运行时行为。该代理将在运行时记录所有需要GraalVM提示的相关调用，并将它们写入JSON配置文件。

为了更有针对性的发现和测试，Spring Framework提供了一个带有核心AOT测试工具的专用模块，“org.springframework: Spring -core-test”。这个模块包含RuntimeHints
Agent，这是一个Java代理，它记录所有与运行时提示相关的方法调用，并帮助您断言给定的RuntimeHints实例涵盖了所有记录的调用。让我们考虑一个基础设施，我们希望测试在AOT处理阶段提供的提示。

```java
public class SampleReflection {
    
    private final Log logger = LogFactory.getLog(SampleReflection.class);
    
    public void performReflection() {
        try {
            Class<?> springVersion = ClassUtils.forName("org.springframework.core.SpringVersion", null);
            Method getVersion = ClassUtils.getMethod(springVersion, "getVersion");
            String version = (String) getVersion.invoke(null);
            logger.info("Spring version:" + version);
        } catch (Exception exc) {
            logger.error("reflection failed", exc);
        }
    }

}
```

然后我们可以编写一个单元测试(不需要本机编译)来检查我们提供的提示:

```java
// @EnabledIfRuntimeHintsAgent signals that the annotated test class or test
// method is only enabled if the RuntimeHintsAgent is loaded on the current JVM.
// It also tags tests with the "RuntimeHints" JUnit tag.
@EnabledIfRuntimeHintsAgent
class SampleReflectionRuntimeHintsTests {
    
    @Test
    void shouldRegisterReflectionHints() {
        RuntimeHints runtimeHints = new RuntimeHints();
        // Call a RuntimeHintsRegistrar that contributes hints like:
        runtimeHints.reflection().registerType(SpringVersion.class, typeHint -> {
            typeHint.withMethod("getVersion", List.of(), ExecutableMode.INVOKE);
        });
        
        // Invoke the relevant piece of code we want to test within a recording lambda
        RuntimeHintsInvocations invocations = RuntimeHintsRecorder.record(() -> {
            SampleReflection sample = new SampleReflection();
            sample.performReflection();
        });
        // assert that the recorded invocations are covered by the contributed hints
        assertThat(invocations).match(runtimeHints);
    }

}
```

如果你忘记提供一个提示，测试将失败，并提供一些关于调用的详细信息:

```java
org.springframework.docs.core.aot.hints.testing.SampleReflection performReflection INFO:Spring version:6.0.0-SNAPSHOT
        
        Missing<"ReflectionHints">for invocation<java.lang.Class#forName>
        with arguments["org.springframework.core.SpringVersion",
        false,
        jdk.internal.loader.ClassLoaders$AppClassLoader@251a69d7].
        Stacktrace:
<"org.springframework.util.ClassUtils#forName, Line 284 io.spring.runtimehintstesting.SampleReflection#performReflection,Line 19 io.spring.runtimehintstesting.SampleReflectionRuntimeHintsTests#lambda$shouldRegisterReflectionHints$0,Line 25
```

在构建中有多种配置Java代理的方法，因此请参考构建工具和测试执行插件的文档。代理本身可以配置为检测特定的包(默认情况下，只有org。Springframework是工具化的)。您可以在Spring Framework buildSrc
README文件中找到更多细节。


