import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * A recommender system based on user similarity and bias correction.
 *
 * @param <T> The item type, extending the Item interface.
 */
class SimilarityBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    private final double globalBias;
    private final Map<Integer, Double> itemBiases;
    private final Map<Integer, Double> userBiases;

    /**
     * Constructs the recommender system by computing biases.
     */
    public SimilarityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
        globalBias = computeGlobalBias();
        itemBiases = computeItemBiases();
        userBiases = computeUserBiases();
    }

    // =====================
    // === Bias Computation ===
    // =====================

    /**
     * Computes the global average rating.
     */
    private double computeGlobalBias() {
        return ratings.stream()
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Computes the item-specific biases (rating - globalBias).
     */
    private Map<Integer, Double> computeItemBiases() {
        return ratings.stream()
                .collect(groupingBy(Rating::getItemId,
                        collectingAndThen(
                                mapping(r -> r.getRating() - globalBias, toList()),
                                list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
                        )));
    }

    /**
     * Computes user-specific biases (rating - globalBias - itemBias).
     */
    private Map<Integer, Double> computeUserBiases() {
        return ratings.stream()
                .collect(groupingBy(Rating::getUserId,
                        collectingAndThen(
                                mapping(r -> {
                                    double itemBias = itemBiases.getOrDefault(r.getItemId(), 0.0);
                                    return r.getRating() - globalBias - itemBias;
                                }, toList()),
                                list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
                        )));
    }

    // ================================
    // === Similarity and Prediction ===
    // ================================

    /**
     * Computes similarity between two users using dot-product of bias-free ratings.
     *
     * @return similarity score or 0 if fewer than 10 shared items.
     */
    public double getSimilarity(int u1, int u2) {
        Map<Integer, Double> u1Ratings = getBiasFreeRatings(u1);
        Map<Integer, Double> u2Ratings = getBiasFreeRatings(u2);

        List<Integer> sharedItems = u1Ratings.keySet().stream()
                .filter(u2Ratings::containsKey)
                .collect(Collectors.toList());

        if (sharedItems.size() < 10) return 0;

        return sharedItems.stream()
                .mapToDouble(itemId -> u1Ratings.get(itemId) * u2Ratings.get(itemId))
                .sum();
    }

    /**
     * Gets the top 10 similar users to the specified user.
     */
    public List<Integer> getTop10SimilarUsers(int userId) {
        return users.keySet().stream()
                .filter(other -> other != userId)
                .map(other -> Map.entry(other, getSimilarity(userId, other)))
                .filter(entry -> entry.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Predicts ratings for items not yet rated by the specified user.
     */
    public Map<Integer, Double> predictRatings(int userId) {
        List<Integer> similarUsers = getTop10SimilarUsers(userId);
        Set<Integer> ratedItems = getUserRatedItems(userId);

        Map<Integer, Double> predictions = new HashMap<>();
//TODO: CHANGE TO STREAMS
        for (Integer itemId : ratingsByItem.keySet()) {
            if (ratedItems.contains(itemId)) continue;

            List<Rating<T>> relevantRatings = ratingsByItem.get(itemId).stream()
                    .filter(r -> similarUsers.contains(r.getUserId()))
                    .collect(Collectors.toList());

            if (relevantRatings.size() < 5) continue;

            double predicted = predictRating(userId, itemId, relevantRatings);
            if (!Double.isNaN(predicted)) {
                predictions.put(itemId, predicted);
            }
        }

        return predictions;
    }

    /**
     * Recommends the top 10 items for the user.
     */
    @Override
    public List<T> recommendTop10(int userId) {
        Map<Integer, Double> predictedRatings = predictRatings(userId);
        Map<Integer, Long> ratingCounts = ratings.stream()
                .collect(groupingBy(Rating::getItemId, counting()));

        return predictedRatings.entrySet().stream()
                .sorted((e1, e2) -> {
                    int cmp = Double.compare(e2.getValue(), e1.getValue());
                    if (cmp != 0) return cmp;

                    cmp = Long.compare(
                            ratingCounts.getOrDefault(e2.getKey(), 0L),
                            ratingCounts.getOrDefault(e1.getKey(), 0L));
                    if (cmp != 0) return cmp;

                    return items.get(e1.getKey()).getName()
                            .compareTo(items.get(e2.getKey()).getName());
                })
                .limit(10)
                .map(entry -> items.get(entry.getKey()))
                .collect(Collectors.toList());
    }

    // ========================
    // === Helper Functions ===
    // ========================

    private Map<Integer, Double> getBiasFreeRatings(int userId) {
        return ratingsByUser.getOrDefault(userId, Set.of()).stream()
                .collect(toMap(
                        Rating::getItemId,
                        r -> r.getRating() - globalBias
                                - itemBiases.getOrDefault(r.getItemId(), 0.0)
                                - userBiases.getOrDefault(userId, 0.0)
                ));
    }

    private Set<Integer> getUserRatedItems(int userId) {
        return ratingsByUser.getOrDefault(userId, Set.of()).stream()
                .map(Rating::getItemId)
                .collect(Collectors.toSet());
    }

    private double predictRating(int userId, int itemId, List<Rating<T>> relevantRatings) {
        double numerator = 0;
        double denominator = 0;

        for (Rating<T> r : relevantRatings) {
            int simUserId = r.getUserId();
            double similarity = getSimilarity(userId, simUserId);
            if (similarity == 0) continue;

            double biasFreeRating = r.getRating()
                    - globalBias
                    - itemBiases.getOrDefault(itemId, 0.0)
                    - userBiases.getOrDefault(simUserId, 0.0);

            numerator += similarity * biasFreeRating;
            denominator += similarity;
        }

        if (denominator == 0) return Double.NaN;

        return globalBias
                + userBiases.getOrDefault(userId, 0.0)
                + itemBiases.getOrDefault(itemId, 0.0)
                + numerator / denominator;
    }

    // ==============================
    // === Bias Debugging Output ===
    // ==============================

    public void printGlobalBias() {
        System.out.printf("Global bias: %.2f%n", globalBias);
    }

    public void printItemBias(int itemId) {
        System.out.printf("Item bias for item %d: %.2f%n", itemId, itemBiases.getOrDefault(itemId, 0.0));
    }

    public void printUserBias(int userId) {
        System.out.printf("User bias for user %d: %.2f%n", userId, userBiases.getOrDefault(userId, 0.0));
    }

    public double getGlobalBias() {
        return globalBias;
    }

    public double getItemBias(int itemId) {
        return itemBiases.getOrDefault(itemId, 0.0);
    }

    public double getUserBias(int userId) {
        return userBiases.getOrDefault(userId, 0.0);
    }
}
