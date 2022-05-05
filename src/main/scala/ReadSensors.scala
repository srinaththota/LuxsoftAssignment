import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, SourceShape}
import akka.stream.scaladsl.{Concat, FileIO, Flow, Framing, GraphDSL, Sink, Source}
import akka.util.ByteString

import java.nio.file.Paths

case class Record(sensor:String,reading:String)
case class MergedRecord(sensor:String,mergedReading:List[String])
object ReadSensors extends App {

  implicit val actorSystem = ActorSystem("readsensors") // created actor system
  implicit val materializer = ActorMaterializer()

  val logFile1 = Paths.get("src/main/resources/leader-1.csv")
  val logFile2 = Paths.get("src/main/resources/leader-2.csv")

  val source1: Source[ByteString, _] = FileIO.fromPath(logFile1)
  val source2: Source[ByteString, _] = FileIO.fromPath(logFile2)



  val sourceGraph = Source.fromGraph(
    GraphDSL.create(){
      implicit builder =>
      import GraphDSL.Implicits._
      val concat = builder.add(Concat[ByteString](2))
        source1 ~> concat
        source2 ~> concat
      SourceShape(concat.out)
  })
  val flow = Framing
   // .delimiter(ByteString(System.lineSeparator()), maximumFrameLength = 512, allowTruncation = true)
    .delimiter(ByteString("\n"), maximumFrameLength = 512, allowTruncation = true)
    .map(_.utf8String).filter(x => !(x contains "sensor")).map(str=>str.split(",")).map(arr=>{

    Record(arr(0),arr(1))

  })
  /*
    .fold(Record)((m,e)=>{
    val newVal = e.reading.toInt + m.tupled.
      Record(e.sensor,newVal.toString)
  })
*/
  val flow1 = Flow[Record].filter(x=>x.reading == "NaN")
  val sink = Sink.foreach(println)

  //sourceGraph.via(flow).to(sink).run()
  sourceGraph.via(flow).via(flow1).to(sink).run()
}
