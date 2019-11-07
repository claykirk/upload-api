# Description

I created this repository to reproduce an issue I am running into when uploading large files using micronaut/netty in memory (micronaut.server.multipart.disk=false). 
My use case requires that I use memory vs disk due to data policies. I've created a controller that receives a Flowable<byte[]> and consumes the flowable when suscribed. 
For relatively small files the application performs well. However when files are uploaded that exceed the heap space defined for the application the heap space is exhausted and an error is thrown.
Moreover, if I send multiple files that in total size exceed the heap space an error is thrown. So it seems the application's ability to stream files is bound
by the heap space allocated. 

I also captured a heap dump and exported it to html. It is included in this repo under "heap-dump". Just open the "index.html" in a browser.

Thanks for the help in advance. I really appreciate it.

Cheers!

Clay


###How to Reproduce:

####Using Curl:
1. Run the application

    From the project root folder:
    
     `./gradlew clean build -x test && java -Xmx1g -jar build/libs/upload-api.jar`
     
     If you want heap dumps:
     
     `./gradlew clean build -x test && java -Xmx1g -XX:+HeapDumpOnOutOfMemoryError -jar build/libs/upload-api.jar`

    The server runs on http://localhost:8080
    
2. Test using curl. You will need a file > 1G to reproduce the heap space error.

    `curl "http://localhost:8080/upload/receive-flow-control" -H "Content-Type:multipart/form-data"  -F "file=@your-test-file"`
    
    
####Using Spock:
There is a unit test that simulates multiple requests in parrallel. I'm not sure if I'm simulating requests
correctly (subscribing on Schedulers.io) but I'm also seeing the issue using multiple curl commands.

1. To run the test

    From the project root folder:
    
    `./gradlew clean test`
 

