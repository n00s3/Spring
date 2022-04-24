# Spring Framework AOP

## 프록시의 주요 기능
* 접근 제어 (프록시 패턴)
    * 권한에 따른 접근 차단
    * 캐싱
    * 지연 로딩
* 부가 기능 추가 (데코레이터 패턴)
    * 예) 요청 값이나, 응답 값을 중간에 변형
    * 예) 실행 시간을 측정해서 추가 로그를 남김
    
## 동적 프록시
### 리플렉션
클래스나 메서드의 메타정보를 동적으로 획득하고, 코드도 동적으로 호출할 수 있다.
* 리플렉션 예제
```java
    static class Hello {
        public String callA() {
            log.info("callA");
            return "A";
        }

        public String callB() {
            log.info("callB");
            return "B";
        }
    }

    @Test
    void reflection() throws Exception {
        // 클래스 정보
        Class classHello = Class.forName("hello.proxy.jdkdynamic.ReflectionTest$Hello");

        Hello target = new Hello();
        // callA 메서드 정보
        Method methodCallA = classHello.getMethod("callA");
        Object result1 = methodCallA.invoke(target);
        log.info("result1={}", result1);

        // callB 메서드 정보
        Method methodCallB = classHello.getMethod("callB");
        Object result2 = methodCallB.invoke(target);
        log.info("result2={}", result2);
    }

    /*
    INFO hello.proxy.jdkdynamic.ReflectionTest$Hello - callA
    INFO hello.proxy.jdkdynamic.ReflectionTest - result1=A
    INFO hello.proxy.jdkdynamic.ReflectionTest$Hello - callB
    INFO hello.proxy.jdkdynamic.ReflectionTest - result2=B
    */
```

* 리플렉션을 통해 공통화
```java
    @Test
    void reflection() throws Exception {
        // 클래스 정보
        Class classHello = Class.forName("hello.proxy.jdkdynamic.ReflectionTest$Hello");

        Hello target = new Hello();
        // callA 메서드 정보
        Method methodCallA = classHello.getMethod("callA");
        dynamicCall(methodCallA, target);

        // callB 메서드 정보
        Method methodCallB = classHello.getMethod("callB");
        dynamicCall(methodCallA, target);
    }

    private void dynamicCall(Method method, Object target) throws InvocationTargetException, IllegalAccessException {
        log.info("start");
        Object result = method.invoke(target);
        log.info("result={}", result);
    }
```
> 리플렉션은 컴파일 시점에 오류를 잡지못하고 코드를 직접 실행하는 시점에 발생하는 런타임 오류가 발생하기 때문에 주의해야한다.

### JDK dynamic proxy
`InvocationHandler` 인터페이스를 구현해서 프록시에 적용할 공통 로직을 개발할 수 있다.
* 시간 측정을 하는 예제
```java
@Slf4j
public class TimeInvocationHandler implements InvocationHandler {
    private final Object target;

    public TimeInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("TimeProxy 실행");
        long startTime = System.currentTimeMillis();

        Object result = method.invoke(target, args);

        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("TimeProxy 종료 resultTime={}", resultTime);

        return result;
    }
}
```
* `Object target`: 동적 프록시가 호출할 대상
* `args`: 메서드 호출시 넘겨줄 인수 

```java
    @Test
    void dynamicA() {
        AInterface target = new AImpl();
        TimeInvocationHandler handler = new TimeInvocationHandler(target);

        AInterface proxy = (AInterface) Proxy.newProxyInstance(AInterface.class.getClassLoader(), new Class[]{AInterface.class}, handler);

        proxy.call();
    }
```

### CGLIB (Code Generator Library)
* 바이트코드를 조작해서 동적으로 클래스를 생성하는 기술을 제공
* 인터페이스가 없어도 구체 클래스만 가지고 동적 프록시를 생성
스프링의 `ProxyFactory`가 편리하게 사용하게 도와주고 있다.

## 스프링 프록시 팩토리
인터페이스가 있으면 `JDK Dynamic Proxy`를 사용하고 구체 클래스가 있으면 `cglib`을 사용한다.

즉, 프록시 팩토리 하나로 편리하게 동적 프록시를 생성할 수 있다.

> Advice 도입
* 프록시가 호출하는 부가 기능.


* JDK dynamic proxy의 InvocationHandler
* cglib의 MethodInterceptor

위 두개 모두 Advice를 호출한다.
> Pointcut
* 어디에 부가 기능을 적용할지, 어디에 부가 기능을 적용하지 않을지 판단하는 필터링 로직.
특정 조건에 맞을 때 프록시 로직을 추가하는 경우에 사용된다.

    * `ClassFilter`: 클래스를 기준으로 필터링 
    * `MethodFilter`: 메서드를 기준으로 필터링

> Advisor
* `Pointcut` + `Advice`


