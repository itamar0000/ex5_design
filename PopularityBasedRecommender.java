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
                .map(r -> r.getItemId())
                .collect(Collectors.toSet());

        List<T> popularUnratedItems = ratingsByItem.entrySet().stream()
                .filter(e -> e.getValue().size() > 100)
                .map(Map.Entry::getKey)
                .filter(itemId -> !itemsRatedByUser.contains(itemId))
                .map(items::get)
                .collect(Collectors.toList());

        Set<Integer> popularUnratedItemIds = popularUnratedItems.stream().map(item->item.getId()).collect(toSet());

        Map<Integer, Double> averageRatingsForPopularUnratedItems = ratings.stream()
                .filter(r -> popularUnratedItemIds.contains(r.getItem().getId())) // keep only relevant items
                .collect(Collectors.groupingBy(
                        r -> r.getItem().getId(),                      // group by item ID
                        Collectors.averagingDouble(Rating::getRating)  // average rating
                ));
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
        // TODO: implement

    }

}
