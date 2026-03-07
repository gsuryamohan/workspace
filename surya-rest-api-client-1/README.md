# surya-rest-api-client-1

Simple Java client that calls the deployed **SimpleMath** `add` API 1000 times with random positive integers.

## Prerequisites

- Java 11+ installed on your Mac (you already have Java for the other project).

## Source layout

- `src/com/surya/client/SuryaRestApiClient1.java`

## How to compile and run

```bash
cd /Users/suryamohang/Documents/workspace/surya-rest-api-client-1

# Compile
javac -d out src/com/surya/client/SuryaRestApiClient1.java

# Run
java -cp out com.surya.client.SuryaRestApiClient1
```

The program will:

- Loop **1000 times**
- On each iteration:
  - Generate two random positive integers
  - Call your EC2-hosted `SimpleMath` `add` API
  - Print the HTTP status, inputs, and response body

