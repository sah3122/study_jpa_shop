package me.study.studyjpashop.repository;

import lombok.RequiredArgsConstructor;
import me.study.studyjpashop.domain.Order;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {
    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    //public List<Order> findAll(OrderSearch orderSearch) {}
}