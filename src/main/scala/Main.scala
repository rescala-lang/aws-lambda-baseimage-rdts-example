package lambdatest

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}

object Main {

  def main(args: Array[String]): Unit = {
    val apiurl = System.getenv("AWS_LAMBDA_RUNTIME_API")
    val client = HttpClient.newHttpClient()

    while (true) {
      val req = HttpRequest.newBuilder().uri(
        URI.create(s"http://${apiurl}/2018-06-01/runtime/invocation/next")
      ).build()

      val res       = client.send(req, BodyHandlers.ofString())
      val reqId     = res.headers().firstValue("Lambda-Runtime-Aws-Request-Id").get()
      val eventData = res.body()

      val req2 = HttpRequest.newBuilder().uri(
        URI.create(s"http://${apiurl}/2018-06-01/runtime/invocation/$reqId/response")
      ).method("POST", BodyPublishers.ofString(eventData)).build()

      client.send(req2, BodyHandlers.discarding())
    }
  }

}
