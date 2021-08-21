see prerequisites:
https://docs.aws.amazon.com/lambda/latest/dg/runtimes-walkthrough.html

note, the following commands are fish shell, i.e., the (pwd) probably needs to replaced by something bash compatible like $(pwd)

build docker image
sudo docker build -t awscompile .

use to create native image
sudo docker run -v (pwd):/proj awscompile

edit update.sh for correct IAM role then run
./update.sh
