package fastparse.byte

import java.io.InputStream

import fastparse.byte.all._
import utest._

object LargeBmpIteratorTests extends TestSuite {

  class StreamToIteratorByte(stream: InputStream, bufferSize: Int) extends Iterator[Bytes] {
    val buffer = new Array[Byte](bufferSize)
    var bufferLen = 0
    var isRead = false

    private def readBuffer() = {
      bufferLen = stream.read(buffer)
      isRead = true
    }

    override def hasNext: Boolean =
      if (bufferLen != -1 && isRead) {
        true
      } else {
        readBuffer()
        bufferLen != -1
      }

    override def next(): Bytes = {
      if (!isRead)
        readBuffer()
      isRead = false
      Bytes.view(buffer.take(bufferLen))
    }
  }

  def lenaIterator = new StreamToIteratorByte(getClass.getResource("/lena.bmp").openStream(), 100)

  val tests = TestSuite {
    'large {
      val Parsed.Success(_, i) = BmpTests.BmpParse.bmp.parseIterator(lenaIterator)
      val expectedIndex = lenaIterator.map(_.length).sum
      assert(i == expectedIndex)
    }

    'maxInnerLength {
      val loggedInput = new IteratorParserInput(lenaIterator) {
        var maxInnerLength = 0

        override def dropBuffer(index: Int): Unit = {
          maxInnerLength = math.max(maxInnerLength, this.innerLength)
          super.dropBuffer(index)
        }
      }

      val Parsed.Success(_, i) = BmpTests.BmpParse.bmp.parseInput(loggedInput)
      println(s"Size: ${lenaIterator.map(_.length).sum}")
      println(s"Max buffer length: ${loggedInput.maxInnerLength}")
    }
  }
}
