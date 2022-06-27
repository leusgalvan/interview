<img src="/paidy.png?raw=true" width=300 style="background-color:white;">

# Solution to the Paidy take home exercise

## Assumptions regarding the requirements
- Tokens are like api keys, meaning they do not expire and don't need to be refreshed. We can then store them in an environment variable.
- The test token is safe to embed in code, for example to write some tests against the OneFrame API.
- Only currencies provided in Currency.scala are supported (even if OneFrame supports more).
- Rates from OneFrame come up-to-date, meaning we can cache them for 5 minutes and not break the 5 minutes requirement.
- If the rate from currency A to currency B is R, then the rate from B to A is 1/R.
- If the rate from currency A to B is R1 and the rate from currency B to C is R2, then the rate from A to C is R1 * R2

## Caching
### Description
In order to help fulfill 10,000 requests for a single token we are going to use a Redis cache to store the results from OneFrame. We will expire keys after 5 minutes to make sure rates stay up-to-date (at most 5 minutes old). 

Caching alone is not enough to prevent us from calling OneFrame more than 1,000 times. We'll use some optimizations.

First, we will use the fact that if we have the rate of A to B and B to C, we can calculate the rate of A to C as their product. 
We can then sort currencies by some criteria (e.g. alphabetically) to get a sequence C<sub>1</sub>, ..., C<sub>n</sub>. 
Once we have that sorted sequence, we can get the rates for every pair (C<sub>i</sub>, C<sub>i+1</sub>) and store them in a cache (i.e. Redis).
Lastly, we can calculate the rate for any pair (C<sub>i</sub>, C<sub>j</sub>) where i < j just by multiplying all the rates (C<sub>i</sub>, C<sub>i+1</sub>), (C<sub>i+1</sub>, C<sub>i+2</sub>),..., (C<sub>j-1</sub>, C<sub>j</sub>), without the need to fetch from OneFrame.

Second, we can calculate rates for pairs (C<sub>i</sub>, C<sub>j</sub>) where i > j by using the assumption that rate(C<sub>i</sub>, C<sub>j</sub>) == 1/rate(C<sub>j</sub>, C<sub>i</sub>). That is, we can get the rate R for pair (C<sub>j</sub>, C<sub>i</sub>) with the same procedure as before and then just return 1/R. 

Finally, the case where C<sub>i</sub> == C<sub>j</sub> does not need any calculation as it is always 1.

### Analysis
We have 9 supported currencies. This means the cache will contain 8 pairs. Assuming we refresh them every 5 minutes, and considering a day has 1440 minutes, we will need 1440 / 5 = 288 calls to OneFrame.

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
