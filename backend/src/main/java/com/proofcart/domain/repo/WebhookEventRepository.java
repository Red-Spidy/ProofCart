package com.proofcart.domain.repo;

import com.proofcart.domain.entity.WebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, String> {
}
