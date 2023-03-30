package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
    private final OrderRepository orderRepo;
    private final OrderCollectionQueryRepository orderCollectionQueryRepo;
    private final OrderQueryService orderQueryService;

    /**
     * [v1]
     * 엔티티를 조회
     * 엔티티를 반환
     * 1+N 문제 발생
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepo.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();

            List<OrderItem> orderItems = order.getOrderItems();
            for (OrderItem orderItem : orderItems) {
                orderItem.getItem().getName();
            }
        }
        return all;
    }

    /**
     * [v2]
     * 엔티티를 조회
     * Dto를 반환
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepo.findAllByString(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    /**
     * [v3]
     * 엔티티를 조회
     * Dto를 반환
     * fetch join 사용
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepo.findAllwithItem();
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    /**
     * [v3.1]
     * 엔티티를 조회
     * Dto를 반환
     * @ToOne만 fetch join 사용
     * @ToMany는 지연로딩 + batch fetch size:100 설정
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_paging() {
        List<Order> orders = orderRepo.findAllWithMemberDelivery();
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    /**
     * [v4]
     * Dto를 조회
     * Dto를 반환
     * @ToOne만 fetch join 사용
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderCollectionQueryRepo.findOrderQueryDtos();
    }

    /**
     * [v5]
     * Dto를 조회
     * Dto를 반환
     * @ToOne만 fetch join 사용
     * 컬렉션 최적화
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderCollectionQueryRepo.findAllByDto();
    }

    /**
     * [v6]
     * Dto를 조회
     * Dto를 반환
     * 모든 테이블을 조인해서 조회하는
     * 한방 쿼리를 사용해
     * 4개의 행을 조회.
     * 그 중 중복으로 불필요한 2개의 행을
     * 다시 한번 정제하는 쿼리를 작성하여
     * 최종적으로 필요한 2개의 행만을 조회
     * 쿼리가 한 번만 나간다는 장점이 있지만
     * 추가 작업이 필수적이고
     * Order를 기준으로 페이징 불가능
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderQueryDto2> flats = orderCollectionQueryRepo.findAllByDto2();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                                o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(),
                                o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }

    @Data
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;
        /** 여기서 참조하는 엔티티도 Dto로 변환해서 사용 */

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }

        @Data
        static class OrderItemDto {
            private String itemName;
            private int orderPrice;
            private int count;

            public OrderItemDto(OrderItem orderItem) {
                itemName = orderItem.getItem().getName();
                orderPrice = orderItem.getOrderPrice();
                count = orderItem.getCount();
            }
        }
    }
}