### 사용 예제
Advice를 생성하고 ProxyFactory에 target과 생성한 Advice를 넣어주면 끝이다.
```java
// target 대상
@Slf4j
public class ServiceImpl implements ServiceInterface {

    @Override
    public void save() {
        log.info("save 호출");
    }

    @Override
    public void find() {
        log.info("find 호출");
    }
}


// Advice 생성
@Slf4j
public class TimeAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 공통 또는 중복 로직 시작
        log.info("TimeProxy 실행");
        long startTime = System.currentTimeMillis();
        
        // 비즈니스 로직 실행 부분
        Object result = invocation.proceed();

        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("TimeProxy 종료 resultTime={}", resultTime);

        // 공통 또는 중복 로직 종료
        return result;
    }
}
```
```java
// ProxyFactory 생성
@Test
void interfaceProxy() {
    ServiceInterface target = new ServiceImpl();
    ProxyFactory proxyFactory = new ProxyFactory(target);
    proxyFactory.addAdvice(new TimeAdvice());
    // 메서드 내부에서 Advisor가 생성된다.
    // DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(Pointcut.TRUE, new TimeAdvice());
    // proxyFactory.addAdvisor(advisor);

    ServiceInterface proxy = (ServiceInterface) proxyFactory.getProxy();

    log.info("targetClass={}", target.getClass());
    log.info("proxyClass={}", proxy.getClass());

    proxy.find();
    proxy.save();

    assertThat(AopUtils.isAopProxy(proxy)).isTrue();
    assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue(); // 인터페이스가 있는 타겟이기 때문에 JdkDynamicProxy로 생성되었음을 확인 
    assertThat(AopUtils.isCglibProxy(proxy)).isFalse();
}

/* 실행결과
INFO hello.proxy.proxyfactory.ProxyFactoryTest - targetClass=class hello.proxy.common.service.ServiceImpl
INFO hello.proxy.proxyfactory.ProxyFactoryTest - proxyClass=class com.sun.proxy.$Proxy13
INFO hello.proxy.common.advice.TimeAdvice - TimeProxy 실행
INFO hello.proxy.common.service.ServiceImpl - find 호출
INFO hello.proxy.common.advice.TimeAdvice - TimeProxy 종료 resultTime=0
INFO hello.proxy.common.advice.TimeAdvice - TimeProxy 실행
INFO hello.proxy.common.service.ServiceImpl - save 호출
INFO hello.proxy.common.advice.TimeAdvice - TimeProxy 종료 resultTime=0
*/
```

* Bean으로 등록하기 위한 동적 프록시 생성코드가 많아지는 단점
* 실제 객체 대신 프록시 객체를 Bean으로 등록 해야하는 단점

위 두가지 문제를 빈 후처리기가 해결한다.
### 빈 후처리기(Bean PostProcessor)
빈 저장소에 등록하기 직전에 조작이 가능하다.

* 객체 조작
* 다른 객체로 바꿔치기가 가능

```java
// 방법
// BeanPostProcessor의 postProcessAfterInitialization를 Override 해야한다.
public class PackageLogTracePostProcessor implements BeanPostProcessor {

    private final String basePackage;
    private final Advisor advisor;

    public PackageLogTracePostProcessor(String basePackage, Advisor advisor) {
        this.basePackage = basePackage;
        this.advisor = advisor;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        log.info("param beanName={} bean={}", beanName, bean.getClass());

        // 프록시 적용 대상 여부 체크
        // 프록시 적용 대상이 아니면 원본을 그대로 진행
        String packageName = bean.getClass().getPackageName();
        if (!packageName.startsWith(basePackage)) {
            return bean;
        }

        // 프록시 대상이면 프록시를 만들어서 반환
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.addAdvisor(advisor);

        Object proxy = proxyFactory.getProxy();
        log.info("create proxy: target={} proxy={}", bean.getClass(), proxy.getClass());
        return proxy;
    }
}
```

### AnnotationAwareAspectJAutoProxyCreator
> 라이브러리 추가
* Gradle인 경우 `implementation 'org.springframework.boot:spring-boot-starter-aop'`를 추가한다.

자동 프록시 생성기이다. 스프링 부트 환경에서는 라이브러리만 있으면 별다른 설정이 필요하지않다.
> 스프링 부트에서는 `@EnableAspectJAutoProxy` 설정이 따료 필요하지 않다.

`Advisor`만 `Bean`으로 등록해주면 알아서 프록시가 생성되고 `Bean`으로 등록된다.

* 자동 프록시 생성기는 `Pointcut`으로 적용 대상여부를 판단하고 하나라도 일치하면 프록시를 생성한다.
* 하나의 프록시에 여러 `Advisor(pointcut + advisor)`를 등록할 수 있다.

```java
    @Bean
    public Advisor advisor(LogTrace logTrace) {
        // pointcut
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* hello.proxy.app..*(..)) && !execution(* hello.proxy.app..noLog(..))");


        // advice
        LogTraceAdvice advice = new LogTraceAdvice(logTrace);
        return new DefaultPointcutAdvisor(pointcut, advice);
    }
```

