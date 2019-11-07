package example.micronaut

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.DefaultHttpClient
import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.inject.Inject
import java.time.Duration

@MicronautTest
@Slf4j
class UploadSpec extends Specification {

    @Inject
    @Shared
    EmbeddedServer server

    @Shared
    HttpClient client

    final Long TEN_MB = 1024L * 1024L * 10

    def setupSpec() {
        client = new DefaultHttpClient(server.URL, new DefaultHttpClientConfiguration(
                readTimeout: Duration.ofMinutes(5), readIdleTimeout: Duration.ofMinutes(5)
        ))
    }

    def "test parrallel receiving flowables"() {
        given:
        Integer count = 0
        def val = ('abcdefghij' * TEN_MB).bytes
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("file", "baf.txt", val)
                .build()

        when:
        HttpResponse<String> response
        Observable.range(0, 50).flatMap(new Function<Integer, ObservableSource<? extends HttpResponse<String>>>() {
            @Override
            ObservableSource<? extends HttpResponse<String>> apply(@NonNull Integer integer) throws
                    Exception {
                Observable.just(integer)
                        .subscribeOn(Schedulers.io())
                        .map(new Function<Integer, HttpResponse<String>>() {
                            @Override
                            HttpResponse<String> apply(@NonNull Integer index) throws Exception {
                                return Flowable.fromPublisher(client.exchange(
                                        HttpRequest.POST("/upload/receive-flow-control", requestBody)
                                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                                .accept(MediaType.TEXT_PLAIN_TYPE),
                                        String
                                )).blockingLast()
                            }
                        })
            }
        }).subscribe(new Consumer<HttpResponse<String>>() {
            @Override
            void accept(HttpResponse<String> stringHttpResponse) throws Exception {
                response = stringHttpResponse
                log.info("body -> " + stringHttpResponse.body())
                if (response.code() == HttpStatus.OK.code) {
                    count++
                }
            }
        })

        then:
        new PollingConditions(timeout: 30, delay: 1).eventually {
            assert count == 50
        }

    }
}
