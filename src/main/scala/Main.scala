package lambdatest

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import rescala.extra.lattices.delta.CContext.DietMapCContext
import rescala.extra.lattices.delta.Codecs._
import rescala.extra.lattices.delta.Delta
import rescala.extra.lattices.delta.crdt.reactive.RGA
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, ListObjectsV2Request, PutObjectRequest}

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import scala.jdk.CollectionConverters._
import scala.util.Random


object Main {

  def main(args: Array[String]): Unit = {
    val apiurl = System.getenv("AWS_LAMBDA_RUNTIME_API")
    val client = HttpClient.newHttpClient()

    val vmID = Random.nextLong().toString

    val bucketName = "de-tu-darmstadt-stg-crdt"

    var counter = 0

    val s3 = S3Client.builder().region(Region.EU_CENTRAL_1).build()

    implicit val todoTaskCodec: JsonValueCodec[TodoTask] = JsonCodecMaker.make

    implicit val taskListCodec: JsonValueCodec[List[TodoTask]] = JsonCodecMaker.make

    implicit val InputCodec: JsonValueCodec[InputEvent] = JsonCodecMaker.make

    def putDelta(deltaState: RGA.State[TodoTask, DietMapCContext]): Unit = {
      val key = s"$vmID:$counter"
      counter += 1

      s3.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(key).build(),
        RequestBody.fromBytes(writeToArray(deltaState))
      )
    }

    def getState(key: String): RGA.State[TodoTask, DietMapCContext] = {
      val resp = s3.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build())

      readFromArray[RGA.State[TodoTask, DietMapCContext]](resp.readAllBytes())
    }

    var todoList = {
      val listResponse = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).build())

      listResponse.contents().asScala.toList.map { obj =>
        getState(obj.key())
      }.foldLeft(RGA[TodoTask, DietMapCContext](vmID)) { (s, delta) =>
        s.applyDelta(Delta("remote", delta))
      }.resetDeltaBuffer()
    }

    while (true) {
      println("waiting for request")
      val req = HttpRequest.newBuilder().uri(
        URI.create(s"http://$apiurl/2018-06-01/runtime/invocation/next")
      ).build()

      val res   = client.send(req, BodyHandlers.ofString())
      val reqId = res.headers().firstValue("Lambda-Runtime-Aws-Request-Id").get()

      if (res.body().startsWith("{\n\"Records\":")) {
        println("Received S3 Trigger")

        val pattern = ".*key\": \"(?<key> [^\"]*)\".*".r

        val deltaStateKey = pattern.findFirstMatchIn(res.body()).get.group("key")

        if (!deltaStateKey.startsWith(vmID)) {
          val deltaState = getState(deltaStateKey)

          todoList = todoList.applyDelta(Delta("remote", deltaState)).resetDeltaBuffer()

          println(s"Applied delta from S3, new state: ${todoList.toList}")
        }
      } else {
        val response = readFromString[InputEvent](res.body()) match {
          case GetListEvent =>
            s"""{"statusCode": 200, "vmID": "$vmID", "body": "${writeToString[List[TodoTask]](todoList.toList)}"}"""

          case AddTaskEvent(desc) =>
            val task = TodoTask(desc)

            println(s"todoList before add: ${todoList.toList}")
            val mutatedList = todoList.prepend(task)
            println(s"mutatedList: ${mutatedList.toList}")
            println(s"deltaBuffer: ${mutatedList.deltaBuffer}")
            todoList = mutatedList.resetDeltaBuffer()
            println(s"deltaBuffer after reset: ${mutatedList.deltaBuffer}")

            putDelta(mutatedList.deltaBuffer.head.deltaState)

            s"""{"statusCode": 200, "vmID": "$vmID", "body": "${task.id}"}"""

          case ToggleTaskEvent(id) =>
            todoList.toList.find(_.id == id) match {
              case None =>
                s"""{"statusCode": 404, "vmID": "$vmID"}"""
              case Some(TodoTask(desc, done, _)) =>
                val mutatedList = todoList.updateBy(_.id == id, TodoTask(desc, done = !done, id))
                todoList = mutatedList.resetDeltaBuffer()

                putDelta(mutatedList.deltaBuffer.head.deltaState)

                s"""{"statusCode": 200, "vmID": "$vmID"}"""
            }

          case EditTaskEvent(id, desc) =>
            todoList.toList.find(_.id == id) match {
              case None =>
                s"""{"statusCode": 404, "vmID": "$vmID"}"""
              case Some(TodoTask(_, done, _)) =>
                val mutatedList = todoList.updateBy(_.id == id, TodoTask(desc, done, id))
                todoList = mutatedList.resetDeltaBuffer()

                putDelta(mutatedList.deltaBuffer.head.deltaState)

                s"""{"statusCode": 200, "vmID": "$vmID"}"""
            }

          case RemoveTaskEvent(id) =>
            val mutatedList = todoList.deleteBy(_.id == id)

            mutatedList.deltaBuffer.headOption match {
              case None =>
                s"""{"statusCode": 404, "vmID": "$vmID"}"""
              case Some(Delta(_, deltaState)) =>
                todoList = mutatedList.resetDeltaBuffer()

                putDelta(deltaState)

                s"""{"statusCode": 200, "vmID": "$vmID"}"""
            }

          case RemoveDoneEvent =>
            val toRemove = todoList.toList.filter(_.done)

            val mutatedList = todoList.deleteBy(toRemove.contains)

            if (mutatedList.deltaBuffer.nonEmpty) {
              putDelta(mutatedList.deltaBuffer.head.deltaState)

              todoList = mutatedList.resetDeltaBuffer()
            }

            s"""{"statusCode": 200, "vmID": "$vmID"}"""
        }


        val req2 = HttpRequest.newBuilder().uri(
          URI.create(s"http://$apiurl/2018-06-01/runtime/invocation/$reqId/response")
        ).method("POST", BodyPublishers.ofString(response)).build()

        client.send(req2, BodyHandlers.discarding())
      }
    }
  }

}
