package hello.proxy.app.v1;

public class OrderControllerImplV1 implements OrderControllerV1 {
    private final OrderServiceV1 orderServiceV1;

    public OrderControllerImplV1(OrderServiceV1 orderServiceV1) {
        this.orderServiceV1 = orderServiceV1;
    }

    @Override
    public String request(String itemId) {
        orderServiceV1.orderItem(itemId);
        return "ok";
    }

    @Override
    public String noLog() {
        return "ok";
    }
}
