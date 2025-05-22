import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * A popularity-based recommender system.
 * <p>
 * Recommends the most popular items (rated by at least 100 users)
 * that the given user has not rated yet, sorted by average rating,
 * then by number of ratings, and finally by item name.
 *
 * @param <T> the type of items recommended, must extend Item
 */
class PopularityBasedRecommender<T extends Item> extends RecommenderSystem<T> {

    private static final int POPULARITY_THRESHOLD = 100;

    public PopularityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    /**
     * Recommend the top 10 popular items for the given user.
     * Items must not be already rated by the user.
     *
     * @param userId the ID of the user
     * @return list of top 10 recommended items
     */
    @Override
    public List<T> recommendTop10(int userId) {
        Set<Integer> itemsRatedByUser = getItemsRatedByUser(userId);

        return ratingsByItem.entrySet().stream()
                .filter(e -> e.getValue().size() > 100) // Keep only popular items
                .filter(e -> !itemsRatedByUser.contains(e.getKey())) // Keep only unrated items
                .sorted((e1, e2) -> {
                    double avg1 = e1.getValue().stream().mapToDouble(Rating::getRating).average().orElse(0.0);
                    int count1 = e1.getValue().size();

                    double avg2 = e2.getValue().stream().mapToDouble(Rating::getRating).average().orElse(0.0);
                    int count2 = e2.getValue().size();

                    int cmp = Double.compare(avg2, avg1);
                    if (cmp != 0) return cmp;

                    cmp = Integer.compare(count2, count1);
                    if (cmp != 0) return cmp;

                    return items.get(e1.getKey()).getName().compareTo(items.get(e2.getKey()).getName());
                })
                .limit(NUM_OF_RECOMMENDATIONS) // Take the top 10 items
                .map(e -> items.get(e.getKey())) // Convert Map.Entry to Item
                .collect(Collectors.toList());
    }

    /**
     * Get the average rating for a given item.
     *
     * @param itemId the ID of the item
     * @return average rating, or 0.0 if no ratings found
     */
    public double getItemAverageRating(int itemId) {
        return ratings.stream()
                .filter(r -> r.getItemId() == itemId)
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Get the number of ratings for a given item.
     *
     * @param itemId the ID of the item
     * @return number of ratings
     */
    public int getItemRatingsCount(int itemId) {
        return (int) ratings.stream()
                .filter(rating -> rating.getItemId() == itemId)
                .count();
    }

    /**
     * Returns a set of item IDs that the user has already rated.
     *
     * @param userId the ID of the user
     * @return set of rated item IDs
     */
    private Set<Integer> getItemsRatedByUser(int userId) {
        return ratingsByUser.getOrDefault(userId, Set.of()).stream()
                .map(Rating::getItemId)
                .collect(Collectors.toSet());
    }
}
