package jpabook.jpashop.repository.order.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * 기본적으로 Repository는 엔티티를 반환하게 됨
 * 하지만 [v4]의 방식은 Dto를 반환하는데
 * 이런 류의 쿼리들은 Repository와 분리해
 * 따로 패키지와 인터페이스를 만들어 사용하는 것을 권장
 * 그래야 유지보수 측면에서 도움이 됨
 */
@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {
    private final EntityManager em;

    public List<SimpleOrderDto> findOrderDtos() {
        return em.createQuery("select new jpabook.jpashop.repository.order.query.SimpleOrderDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d",SimpleOrderDto.class)
                .getResultList();
    }
}
