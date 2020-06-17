import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.io.StdIn
import scala.util.{Failure, Success}

object Example extends App {

  import akka.http.scaladsl.server.Directives._

  implicit val system = ActorSystem()

  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val routes: Route =
    get {
      pathPrefix("user") {
        path("def") {
          complete {
            HttpResponse(entity =
              //el entity de chuncl se formo de content type y el stream
              HttpEntity.Chunked.fromData(ContentTypes.`text/plain(UTF-8)`,
                Source.single(ByteString("ab")) ++
                Source.single(ByteString("ef")) ++
                Source.single(ByteString("gh"))))
            //source es la absraccion de akkaStream para representar el stream, de donde viene
          }
        } ~
          pathEnd {
            complete("just /user")
          }
      }
    }


  Http().bindAndHandleAsync(Route.asyncHandler(routes), "localHost", 8080)
    .onComplete {
      case Success(_) =>
        println("Server started on port 8080. Press Enter to terminate")
        StdIn.readLine() // let it run until user presses return
        system.terminate()
      case Failure(e) =>
        println("Binding failed")
        e.printStackTrace
        system.terminate()
    }

}
