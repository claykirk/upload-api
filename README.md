# Description

I created this repository to reproduce an issue I am running into when uploading large files using micronaut/netty in memory (micronaut.server.multipart.disk=false). 
My use case requires that I use memory vs disk due to data policies. I've created a controller that receives a StreamingFileUpload and consumes the file when subscribed. 
If I pass in an unrecognized parameter and that parameter is large (large than heap) the application crashes with OOM heap space.

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
    
2. Test using curl. You will need a file > 1G (Allocated Heap) to reproduce the heap space error.

    ` curl "http://localhost:8080/upload/receive-streaming-file" -H "Content-Type:multipart/form-data" -F  "unrecognizedParam=@larger-than-heap-file"`
  
 

