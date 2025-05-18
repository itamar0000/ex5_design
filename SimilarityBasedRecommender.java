import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/** Similarity‑based recommender with bias correction. */
class SimilarityBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    // TODO: add data structures to hold the global/item/user biases
    private final double globalBias; //avg of all ratings in the entire system
    private final Map<Integer, Double> itemBiases; // avg of all ratings for each item
    private final Map<Integer, Double> userBiases; // avg of all ratings for each user
    public SimilarityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
        globalBias = ratings.stream()
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);
        itemBiases = ratings.stream()
                .collect(groupingBy(Rating::getItemId,
                        averagingDouble(Rating::getRating)));
        userBiases = ratings.stream()
                .collect(groupingBy(Rating::getUserId,
                        averagingDouble(Rating::getRating)));
        // TODO: initialize the data structures that hold the global/item/user biases
    }

    /** Dot‑product similarity; 0 if <10 shared items. */
    public double getSimilarity(int u1, int u2) {
        // TODO: implement
        return 0;
    }
    public List<User> getMatchingProfileUsers(int userId) {
        // Look at all users; for each user, check if they share 10+ rated items with the given user
        List<Integer> myItems = ratings.stream()
                .filter(r -> r.getUserId() == userId)
                .map(Rating::getItemId)
                .toList();

        return users.values().stream()
                .filter(user -> user.getId() != userId) // Exclude the specified user
                .filter(user -> {
                    long commonItemsCount = ratings.stream()
                            .filter(r -> r.getUserId() == user.getId())
                            .map(Rating::getItemId)
                            .filter(myItems::contains)
                            .distinct()
                            .count();
                    return commonItemsCount >= 10;
                })
                .toList();
    }

    @Override
    public List<T> recommendTop10(int userId){
        // TODO: implement
        return null;
    }

    public void printGlobalBias() {
        // TODO: fix
        System.out.println("Global bias: " + String.format("%.2f", globalBias));
    }

    public void printItemBias(int itemId) {
        // TODO: fix
        System.out.println("Item bias for item " + itemId + ": " + String.format("%.2f", itemBiases.get(itemId)));
    }
    public void printUserBias(int userId) {
        // TODO: fix
        System.out.println("User bias for user " + userId + ": " + String.format("%.2f",userBiases.get(userId)));
    }
}

