import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.io.StdIn
import scala.util.{Failure, Success}



object AkkaHttpServerExample extends App {

  import akka.http.scaladsl.server.Directives._

  implicit val system = ActorSystem()

  import system.dispatcher

  implicit val materializer = ActorMaterializer()


  val userRoute = path("user" / IntNumber) { id =>
    complete(s"hi user number $id")
  }

  val route: Route =
    get {
      userRoute ~
      path("hello"){
        complete("hello World, akka Http")
      } ~
      path("another"){
        parameter("name", "age".as[Int]) { (name, age) =>
          complete(s"Hello $name, you are $age years old")
        } ~
        parameter("age"){ age =>
          //raw header es para uno customizado
          respondWithHeader(RawHeader("X-INDIO", "test")){
            complete(s"your age $age")
          }

        }~
      }
    }


  Http().bindAndHandleAsync(Route.asyncHandler(route), "localHost", 8080)
    .onComplete {
      case Success(_) =>
        println("Server started on port 8080. Press Enter to terminate")
        StdIn.readLine()
        system.terminate()
      case Failure(e) =>
        println("Binding failed")
        e.printStackTrace
        system.terminate()
    }

}
