package com.proofcart.personalization;

import com.proofcart.domain.entity.PersonalizationEventEntity;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.repo.PersonalizationEventRepository;
import com.proofcart.domain.repo.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PersonalizationService {
    private final PersonalizationEventRepository events;
    private final ProductRepository products;

    public PersonalizationService(PersonalizationEventRepository events, ProductRepository products) {
        this.events = events;
        this.products = products;
    }

    public void record(UUID buyerId, String type, UUID productId, String searchTerm) {
        String eventType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SEARCH", "LIKE", "DISLIKE").contains(eventType)) {
            throw new IllegalArgumentException("eventType must be SEARCH, LIKE, or DISLIKE.");
        }
        if ("SEARCH".equals(eventType) && (searchTerm == null || searchTerm.isBlank())) {
            throw new IllegalArgumentException("A search event needs a searchTerm.");
        }
        if (!"SEARCH".equals(eventType) && productId == null) {
            throw new IllegalArgumentException("Product feedback needs a productId.");
        }
        if (productId != null && !products.existsById(productId)) {
            throw new IllegalArgumentException("The selected product no longer exists.");
        }

        PersonalizationEventEntity event = new PersonalizationEventEntity();
        event.setBuyerId(buyerId);
        event.setEventType(eventType);
        event.setProductId(productId);
        event.setSearchTerm(searchTerm == null ? null : searchTerm.trim().toLowerCase(Locale.ROOT));
        events.save(event);
    }

    public List<Recommendation> recommend(UUID buyerId, UUID merchantId) {
        List<PersonalizationEventEntity> history = buyerId == null
                ? List.of()
                : events.findTop200ByBuyerIdOrderByCreatedAtDesc(buyerId);
        // History is newest first. A user's most recent opinion is their current one:
        // liking a previously disliked item (and the reverse) takes effect immediately.
        Map<UUID, String> latestFeedback = new HashMap<>();
        history.stream()
                .filter(e -> e.getProductId() != null)
                .filter(e -> "LIKE".equals(e.getEventType()) || "DISLIKE".equals(e.getEventType()))
                .forEach(e -> latestFeedback.putIfAbsent(e.getProductId(), e.getEventType()));
        Set<UUID> disliked = latestFeedback.entrySet().stream()
                .filter(entry -> "DISLIKE".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<UUID> liked = latestFeedback.entrySet().stream()
                .filter(entry -> "LIKE".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Map<String, Long> terms = history.stream()
                .filter(e -> "SEARCH".equals(e.getEventType()) && e.getSearchTerm() != null)
                .flatMap(e -> Arrays.stream(e.getSearchTerm().split("[^a-z0-9]+")))
                .filter(word -> word.length() >= 3)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return products.findByMerchantId(merchantId).stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() - p.getReservedQuantity() > 0)
                .filter(p -> !disliked.contains(p.getId()))
                .map(p -> score(p, liked, terms))
                .sorted(Comparator.comparingInt(Recommendation::score).reversed()
                        .thenComparing(r -> r.product().getName()))
                .limit(8)
                .toList();
    }

    private Recommendation score(ProductEntity product, Set<UUID> liked, Map<String, Long> terms) {
        int score = liked.contains(product.getId()) ? 10 : 0;
        List<String> matches = new ArrayList<>();
        if (liked.contains(product.getId())) matches.add("you liked this before");
        String text = (product.getName() + " " + Optional.ofNullable(product.getDescription()).orElse("")).toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Long> term : terms.entrySet()) {
            if (text.contains(term.getKey())) {
                score += Math.toIntExact(term.getValue() * 3);
                matches.add("you often search for " + term.getKey());
            }
        }
        if (matches.isEmpty()) matches.add("popular in your store");
        return new Recommendation(product, score, matches.get(0));
    }

    public record Recommendation(ProductEntity product, int score, String reason) { }
}
