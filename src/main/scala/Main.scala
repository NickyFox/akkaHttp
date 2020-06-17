import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}

import scala.concurrent.Future
import scala.concurrent.duration._

object Main extends App {

  implicit val system = ActorSystem("QuickStart")
  //code here

  /**
    * source tiene el primero que es el tipo de dato que va a emitir, y el segundo el "materialized value"
    *  allows running the source to produce some auxiliary value (e.g. a network source may provide information
    *  about the bound port or the peer’s address). Where no auxiliary information is produced, the type akka.NotUsed
    *  is used.
    */
  val source: Source[Int, NotUsed] = Source(1 to 100)

  val done: Future[Done] = source.runForeach(i => println(i))

  /**
    * When running this source in a scala.App you might notice it does not terminate, because the ActorSystem is
    * never terminated. Luckily runForeach returns a Future[Done] which resolves when the stream finishes:
    */




  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  val result: Future[IOResult] =
    factorials.map(num => ByteString(s"$num\n")).runWith(FileIO.toPath(Paths.get("factorials.txt")))



  /**
    * First we use the scan operator to run a computation over the whole stream: starting with the number 1 (BigInt(1))
    * we multiply by each of the incoming numbers, one after the other; the scan operation emits the initial value and
    * then every calculation result. This yields the series of factorial numbers which we stash away as a Source for
    * later reuse—it is important to keep in mind that nothing is actually computed yet, this is a description of
    * what we want to have computed once we run the stream. Then we convert the resulting series of numbers into a
    * stream of ByteString objects describing lines in a text file. This stream is then run by attaching a file as the
    * receiver of the data. In the terminology of Akka Streams this is called a Sink. IOResult is a type that IO
    * operations return in Akka Streams in order to tell you how many bytes or elements were consumed and whether the
    * stream terminated normally or exceptionally.
    */

  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  /**
    * Starting from a flow of strings we convert each to ByteString and then feed to the already known file-writing
    * Sink. The resulting blueprint is a Sink[String, Future[IOResult] ], which means that it accepts strings as its
    * input and when materialized it will create auxiliary information of type Future[IOResult] (when chaining
    * operations on a Source or Flow the type of the auxiliary information—called the “materialized value”—is given by
    * the leftmost starting point; since we want to retain what the FileIO.toPath sink has to offer, we need to say
    * Keep.right).
    */

  factorials.map(_.toString).runWith(lineSink("factorial2.txt"))


  /**
    * All operations so far have been time-independent and could have been performed in the same fashion on strict
    * collections of elements. The next line demonstrates that we are in fact dealing with streams that can flow at a
    * certain speed: we use the throttle operator to slow down the stream to 1 element per second.
    */

  factorials
    .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
    .throttle(1, 1.second)
    .runForeach(println)

  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())


}

