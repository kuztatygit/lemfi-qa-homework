package lemfi.model.signUp;

import lombok.Builder;
import lombok.Data;
import net.datafaker.Faker;

@Data
@Builder(toBuilder = true)
public class Registration {
    private final String email;
    private final String password;

    private static final Faker faker = new Faker();

    public static Registration ofRandom() {
        return Registration.builder()
                .email(faker.internet().emailAddress())
                .password(faker.internet().password())
                .build();
    }
}