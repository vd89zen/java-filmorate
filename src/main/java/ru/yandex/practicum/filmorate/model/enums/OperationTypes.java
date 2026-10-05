package ru.yandex.practicum.filmorate.model.enums;

public enum OperationTypes {
    ADD(1),
    UPDATE(2),
    REMOVE(3);

    private final int id;

    OperationTypes(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
