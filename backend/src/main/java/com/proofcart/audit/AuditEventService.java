package com.proofcart.audit;

import com.proofcart.domain.entity.AuditEventEntity;
import com.proofcart.domain.repo.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditEventService {
    private final AuditEventRepository repo;

    public AuditEventService(AuditEventRepository repo) {
        this.repo = repo;
    }

    public void record(UUID buyer, UUID merchant, UUID cart, UUID order, String type, String description) {
        AuditEventEntity e = new AuditEventEntity();
        e.setBuyerId(buyer);
        e.setMerchantId(merchant);
        e.setCartId(cart);
        e.setOrderId(order);
        e.setEventType(type);
        e.setDescription(description);
        repo.save(e);
    }

    public List<AuditEventEntity> forOrder(UUID order) {
        return repo.findByOrderIdOrderByCreatedAtAsc(order);
    }
}
