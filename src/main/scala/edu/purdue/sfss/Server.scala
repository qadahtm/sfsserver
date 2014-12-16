/*
   Copyright 2014 - Thamir Qadah

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package edu.purdue.sfss

import akka.actor._
import akka.event._
import akka.dispatch._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer
import akka.io.{ IO, Tcp }
import java.net.InetSocketAddress
import org.apache.log4j.Logger
import akka.util.ByteString
import akka.util.ByteStringBuilder
import java.nio.charset.Charset
import java.nio.charset.MalformedInputException
import java.nio.charset.CodingErrorAction

class NetworkSocketControllerServer(filepath: String, host: String, port: Int, icount: Int, period: Int) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress(host, port))
  var count = icount

  def receive = {
    case b @ Bound(localAddress) => {
      log.info("Bound to : " + localAddress.toString())
    }

    case CommandFailed(_: Bind) => { context stop self }

    case c @ Connected(remote, local) =>
      {
        val connection = sender
        val handler = context.actorOf(Props(classOf[SimplisticHandler], filepath, count, period, connection))
        connection ! Register(handler)
        log.info("Connected to client at : " + remote.toString())
      }
    case cu: ChangeCount => {
      log.info("changing rate for all connections to " + cu.newCount)
      count = cu.newCount
      context.children.foreach(_ ! cu)
    }
    case _ => log.info("got something")
  }

}

// wrapper for count changing
case class ChangeCount(newCount: Int)

class SimplisticHandler(fp: String, icount: Int, period: Int, remote: ActorRef) extends Actor with ActorLogging {

  import Tcp._

  implicit val ec = context.system.dispatcher
  implicit val timeout = Timeout(3)

  implicit val codec = scala.io.Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.IGNORE)
  codec.onUnmappableCharacter(CodingErrorAction.IGNORE)

  var cfs = scala.io.Source.fromFile(fp).getLines
  var iterationCount: Long = 1
  var tupleSentCount: Int = 0
  var tobeSent: Int = 0

  var count = icount

  val s = context.system.scheduler.schedule(0 seconds, period seconds) {
    self ! "sendout"
  }

  def sendOutData(ccount: Int) = {
    val bsb = new ByteStringBuilder()
    while (cfs.hasNext && tupleSentCount < ccount) {
      bsb.putBytes((cfs.next + "\n").getBytes())
      remote ! Write(bsb.result)
      bsb.clear()
      tupleSentCount = tupleSentCount + 1
    }
  }

  def receive = {
    case Received(data) => { sender ! Write(ByteString("Server: You should not send anything to me. Please don't do it again.\n")) }
    case ChangeCount(nc) => {
      log.info(s"changing rate to $nc per second")
      count = nc
    }
    case PeerClosed => {
      log.info("Client Teminated, we have done " + iterationCount + " iterations over input file")
      s.cancel
      context stop self
    }

    case "sendout" => {
      //log.info("sending out")
      sendOutData(count)

      if (tupleSentCount != count) {
        //Disconnect on EOF
        //        log.info("EOF reached, Disconnecting client")   
        //        s.cancel
        //        context stop self

        // Repeat on EOF
        log.info("EOF reached, Reopening the file and starting from the begining")
        // file data finished before count reached
        // reopen the file and send the rest
        iterationCount = iterationCount + 1
        cfs = scala.io.Source.fromFile(fp).getLines
        val tobeSent = count - tupleSentCount
        sendOutData(tobeSent)
        tupleSentCount = 0

      } else {
        tupleSentCount = 0
      }
    }
  }
}