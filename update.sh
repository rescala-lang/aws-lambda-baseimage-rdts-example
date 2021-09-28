#!/usr/bin/sh

rm awstest.zip
rm bootstrap
cp awslambda bootstrap
zip awstest.zip bootstrap
aws lambda update-function-code --function-name scala-runtime \
  --zip-file fileb://awstest.zip

