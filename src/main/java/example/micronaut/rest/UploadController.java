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
import io.micronaut.http.multipart.PartData;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.LongAdder;

@Singleton
@Controller("/upload")
public class UploadController {

    @Post(value =  "/receive-flow-control", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> go(Flowable<PartData> file) {
       return streamPartData(file);
    }

    @Post(value =  "/receive-streaming-file", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> streaming(StreamingFileUpload file) {
        return streamFile(file);
    }

    @Post(value =  "/receive-streaming-multiple", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> multipleStreaming(StreamingFileUpload file1,StreamingFileUpload file2) {
        return streamFile(file1).zipWith(streamFile(file2), (s, s2) -> "first: " + s + ", second: " + s2);
    }

    @Post(value =  "/receive-streaming-iterable", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_PLAIN)
    Single<String> streamingIterable(Flowable<StreamingFileUpload> files) {
          return files.subscribeOn(Schedulers.io()).flatMap((StreamingFileUpload upload) ->
                  Flowable.fromPublisher(streamFile(upload).toFlowable()).map(result -> result))
                  .collect(ArrayList<String>::new, (results, r) -> results.add(r)).map(results -> String.join(", ", results));
    }


    private Single<String> streamFile(StreamingFileUpload file) {
        return Single.create(singleEmitter ->
            file.subscribe(new PartDataSubscriber(singleEmitter))
        );
    }

    private Single<String> streamPartData(Flowable<PartData> file) {
        return Single.create(singleEmitter ->
            file.subscribe(new PartDataSubscriber(singleEmitter))
        );
    }

    class PartDataSubscriber implements Subscriber<PartData> {
        private Subscription subscription;
        private LongAdder longAdder = new LongAdder();
        private SingleEmitter singleEmitter;

        PartDataSubscriber(SingleEmitter singleEmitter) {
            this.singleEmitter = singleEmitter;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(PartData pd) {
            try {
                byte[] bytes = pd.getBytes();
                System.out.println("got " + bytes.length + " bytes.");
                longAdder.add(bytes.length);
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }
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
    }
}



