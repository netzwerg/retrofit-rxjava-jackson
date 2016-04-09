package ch.netzwerg.retrofit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.control.Option;
import retrofit2.http.GET;
import rx.Observable;
import rx.observers.TestSubscriber;

public class ActivitiClientDemo {

    private static final String BASE_URL = "http://localhost:8090/activiti-rest/service/";

    private interface IdentityService {

        @GET("identity/users")
        Observable<ResultList<User>> getUsersResultList();

        default Observable<User> getUsers() {
            return ResultListExtractor.extract(getUsersResultList());
        }

    }

    private interface ResultListExtractor {
        static <T> Observable<T> extract(Observable<ResultList<T>> observableResultList) {
            return observableResultList.flatMap(resultList -> Observable.from(resultList.getData()));
        }
    }

    private static final class ResultList<T> {

        private final List<T> data;

        @JsonCreator
        public ResultList(@JsonProperty("data") List<T> data) {
            this.data = data;
        }

        public List<T> getData() {
            return data;
        }

    }

    private static final class User {

        private final String firstName;
        private final String lastName;

        @JsonCreator
        public User(@JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

    }

    public static void main(String... args) throws InterruptedException {
        Tuple2<String, String> credentials = Tuple.of("kermit", "kermit");
        IdentityService identityService = ServiceFactory.createService(BASE_URL, IdentityService.class, Option.of(credentials));

        Observable<User> users = identityService.getUsers();

        TestSubscriber<User> subscriber = new TestSubscriber<>();
        users.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.getOnNextEvents().forEach(
                user -> System.out.println(user.getFirstName() + " " + user.getLastName())
        );
        subscriber.getOnErrorEvents().forEach(Throwable::printStackTrace);
    }

}
