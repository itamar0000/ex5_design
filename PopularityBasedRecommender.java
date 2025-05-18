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
        //get over all Ratings
        ratings.stream().collect(groupingBy(Rating::getItemId))
        // find all the ratings with itemId
        // get the sum of all ratings
        // get the count of all ratings
        // divide the sum by the count
        // TODO: implement
        return 0;
    }
    public int getItemRatingsCount(int itemId) {
        return (int)ratings.stream() //RETURN LONG IN DEFAULT
                .filter(rating -> rating.getItemId() == itemId)
                .count();
        // TODO: implement

    }

}
