import java.util.*;

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
        // TODO: implement

        return null;
    }

    public double getItemAverageRating(int itemId) {

        return ratings.stream()
                .filter(r -> r.getItemId()==itemId)
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);

    }
    public int getItemRatingsCount(int itemId) {
        // TODO: implement
        return 0;
    }

}
