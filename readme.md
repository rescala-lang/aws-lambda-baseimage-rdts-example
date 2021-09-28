# Delta CRDTs in AWS Lambda

This case study uses delta CRDTs to implement a todolist application in AWS Lambda. Amazon S3 is used for persistent storage of the todolist.

## Prerequisites

General information about publishing a custom runtime to AWS Lambda:
https://docs.aws.amazon.com/lambda/latest/dg/runtimes-walkthrough.html

In the IAM console, create a role to be used by the lambda function and give it the permissions AWSLambdaBasicExecutionRole and AmazonS3FullAccess. In create.sh, replace the value in the --role parameter with the ARN of this role.

In Amazon S3, create a bucket and replace the bucketName value in src/main/scala/Main.scala:30 with the name of this bucket.

## Build

note, the following commands are fish shell, i.e., the (pwd) probably needs to replaced by something bash compatible like $(pwd)

build docker image
sudo docker build -t awscompile .

use to create native image
sudo docker run -v (pwd):/proj awscompile

when uploading your code for the first time, run
./create.sh

afterwards, you can upload changes after building by running
./update.sh

## API

The todolist modeled in this case study can be accessed by executing ./test.sh . The following request objects can be passed in the payload parameter:

### GetList

Returns the current content of the todolist.

#### Request

```json
{
  "type": "GetListEvent"
}
```

#### Response

The response object contains the following fields:

* statusCode: HTTP status code (200 on success)
* vmID: Indicates the lambda instance that this request was processed in
* body: A list of objects that represent the tasks in the last. Each task object has the following fields:
  * desc: The description of the task
  * id: The id of the task, can be used to modify or delete the task
  * done: Indicates whether the task is marked as done (only included in the response if it is true)

### AddTask

Add a new task to the todolist.

#### Request

```json
{
  "type": "AddTaskEvent",
  "desc": "Write Code"
}
```

Parameters:

* desc: The description of the task to be added

#### Response

The response object contains the following fields:

* statusCode: HTTP status code (200 on success)
* vmID: Indicates the lambda instance that this request was processed in
* body: The id of the task, can be used to modify or delete the task

### EditTask

Edit the description of a task in the todolist.

#### Request

```json
{
  "type": "EditTaskEvent",
  "id": "abc123",
  "desc": "Updated description"
}
```

Parameters:

* id: The id of the task to be edited
* desc: The new description to be given to the task

#### Response

The response object contains the following fields:

* statusCode: HTTP status code (200 on success, 404 if there exists no task with the specified id)
* vmID: Indicates the lambda instance that this request was processed in

### ToggleTask

Toggle the "done" field of a task.

#### Request

```json
{
  "type": "ToggleTaskEvent",
  "id": "abc123"
}
```

Parameters:

* id: The id of the task to be toggled

#### Response

The response object contains the following fields:

* statusCode: HTTP status code (200 on success, 404 if there exists no task with the specified id)
* vmID: Indicates the lambda instance that this request was processed in

### RemoveTask

Remove a task from the todolist.

#### Request

```json
{
  "type": "RemoveTaskEvent",
  "id": "abc123"
}
```

Parameters:

* id: The id of the task to be removed

#### Response

The response object contains the following fields:

* statusCode: HTTP status code (200 on success, 404 if there exists no task with the specified id)
* vmID: Indicates the lambda instance that this request was processed in

### RemoveDone

Remove all tasks marked as done from the todolist.

#### Request

```json
{
  "type": "RemoveDoneEvent"
}
```

#### Response

The response object contains the following fields:

* statusCode: HTTP status code (200 on success)
* vmID: Indicates the lambda instance that this request was processed in