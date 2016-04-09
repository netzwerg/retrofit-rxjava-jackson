package ch.netzwerg.retrofit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javaslang.Tuple2;
import javaslang.control.Option;
import javaslang.jackson.datatype.JavaslangModule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Base64;

public class ServiceFactory {

    public static <S> S createService(String baseUrl, Class<S> serviceClass, Option<Tuple2<String, String>> credentials) {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder().addInterceptor(logging);

        credentials.forEach(t -> {
            String user = t._1;
            String password = t._2;
            String basic = "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
            httpClientBuilder.addInterceptor(chain -> {
                Request original = chain.request();

                Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", basic)
                        .header("Accept", "application/json")
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            });
        });

        ObjectMapper jacksonMapper = new ObjectMapper();
        jacksonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jacksonMapper.registerModule(new JavaslangModule());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(jacksonMapper))
                .client(httpClientBuilder.build())
                .build();

        return retrofit.create(serviceClass);
    }

}
