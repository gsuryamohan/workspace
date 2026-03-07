# surya-rest-api-client-2

Simple Java client that calls the deployed **SimpleMath** `multiply` API 1000 times with random positive integers.

## Prerequisites

- Java 11+ (use Java 17 for compatibility with EC2 Amazon Linux).

## Source layout

- `src/com/surya/client/SuryaRestApiClient2.java`

## Build and run

```bash
cd /Users/suryamohang/Documents/workspace/surya-rest-api-client-2

# Compile (Java 17 target for EC2)
javac --release 17 -d out src/com/surya/client/SuryaRestApiClient2.java

# Create runnable JAR
echo "Main-Class: com.surya.client.SuryaRestApiClient2" > manifest.txt
jar cfe surya-rest-api-client-2.jar com.surya.client.SuryaRestApiClient2 -C out .

# Run
java -jar surya-rest-api-client-2.jar
```

The program loops **1000 times**, each time generating two random positive integers and calling the SimpleMath `multiply` API.
