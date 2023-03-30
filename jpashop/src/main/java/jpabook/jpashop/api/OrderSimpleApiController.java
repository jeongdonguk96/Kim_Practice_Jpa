package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import jpabook.jpashop.repository.order.query.SimpleOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order
 * XToOne
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    private final OrderRepository orderRepo;
    private final OrderQueryRepository orderQueryRepo;

    /**
     * [v1]
     * 엔티티로 받고
     * 엔티티를 반환할 경우
     * 필요없는 데이터도 반환/노출
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepo.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
        }

        return all;
    }

    /**
     * [v2]
     * 엔티티로 받고
     * Dto를 반환할 경우
     * 필요한 데이터만 Dto로 생성해 핏하게 최적화
     * v1과 v2 모두 1+N 문제 발생
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        // 이 쿼리 1번으로 order 전체를 조회, 2개 리턴
        List<Order> orders = orderRepo.findAllByString(new OrderSearch());

        // 2개가 리턴되니까 반복문이 2번 돌아감
        // new SimpleOrderDto(o) 내부에서 member와 delivery 테이블을 조회
        // 반복문이 2번이니까 총 4번 조회
        // 맨 위의 1번, 여기서 4번 도합 5번이 돌음
        // 만약 맨 위에서 10개가 리턴됐다면?
        // 도합 21번이 됐을 것임
        // 이것이 1+N 문제(성능이슈!)
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * [v3]
     * 엔티티로 받고
     * Dto를 반환하고 fetch join 사용
     * fetch join을 사용함으로써
     * order와 member와 delivery 모두를
     * 한번에 긁어 오는
     * 단 1번의 쿼리만 사용
     * fetch join은 LAZY 전략에 우선
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepo.findAllWithMemberDelivery();

        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * [v4]
     * Dto로 받고
     * Dto를 반환함
     * orderRepo.findOrderDtos() 매서드의
     * 조회대상을 딱 필요한 정보로만 설정하여
     * 불필요환 조회를 하지 않음 -> 성능상 우위
     * 하지만, 최적화라는 말 그대로
     * 여기서만 사용가능한 코드가 되어버림
     * (Dto를 딱 여기 용도로만 구성했기 때문)
     */
    @GetMapping("/api/v4/simple-orders")
    public List<SimpleOrderDto> ordersV4() {
        return orderQueryRepo.findOrderDtos();
    }

    /**
     * [v3]과 [v4]를 비교해보면
     * from절에서 조인은 같으나
     * select절에서 조회되는 컬럼 수가 다름
     * 하지만 성능 이슈는
     * select절에서 생기는 게 아니라
     * from절에서 조인을 하면서 생김
     * 그렇기에 불필요한 컬럼을
     * 몇 개 더 조회한다고 해서
     * 성능상 큰 차이가 생기지 않음
     * 그러므로 코드작성의 난이도나 재사용성면으로
     * 봤을 때 [v3]의 방식을 채택하는 것이 좋음
     * (만약 컬럼의 갯수가 정말 많고
     * 트래픽이 정말 많은 조회 코드라면
     * [v4] 방식을 고려해봐야 함)
     */

    /**
     * 쿼리 방식 선택 권장 순서
     * 1. 엔티티로 받아 Dto를 반환하는 방식
     * 2. 필요 시 fetch join을 사용, 성능 이슈 해결
     * 3. Dto로 직접 받는 방식
     * 4. 최후의 방법으로 native SQl 사용
     */


}
