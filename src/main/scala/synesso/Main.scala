package synesso

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sse.scaladsl.EventSource
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.util.Success

//noinspection TypeAnnotation
object Main {

  /**
    * `curl -H "Accept: text/event-stream" -H "Last-Event-ID: 85025096736968704" "https://horizon.stellar.org/transactions"`
    * returns a seemingly valid SSE response.
    * The same data is parsed correctly by the utility at:
    *   https://www.stellar.org/laboratory/#explorer?resource=transactions&endpoint=all&values=eyJjdXJzb3IiOiI4NTAyNTA5NjczNjk2ODcwNCIsInN0cmVhbWluZyI6dHJ1ZX0%3D&network=public
    * But the following code fails to find any events.
    */
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("sse-test")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    def requestWithDebugging(r: HttpRequest): Future[HttpResponse] = {
      println("\n=== Reconnection ===")
      println("Request headers:")
      val request = r
      request.headers.foreach(h => println(s"* $h"))
      println()
      val f = Http().singleRequest(r)
      f.onComplete {
        case Success(r2) =>
          println("Response headers: ")
          r2.headers.foreach(h => println(s"* $h"))
        case _ =>
      }
      f
    }

    val work: Future[Done] =
      EventSource(Uri("https://horizon.stellar.org/transactions"), requestWithDebugging, Some("85025096736968704"), 1.second)
        .mapConcat{ case ServerSentEvent(data, eventType, id, retry) =>
          print(s"eventType: $eventType, id:$id, retry:$retry, data:")
          (if (eventType.contains("open")) None else Some(data)).to[collection.immutable.Iterable]
        }.toMat(Sink.foreach(println))(Keep.right).run()

    work.onComplete(_ => system.terminate())
    Await.ready(system.whenTerminated, Duration.Inf)
  }

}