## @Aspect 프록시
`AnnotationAwareAspectJAutoProxyCreator`는 `@Aspect` 어노테이션이 붙은 Bean을 Bean 컨테이너에서 찾아 `Advisor`로 만들어준다. 그리고 `Advisor`를 기반으로 프록시를 생성한다.
* 애플리케이션의 여러 기능들 사이에 걸쳐서 들어가는 관심사이다. **횡단 관심사 (cross-cutting concerns)** 라고 한다.

```java
@Aspect
public class LogTraceAspect {

    /*
        필요한 멤버 변수와 생성자
    */

    @Around("execution(* hello.proxy.app..*(..))") // Pointcut
    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
        // Advice 로직

        TraceStatus status = null;
        try {
            String message = joinPoint.getSignature().toShortString();
            status = logTrace.begin(message);

            // 로직 호출
            Object result = joinPoint.proceed();

            logTrace.end(status);
            return result;
        } catch (Exception e) {
            logTrace.exception(status, e);
            throw e;
        }
    }
}

// ---
@Configuration
public class AopConfig {
    @Bean
    public LogTraceAspect logTraceAspect(LogTrace logTrace) {
        return new LogTraceAspect(logTrace);
    }
}
```

## AOP
### AOP 적용방식
* 컴파일 시점 - 위빙(Weaving)
    - `.class`를 만드는 시점에 부가기능을 적용
    - `.class`를 디컴파일하면 Aspect 관련 코드가 들어간다.
    - 원본 로직에 부가 기능 로직이 추가되는 것을 `위빙`이라 한다.
    - AspectJ 직접 사용
* 클래스 로딩 시점 - 위빙
    - `.class` 파일을 조작한 다음 JVM에 올린다.
    - ByteCode에 직접 수정을 가해서, 소스 파일의 수정 없이 원하는 기능을 부여
    - 옵션 `java -javaagent`를 통해 클래스 로더 조작기를 지정한다.
    - `java instrumentation`
    - AspectJ 직접 사용
* 런타임 시점 (프록시)
    - 위 모든 예제는 런타임 시점에 적용되는 방법 사용
    - 자바의 `main` 메서드가 이미 실행된 다음 적용
    - 스프링 AOP에서 사용하는 방식

**스프링은 AspectJ 문법을 차용하고 프록시 방식의 AOP를 적용한다. AspecetJ를 직접 사용하는 것이 아니다.**
### AOP 용어정리
* 조인 포인트(Join point)
    * 어드바이스가 적용될 수 있는 위치
        * 메서드 실행, 생성자 호출, 필드 값 접근 등
    * AOP를 적용할 수 있는 모든 지점
    * 스프링 AOP는 프록시 방식을 사용하므로 조인 포인트는 항상 메서드 실행 지점으로 제한
* 포인트컷(Pointcut)
    * 어드바이스가 적용될 위치를 선별하는 기능
    * AspectJ 표현식을 사용해서 지정
* 타겟(Target)
    * 어드바이스를 받는 객체, 포인트컷으로 결정됨
* 어드바이스(Advice)
    * 특정 조인 포인트에서 Aspect에 의해 취해지는 조치
    * Around, Before, After와 같은 다양한 종류가 있음
* 애스펙트(Aspect)
    * 어드바이스 + 포인트컷을 모듈화
    * `@Aspect`
* 어드바이저
    * 하나의 어드바이스와 하나의 포인트컷으로 구성
* 위빙
    * 포인트컷으로 결정한 타겟의 조인 포인트에 어드바이스를 적용하는 것
* AOP 프록시
    * 스프링의 AOP는 JDK dynaimic proxy와 cglib을 사용

### 어드바이스 종류
* `@Around`: 메서드 호출 전후에 수행, 가장 강력한 어드바이스, 조인 포인트 실행 여부 선택, 반환 값 변환, 예외 변환 등이 가능
* `@Before`: 조인 포인트 실행(`joinPoint.proceed()`) 이전에 실행
* `@AfterReturning`: 조인 포인트가 정상 완료후 실행
* `@AfterThrowing`: 메서드가 예외를 던지는 경우 실행
* `@After`: 조인 포인트가 정상 또는 예외에 관계없이 실행(finally)

```java
    @Before("hello.aop.order.aop.Pointcuts.orderAndService()")
    public void doBefore(JoinPoint joinPoint) {
        log.info("[before] {}", joinPoint.getSignature());
    }

    @AfterReturning(value = "hello.aop.order.aop.Pointcuts.orderAndService()", returning = "result")
    public void doReturn(JoinPoint joinPoint, Object result) {
        log.info("[return] {} return={}", joinPoint.getSignature(), result);
    }

    @AfterThrowing(value = "hello.aop.order.aop.Pointcuts.orderAndService()", throwing = "ex")
    public void doThrowing(JoinPoint joinPoint, Exception ex) {
        log.info("[ex] message={}", ex);
    }

    @After(value = "hello.aop.order.aop.Pointcuts.orderAndService()")
    public void doAfter(JoinPoint joinPoint) {
        log.info("[after] {}", joinPoint.getSignature());
    }
```