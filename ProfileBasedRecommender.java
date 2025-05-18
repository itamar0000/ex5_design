import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/** Profile‑based recommender implementation. */
class ProfileBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    public ProfileBasedRecommender(Map<Integer, User> users,
                                   Map<Integer, T> items,
                                   List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    @Override
    public List<T> recommendTop10(int userId) {
        // קבלת רשימת המשתמשים התואמים (מין וגיל)
        List<User> matchingUsers = getMatchingProfileUsers(userId);
        if (matchingUsers.isEmpty()) return Collections.emptyList();

        // הכנת סט של מזהי המשתמשים התואמים
        Set<Integer> matchingUserIds = matchingUsers.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        // סט של פריטים שהמשתמש עצמו דירג (לסינון אחר כך)
        Set<Integer> itemsRatedByUser = ratings.stream()
                .filter(r -> r.getUserId() == userId)
                .map(Rating::getItemId)
                .collect(Collectors.toSet());

        return items.values().stream()
                .filter(item -> {
                    // כל הדירוגים עבור הפריט הנוכחי
                    Set<Rating<T>> itemRatings = ratingsByItem.getOrDefault(item.getId(), Set.of());

                    // סינון דירוגים רק ממשתמשים תואמים
                    List<Rating<T>> relevantRatings = itemRatings.stream()
                            .filter(r -> matchingUserIds.contains(r.getUserId()))
                            .collect(Collectors.toList());

                    // ממוצע הדירוגים
                    double avg = relevantRatings.stream()
                            .mapToDouble(Rating::getRating)
                            .average()
                            .orElse(0.0);

                    // המשתמש עצמו **לא** דירג את הפריט
                    boolean userDidNotRate = !itemsRatedByUser.contains(item.getId());

                    // סינון: לפחות 5 דירוגים, המשתמש לא דירג, ממוצע חיובי
                    return relevantRatings.size() >= 5 && userDidNotRate && avg > 0;
                })
                .sorted((item1, item2) -> {
                    // השוואה לפי ממוצע דירוג יורד
                    double avg1 = ratingsByItem.getOrDefault(item1.getId(), Set.of()).stream()
                            .filter(r -> matchingUserIds.contains(r.getUserId()))
                            .mapToDouble(Rating::getRating)
                            .average()
                            .orElse(0.0);

                    double avg2 = ratingsByItem.getOrDefault(item2.getId(), Set.of()).stream()
                            .filter(r -> matchingUserIds.contains(r.getUserId()))
                            .mapToDouble(Rating::getRating)
                            .average()
                            .orElse(0.0);

                    return Double.compare(avg2, avg1);
                })
                .limit(10)
                .collect(Collectors.toList());
    }


    public List<User> getMatchingProfileUsers(int userId) {
        User targetUser = users.get(userId);
        // if the user doesnt exist
        if (targetUser == null) return Collections.emptyList();

        // Get the target user's profile
        String gender = targetUser.getGender(); //

        return users.values().stream()
                .filter(user -> user.getGender().equals(gender))
                .filter(user -> user.getId() != userId) // do not include the user himself
                .filter(user -> user.getAge() >= targetUser.getAge() - 5 && user.getAge() <= targetUser.getAge() + 5)
                .collect(Collectors.toList());
        // TODO: implement
    }
}
