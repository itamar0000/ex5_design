import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/** Popularity‑based recommender implementation. */
class PopularityBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    private static final int POPULARITY_THRESHOLD = 100;
    public PopularityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    @Override
    public List<T> recommendTop10(int userId) {
        Set<Integer> itemsRatedByUser = ratingsByUser.getOrDefault(userId, Set.of()).stream()
                .map(Rating::getItemId)
                .collect(Collectors.toSet());

        return ratingsByItem.entrySet().stream()
                .filter(e -> e.getValue().size() > POPULARITY_THRESHOLD) // Keep only popular items
                .filter(e -> !itemsRatedByUser.contains(e.getKey()))    // Keep only unrated items
                .sorted((e1, e2) -> {
                    // Calculate average and count for first item (e1)
                    double avg1 = e1.getValue().stream().mapToDouble(Rating::getRating).average().orElse(0.0);
                    int count1 = e1.getValue().size();

                    // Calculate average and count for second item (e2)
                    double avg2 = e2.getValue().stream().mapToDouble(Rating::getRating).average().orElse(0.0);
                    int count2 = e2.getValue().size();

                    // Compare based on average rating (desc)
                    int cmp = Double.compare(avg2, avg1);
                    if (cmp != 0) return cmp;

                    // If averages are equal, compare based on rating count (desc)
                    cmp = Integer.compare(count2, count1);
                    if (cmp != 0) return cmp;

                    // If both average and count are equal, compare by item name
                    return items.get(e1.getKey()).getName().compareTo(items.get(e2.getKey()).getName());
                })
                .limit(NUM_OF_RECOMMENDATIONS) // Take the top 10 items
                .map(e -> items.get(e.getKey())) // Convert Map.Entry to Item
                .collect(Collectors.toList());
    }

    public double getItemAverageRating(int itemId) {

        return ratings.stream()
                .filter(r -> r.getItemId()==itemId)
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);


    }
    public int getItemRatingsCount(int itemId) {
        return (int)ratings.stream() //RETURN LONG IN DEFAULT
                .filter(rating -> rating.getItemId() == itemId)
                .count();

    }

}
