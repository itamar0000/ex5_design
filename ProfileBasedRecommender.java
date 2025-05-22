import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Profile-based recommender system.
 * <p>
 * Recommends items based on ratings from users with a similar profile
 * (same gender and similar age).
 *
 * @param <T> the type of items to recommend
 */
class ProfileBasedRecommender<T extends Item> extends RecommenderSystem<T> {

    public ProfileBasedRecommender(Map<Integer, User> users,
                                   Map<Integer, T> items,
                                   List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    /**
     * Recommends up to 10 unrated items for a user based on ratings
     * from users with similar profile (same gender and age ±5).
     *
     * @param userId the ID of the target user
     * @return list of top recommended items
     */
    @Override
    public List<T> recommendTop10(int userId) {
        User targetUser = users.get(userId);
        if (targetUser == null) return List.of();

        Set<Integer> itemsRatedByUser = getItemsRatedByUser(userId);
        List<User> matchingUsers = getMatchingProfileUsers(userId);
        Set<Integer> matchingUserIds = matchingUsers.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<Rating<T>> matchingRatings = getRatingsByUsers(matchingUserIds);
        Map<Integer, List<Rating<T>>> ratingsByItem = groupRatingsByItem(matchingRatings);

        return ratingsByItem.entrySet().stream()
                .filter(entry -> !itemsRatedByUser.contains(entry.getKey()))         // skip items already rated
                .filter(entry -> entry.getValue().size() >= 5)                       // require at least 5 ratings
                .map(entry -> {
                    double avg = entry.getValue().stream()
                            .mapToDouble(Rating::getRating)
                            .average()
                            .orElse(0.0);
                    T item = items.get(entry.getKey());
                    return Map.entry(item, avg);
                })
                .sorted((e1, e2) -> {
                    int cmp = Double.compare(e2.getValue(), e1.getValue()); // avg rating descending
                    if (cmp != 0) return cmp;

                    int count1 = ratingsByItem.get(e1.getKey().getId()).size();
                    int count2 = ratingsByItem.get(e2.getKey().getId()).size();
                    cmp = Integer.compare(count2, count1); // count descending
                    if (cmp != 0) return cmp;

                    return e1.getKey().getName().compareTo(e2.getKey().getName()); // name ascending
                })
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of users with a matching profile (same gender and age ±5).
     *
     * @param userId the ID of the target user
     * @return list of matching users
     */
    public List<User> getMatchingProfileUsers(int userId) {
        User targetUser = users.get(userId);
        if (targetUser == null) return Collections.emptyList();

        String gender = targetUser.getGender();

        return users.values().stream()
                .filter(user -> user.getGender().equals(gender))
                .filter(user -> user.getId() != userId) // Exclude target user
                .filter(user -> Math.abs(user.getAge() - targetUser.getAge()) <= 5)
                .collect(Collectors.toList());
    }

    /**
     * Returns all ratings made by the given set of user IDs.
     *
     * @param userIds the set of user IDs
     * @return list of matching ratings
     */
    private List<Rating<T>> getRatingsByUsers(Set<Integer> userIds) {
        return ratings.stream()
                .filter(r -> userIds.contains(r.getUserId()))
                .collect(Collectors.toList());
    }

    /**
     * Groups a list of ratings by item ID.
     *
     * @param ratings the list of ratings
     * @return map from item ID to list of ratings
     */
    private Map<Integer, List<Rating<T>>> groupRatingsByItem(List<Rating<T>> ratings) {
        return ratings.stream()
                .collect(groupingBy(Rating::getItemId));
    }

    /**
     * Returns the set of item IDs that a user has already rated.
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
