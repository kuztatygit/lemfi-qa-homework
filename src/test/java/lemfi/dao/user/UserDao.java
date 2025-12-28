package lemfi.dao.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public User selectUser(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM users WHERE email = ?",
                (rs, rowNum) -> {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    user.setFirstName(rs.getString("first_name"));
                    user.setSurname(rs.getString("surname"));
                    user.setPersonalId(rs.getLong("personal_d"));
                    user.setBalance(rs.getBigDecimal("balance"));
                    return user;
                },
                email
        );
    }

    public void deleteByEmail(String email) {
        jdbcTemplate.update(
                "DELETE FROM users WHERE email = ?",
                email
        );
    }
}
