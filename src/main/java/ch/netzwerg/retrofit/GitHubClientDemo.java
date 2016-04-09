package ch.netzwerg.retrofit;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

public class GitHubClientDemo {

    private interface GitHub {
        @GET("repos/{owner}/{repo}/contributors")
        Observable<List<Contributor>> contributors(@Path("owner") String owner, @Path("repo") String repo);
    }

    private static final class Contributor {

        private final String login;
        private final int contributions;

        @JsonCreator
        public Contributor(@JsonProperty("login") String login, @JsonProperty("contributions") int contributions) {
            this.login = login;
            this.contributions = contributions;
        }

        String getLogin() {
            return login;
        }

        int getContributions() {
            return contributions;
        }

    }

    public static void main(String... args) throws InterruptedException {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        ObjectMapper jacksonMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(jacksonMapper))
                .build();

        GitHub gitHub = retrofit.create(GitHub.class);

        Observable<Contributor> contributors = gitHub.contributors("square", "retrofit").
                flatMap(Observable::from); // convert Observable<List<Contributor> to Observable<Contributor>

        TestSubscriber<Contributor> subscriber = new TestSubscriber<>();
        contributors.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.getOnNextEvents().forEach(
                contributor -> System.out.println(contributor.getLogin() + " (" + contributor.getContributions() + ")")
        );
        subscriber.getOnErrorEvents().forEach(Throwable::printStackTrace);
    }

}
