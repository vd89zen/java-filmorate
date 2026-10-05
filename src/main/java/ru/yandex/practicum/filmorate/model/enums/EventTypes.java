package ru.yandex.practicum.filmorate.model.enums;

public enum EventTypes {
    LIKE(1),
    REVIEW(2),
    FRIEND(3);

    private final int id;

    EventTypes(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
