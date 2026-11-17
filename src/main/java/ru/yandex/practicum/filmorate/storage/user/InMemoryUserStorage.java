package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 0;

    @Override
    public User create(User user) {
        user.setId(++currentId);
        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public List<User> getAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> getById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        getById(userId).ifPresent(u -> u.getFriends().add(friendId));
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        getById(userId).ifPresent(u -> u.getFriends().remove(friendId));
    }

    @Override
    public List<User> getFriends(Long userId) {
        User user = users.get(userId);
        if (user == null || user.getFriends() == null) {
            return Collections.emptyList();
        }
        List<User> friends = new ArrayList<>();
        for (Long friendId : user.getFriends()) {
            if (users.containsKey(friendId)) {
                friends.add(users.get(friendId));
            }
        }
        return friends;
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        List<User> userFriends = new ArrayList<>(getFriends(userId));
        userFriends.retainAll(getFriends(otherId));
        return userFriends;
    }

    @Override
    public void delete(Long id) {
        users.remove(id);
    }
}