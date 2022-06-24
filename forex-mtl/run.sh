export ONE_FRAME_TOKEN=10dc303535874aeccc86a8251e6992f5

sudo docker pull redis
sudo docker pull paidyinc/one-frame:latest

sudo docker run -p 6379:6379 redis > /dev/null 2>&1 &
sudo docker run -p 8080:8080 paidyinc/one-frame > /dev/null 2>&1 &

sbt run
