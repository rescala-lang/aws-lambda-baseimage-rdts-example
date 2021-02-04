package lambdatest

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import scala.util.Random

object Main {

  def main(args: Array[String]): Unit = {
    val apiurl = System.getenv("AWS_LAMBDA_RUNTIME_API")
    val client = HttpClient.newHttpClient()

    val vmID = Random.nextLong().toString

    while (true) {
      println("waiting for request")
      val req = HttpRequest.newBuilder().uri(
        URI.create(s"http://${apiurl}/2018-06-01/runtime/invocation/next")
      ).build()

      val res   = client.send(req, BodyHandlers.ofString())
      val reqId = res.headers().firstValue("Lambda-Runtime-Aws-Request-Id").get()
      //val eventData = res.body()

      println(s"got request ${res.body()} \nheaders: ${res.headers()}")

      val req2 = HttpRequest.newBuilder().uri(
        URI.create(s"http://${apiurl}/2018-06-01/runtime/invocation/$reqId/response")
      ).method("POST", BodyPublishers.ofString(s"""{"statusCode": 200, "vmID": "$vmID", "body": "$vmID"}""")).build()

      client.send(req2, BodyHandlers.discarding())
    }
  }

}
