#!/usr/bin/sh

aws lambda invoke --function-name scala-runtime --payload '{"type":"GetListEvent"}' response.txt