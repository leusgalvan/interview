<img src="/paidy.png?raw=true" width=300 style="background-color:white;">

# Solution to the Paidy take home exercise

## Assumptions regarding the requirements
- Tokens are like api keys, meaning they do not expire and don't need to be refreshed. We can then store them in an environment variable.
- The test token is safe to embed in code, for example to write some tests against the OneFrame API.
- Only currencies provided in Currency.scala are supported (even if OneFrame supports more).
- Rates from OneFrame come up-to-date, meaning we can cache them for 5 minutes and not break the 5 minutes requirement.

## Caching
### Description
In order to help fulfill 10,000 requests for a single token we are going to use a Redis cache to store the results from OneFrame. We will expire keys after 5 minutes to make sure rates stay up-to-date (at most 5 minutes old). 

We will fetch all pairs in one single OneFrame request in order to minimize the impact on the quota.

### Analysis
We have 9 supported currencies. If we optimize the case where from and to currencies are equal, this
means there are 9 * 8 = 72 possible pairs. So our Redis cache will contain that number of entries.

Assuming we fetch every 5 minutes, and noting that a day has 1440 minutes, we will need 1440 / 5 =
288 requests to OneFrame per day, which does not exceed the quota.

## Running the application
In order to run the application, execute the following commands:

> cd forex-mtl

> sudo chmod +x run.sh

> ./run.sh

## Running the tests
To run the tests, execute:

> cd forex-mtl

> sudo chmod +x test.sh

> ./test.sh
