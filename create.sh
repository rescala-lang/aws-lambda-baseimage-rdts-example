cp awslambda bootstrap
zip awstest.zip bootstrap

aws lambda create-function --function-name scala-runtime \
  --zip-file fileb://awstest.zip --handler function.handler --runtime provided \
  --role arn:aws:iam::647260201344:role/lambda-role