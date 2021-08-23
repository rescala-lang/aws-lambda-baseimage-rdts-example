#!/usr/bin/sh

aws lambda invoke --function-name scala-runtime --payload '{"text":"waat4"}' response.txt