package lemfi.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import lombok.experimental.UtilityClass;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UtilityClass
public class ApiErrorAssertions {

    public class ApiNegativeAsserts {
        public static <B, T> void assertBadRequest(
                Supplier<B> builderSupplier,
                Consumer<B> modifier,
                Function<B, T> buildFunction,
                Function<T, Response> requestFunction,
                String expectedMessage
        ) throws JsonProcessingException {

            B builder = builderSupplier.get();
            modifier.accept(builder);
            T body = buildFunction.apply(builder);

            Response response = requestFunction.apply(body);
            assertEquals(400, response.getStatusCode());
            assertEquals(expectedMessage, response.jsonPath().getString("message"));
        }
    }


}
