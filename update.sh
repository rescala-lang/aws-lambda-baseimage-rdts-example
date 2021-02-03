#!/usr/bin/sh

sbt nativeImage

cp target/native-image/awslambda bootstrap
zip awstest.zip bootstrap
aws lambda delete-function --function-name scala-runtime

aws lambda create-function --function-name scala-runtime \
  --zip-file fileb://awstest.zip --handler function.handler --runtime provided \
  --role arn:aws:iam::601982883386:role/lambda-role

aws lambda invoke --function-name scala-runtime --payload '{"text":"waat4"}' response.txt

