package lambdatest

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import rescala.extra.lattices.delta.CContext.DietMapCContext
import rescala.extra.lattices.delta.Delta
import rescala.extra.lattices.delta.crdt.reactive.AWSet
import rescala.extra.lattices.delta.Codecs._
import software.amazon.awssdk.core.sync.RequestBody
// import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, HeadObjectRequest, ListObjectsV2Request, PutObjectRequest}

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import scala.util.Random
import scala.jdk.CollectionConverters._


object Main {

  def main(args: Array[String]): Unit = {
    val apiurl = System.getenv("AWS_LAMBDA_RUNTIME_API")
    val client = HttpClient.newHttpClient()

    val vmID = Random.nextLong().toString

    scribe.info("test")

    val bucketName = "de-tu-darmstadt-stg-crdt"

    val deltaStateKey = "deltaState"

    //val httpClient = ApacheHttpClient.builder().build()

    val s3 = S3Client.builder().region(Region.EU_CENTRAL_1).build()

    implicit val intCodec: JsonValueCodec[Int] = JsonCodecMaker.make

    implicit val InputCodec: JsonValueCodec[List[Int]] = JsonCodecMaker.make

    val set = {
      val listResponse = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).build())

      listResponse.contents().asScala.toList.map { obj =>
        val resp = s3.getObject(GetObjectRequest.builder().bucket(bucketName).key(obj.key()).build())

        readFromArray[AWSet.State[Int, DietMapCContext]](resp.readAllBytes())
      }.foldLeft(AWSet[Int, DietMapCContext](vmID)) { (s, delta) =>
        s.applyDelta(Delta("remote", delta))
      }.resetDeltaBuffer()
    }

    println(f"Set state from combined deltas: ${set.elements}")

    while (true) {
      println("waiting for request")
      val req = HttpRequest.newBuilder().uri(
        URI.create(s"http://${apiurl}/2018-06-01/runtime/invocation/next")
      ).build()

      val res   = client.send(req, BodyHandlers.ofString())
      val reqId = res.headers().firstValue("Lambda-Runtime-Aws-Request-Id").get()
      //val eventData = res.body()

      println(s"got request ${res.body()} \nheaders: ${res.headers()}")

      val addList = readFromString[List[Int]](res.body())

      val mutatedSet = set.addAll(addList)

      val deltaState = mutatedSet.deltaBuffer.head.deltaState

      s3.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(deltaStateKey).build(),
        RequestBody.fromBytes(writeToArray(deltaState))
      )

      s3.waiter().waitUntilObjectExists(
        HeadObjectRequest.builder().bucket(bucketName).key(deltaStateKey).build()
      )

      val getResponse = s3.getObject(GetObjectRequest.builder().bucket(bucketName).key(deltaStateKey).build())

      val receivedState = readFromArray[AWSet.State[Int, DietMapCContext]](getResponse.readAllBytes())

      println("Received delta state")
      println(receivedState)

      val req2 = HttpRequest.newBuilder().uri(
        URI.create(s"http://${apiurl}/2018-06-01/runtime/invocation/$reqId/response")
      ).method("POST", BodyPublishers.ofString(s"""{"statusCode": 200, "vmID": "$vmID", "body": "$vmID"}""")).build()

      client.send(req2, BodyHandlers.discarding())
    }
  }

}
