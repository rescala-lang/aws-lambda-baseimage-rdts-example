see prerequisites:
https://docs.aws.amazon.com/lambda/latest/dg/runtimes-walkthrough.html

build docker image
sudo docker build -t awscompile .

use to create native image
sudo docker run -v (pwd):/proj aws

edit update.sh for correct IAM role then run
./update.sh
