package lemfi.model.personalDataUpdate;

import lombok.Builder;
import lombok.Data;
import net.datafaker.Faker;

import java.util.concurrent.ThreadLocalRandom;

@Data
@Builder(toBuilder = true)
public class PersonalData {
    private String firstName;
    private String surname;
    private Long personalId;

    private static final Faker faker = new Faker();

    public static PersonalData ofRandom() {
        return PersonalData.builder()
                .firstName(faker.name().firstName())
                .surname(faker.name().lastName())
                .personalId(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L))
                .build();
    }
}
