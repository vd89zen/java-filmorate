package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты FriendshipDbStorage")
class FriendshipDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserDbStorage userStorage;

    private FriendshipDbStorage storage;
    private Long userId1;
    private Long userId2;
    private Long userId3;
    private Long userId4;
    private int prefixEmail = 1;

    @BeforeEach
    void setUp() {
        storage = new FriendshipDbStorage(jdbcTemplate);
        userId1 = createTestUser();
        userId2 = createTestUser();
        userId3 = createTestUser();
        userId4 = createTestUser();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private Long createTestUser() {
        User user = User.builder()
                .email(String.format("%dtest@test.com", prefixEmail++))
                .login("testLogin")
                .name("testName")
                .birthday(LocalDate.now().minusYears(17))
                .build();
        return userStorage.create(user).getId();
    }

    @Nested
    @DisplayName("Тесты addFriend()")
    class AddFriendTests {
        @Test
        @DisplayName("Добавляем друга")
        void addFriend_Should_Add_Friend_Test() {
            // given
            Long user = userId1;
            Long friend = userId2;
            // when
            storage.addFriend(user, friend);
            // then
            assertThat(storage.isFriend(user, friend)).isTrue();
        }

        @Test
        @DisplayName("Проверяем выброс исключения при повторном добавлении друга")
        void addFriend_Should_Throw_Exception_On_Duplicate_Test() {
            // given
            Long user = userId1;
            Long friend = userId2;
            // when
            storage.addFriend(user, friend);
            // then
            assertThatThrownBy(() -> storage.addFriend(user, friend))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Не удалось добавить запись в БД");
            assertThat(storage.getUserFriendsCount(user)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Тесты getFriendsIdsOfUser()")
    class GetFriendsIdsOfUserTests {
        @Test
        @DisplayName("Получаем список IDs друзей")
        void getFriendsIdsOfUser_Should_Return_Correct_List_Test() {
            // given
            storage.addFriend(userId1, userId2);
            storage.addFriend(userId1, userId3);
            // when
            List<Long> friends = storage.getFriendsIdsOfUser(userId1);
            // then
            assertThat(friends)
                    .hasSize(2)
                    .containsExactlyInAnyOrder(userId2, userId3)
                    .doesNotContain(userId4);
        }

        @Test
        @DisplayName("Получение друзей для пользователя без друзей — должен вернуться пустой список")
        void getFriendsIdsOfUser_Should_Return_Empty_List_For_User_Without_Friends_Test() {
            // given, when
            List<Long> friends = storage.getFriendsIdsOfUser(userId4);
            // then
            assertThat(friends).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты getUserFriendsCount()")
    class GetUserFriendsCountTests {
        @Test
        @DisplayName("Получаем количество друзей юзера")
        void getUserFriendsCount_Should_Return_Correct_Count_Test() {
            // given
            storage.addFriend(userId1, userId2);
            storage.addFriend(userId1, userId3);
            storage.addFriend(userId2, userId4);
            // when
            int count1 = storage.getUserFriendsCount(userId1);
            int count2 = storage.getUserFriendsCount(userId2);
            // then
            assertThat(count1).isEqualTo(2);
            assertThat(count2).isEqualTo(1);
        }

        @Test
        @DisplayName("Получение количества друзей для пользователя без друзей — ждём 0")
        void getUserFriendsCount_Should_Return_Zero_For_User_Without_Friends_Test() {
            // given, when
            int count = storage.getUserFriendsCount(userId4);
            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("Тесты getFriendsCountByUsersIds()")
    class GetFriendsCountByUsersIdsTests {

        @Test
        @DisplayName("Получаем количество друзей по списку ID")
        void getFriendsCountByUsersIds_Should_Return_Correct_Map_Test() {
            // given
            storage.addFriend(userId1, userId2);
            storage.addFriend(userId1, userId3);
            storage.addFriend(userId2, userId4);
            Set<Long> userIds = Set.of(userId1, userId2, userId3, userId4);
            // when
            Map<Long, Integer> result = storage.getFriendsCountByUsersIds(userIds);
            // then
            assertThat(result)
                    .containsKey(userId1)
                    .containsKey(userId2)
                    .containsKey(userId3)
                    .containsKey(userId4)
                    .containsEntry(userId1, 2)
                    .containsEntry(userId2, 1)
                    .containsEntry(userId3, 0)
                    .containsEntry(userId4, 0);
        }

        @Test
        @DisplayName("Проверяем получение пустого списка для пустого набора ID")
        void getFriendsCountByUsersIds_Should_Return_Empty_Map_For_Empty_Set_Test() {
            // given, when
            Map<Long, Integer> result = storage.getFriendsCountByUsersIds(Set.of());
            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты isFriend()")
    class IsFriendTests {
        @Test
        @DisplayName("Проверяем возврат true для существующих друзей")
        void isFriend_Should_Return_True_For_Existing_Friendship_Test() {
            // given
            storage.addFriend(userId1, userId2);
            // when
            boolean isFriend = storage.isFriend(userId1, userId2);
            // then
            assertThat(isFriend).isTrue();
        }

        @Test
        @DisplayName("Проверка возврат false для не друзей")
        void isFriend_Should_Return_False_For_Non_Friends_Test() {
            // given, when
            boolean isFriend = storage.isFriend(userId1, userId3);
            // then
            assertThat(isFriend).isFalse();
        }

        @Test
        @DisplayName("Проверяем получение false для несуществующих юзеров")
        void isFriend_Should_Return_False_For_NonExisting_Users_Test() {
            // given, when
            boolean isFriend = storage.isFriend(666L, 999L);
            // then
            assertThat(isFriend).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты removeFriend()")
    class RemoveFriendTests {
        @Test
        @DisplayName("Удаляем друга")
        void removeFriend_Should_Remove_Specific_One_Way_Friendship_Test() {
            // given
            storage.addFriend(userId1, userId2);
            assertThat(storage.isFriend(userId1, userId2)).isTrue();
            assertThat(storage.isFriend(userId2, userId1)).isFalse();
            // when
            storage.removeFriend(userId1, userId2);
            // then
            assertThat(storage.isFriend(userId1, userId2)).isFalse();
            assertThat(storage.getUserFriendsCount(userId1)).isZero();
        }

        @Test
        @DisplayName("Попытка удаления несуществующей односторонней дружбы")
        void removeFriend_Should_Not_Affect_Other_Friendships_When_NonExisting_Test() {
            // given
            storage.addFriend(userId1, userId3);
            assertThat(storage.isFriend(userId1, userId2)).isFalse();
            // when, then
            assertThatCode(() -> storage.removeFriend(userId1, userId2))
                    .doesNotThrowAnyException();

            assertThat(storage.isFriend(userId1, userId3)).isTrue();
            assertThat(storage.getUserFriendsCount(userId1)).isEqualTo(1);
        }

        @Test
        @DisplayName("Проверяем что обратная связь нетронута при удаление дружбы одной стороной двусторонней дружбы")
        void removeFriend_Should_Keep_Reverse_Friendship_Intact_Test() {
            // given
            storage.addFriend(userId1, userId2);
            storage.addFriend(userId2, userId1);
            assertThat(storage.isFriend(userId1, userId2)).isTrue();
            assertThat(storage.isFriend(userId2, userId1)).isTrue();
            // when
            storage.removeFriend(userId1, userId2);
            // then
            assertThat(storage.isFriend(userId1, userId2)).isFalse();
            assertThat(storage.isFriend(userId2, userId1)).isTrue();
            assertThat(storage.getUserFriendsCount(userId1)).isZero();
            assertThat(storage.getUserFriendsCount(userId2)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Тесты getCommonFriends()")
    class GetCommonFriendsTests {
        @Test
        @DisplayName("Получаем общих друзей — ID пользователей, которых оба добавили")
        void getCommonFriends_Should_Return_Ids_Of_Users_Both_Have_Added_Test() {
            // given
            storage.addFriend(userId1, userId3);
            storage.addFriend(userId1, userId4);
            storage.addFriend(userId2, userId4);
            storage.addFriend(userId2, userId1);
            // when
            List<Long> common = storage.getCommonFriends(userId1, userId2);
            // then
            assertThat(common)
                    .hasSize(1)
                    .containsExactly(userId4);
        }

        @Test
        @DisplayName("Проверяем отсутствие общих друзей — если нет пользователя, которого оба добавили")
        void getCommonFriends_Should_Return_Empty_If_No_Common_Added_User_Test() {
            // given
            storage.addFriend(userId1, userId3);
            storage.addFriend(userId2, userId4);
            // when
            List<Long> common = storage.getCommonFriends(userId1, userId2);
            // then
            assertThat(common).isEmpty();
        }

        @Test
        @DisplayName("Проверяем получение пустого списка когда оба не добавили никого")
        void getCommonFriends_Should_Return_Empty_If_Both_Users_Have_No_Added_Friends_Test() {
            // given, when
            List<Long> common = storage.getCommonFriends(userId1, userId2);
            // then
            assertThat(common).isEmpty();
        }

        @Test
        @DisplayName("Получаем список нескольких общих друзей — все ID, которых оба добавили")
        void getCommonFriends_Should_Return_All_Common_Added_Ids_Test() {
            // given
            storage.addFriend(userId1, userId3);
            storage.addFriend(userId1, userId4);
            storage.addFriend(userId1, userId2);
            storage.addFriend(userId2, userId4);
            storage.addFriend(userId2, userId1);
            storage.addFriend(userId2, userId3);
            // when
            List<Long> common = storage.getCommonFriends(userId1, userId2);
            // then
            assertThat(common)
                    .hasSize(2)
                    .containsExactlyInAnyOrder(userId4, userId3);
        }
    }
}