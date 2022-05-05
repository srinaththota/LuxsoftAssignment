import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.Await

class ReadSensorsTestSpec extends TestKit(ActorSystem("readsensorstest")) with WordSpecLike with BeforeAndAfterAll{
implicit val materializer = ActorMaterializer()

  "a simple Stream " should {
    "satisfy assertions " in {
      val simpleSource = Source(1 to 10)
      val simpleSink = Sink.fold(0)((a: Int, b: Int) => a + b)

      val addFuture = simpleSource.toMat(simpleSink)(Keep.right).run()
      val sum = Await.result(addFuture, 2 seconds)
      assert(sum == 55)
    }
    "integrated with test actors via materialized values " in {
      import akka.pattern.pipe
      import system.dispatcher
      val simpleSource = Source(1 to 10)
      val simpleSink = Sink.fold(0)((a: Int, b: Int) => a + b)
      val probe = TestProbe()
      val addFuture = simpleSource.toMat(simpleSink)(Keep.right).run().pipeTo(probe.ref)

      probe.expectMsg(55)
    }

    "integrate with Streams Testkit Sink " in {
      val sourceUnderTest = Source(1 to 5).map(_ * 2)
      val testSink = TestSink.probe[Int]

      val materializedTestValue = sourceUnderTest.runWith(testSink)
      materializedTestValue.request(5).expectNext(2,4,6,8,10).expectComplete()
    }
  }
}
