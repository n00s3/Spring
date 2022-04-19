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