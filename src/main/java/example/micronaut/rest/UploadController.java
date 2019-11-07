/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.micronaut.rest;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.inject.Singleton;
import java.util.concurrent.atomic.LongAdder;

@Singleton
@Controller("/upload")
public class UploadController {

    @Post(value =  "/receive-flow-control", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> go(Flowable<byte[]> file) {
        return Single.create(singleEmitter -> {
            System.out.println("Making request...");
            file.subscribe(new Subscriber<byte[]>() {
                private Subscription subscription;
                private LongAdder longAdder = new LongAdder();

                @Override
                public void onSubscribe(Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(1);
                }

                @Override
                public void onNext(byte[] bytes) {
                    longAdder.add(bytes.length);
                    subscription.request(1);
                }

                @Override
                public void onError(Throwable throwable) {
                    singleEmitter.onError(throwable);
                }

                @Override
                public void onComplete() {
                    singleEmitter.onSuccess(Long.toString(longAdder.longValue()));
                }
            });
        });
    }

    public static class Data {
        String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "title='" + title + '\'' +
                    '}';
        }
    }
}



