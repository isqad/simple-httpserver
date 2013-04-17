package httpserver

import scala.actors.Actor

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader


class RequestHandler extends Actor {

  def act() {
    println("Started Server...")

    while(true) {
      receive {
        case client:Socket => {
          println("Received message from " + client.getInetAddress() + ":" + client.getPort())
          //Считываем заголовки запроса
          var is:InputStream = client.getInputStream()

          var buffer:BufferedReader = new BufferedReader(new InputStreamReader(is))
          println("First header Line: " + buffer.readLine())
        }
        case _ => println("Request error, discarded!")
      }
    }
  }
}

class HttpServer(port: Int, address: InetAddress) extends Actor {
  private var server:ServerSocket = new ServerSocket(port, 10, address)

  private var handler:RequestHandler = new RequestHandler

  handler.start()

  def act() {
    while(true) {
      var socket:Socket = server.accept()
      handler ! socket
    }
  }
}


object Server extends App {
  var address:InetAddress = InetAddress.getByName("127.0.0.1")

  var server:HttpServer = new HttpServer(1234, address)
  server.start()
}