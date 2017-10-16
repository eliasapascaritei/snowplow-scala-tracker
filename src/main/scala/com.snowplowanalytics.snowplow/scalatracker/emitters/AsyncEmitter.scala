/*
 * Copyright (c) 2015-2017 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.scalatracker
package emitters

import java.util.concurrent.LinkedBlockingQueue

import RequestUtils.{ CollectorRequest, GetCollectorRequest }

object AsyncEmitter {
  // Avoid starting thread in constructor
  /**
   * Start async emitter with single event payload
   *
   * @param host collector host
   * @param port collector port
   * @param https should this use the https scheme
   * @return emitter
   */
  def createAndStart(host: String, port: Int = 80, https: Boolean = false): AsyncEmitter = {
    val emitter = new AsyncEmitter(host, port, https)
    emitter.startWorker()
    emitter
  }
}

/**
 * Asynchronous emitter using LinkedBlockingQueue
 *
 * @param host collector host
 * @param port collector port
 * @param https should this use the https scheme
 */
class AsyncEmitter private(host: String, port: Int, https: Boolean = false) extends TEmitter {

  val queue = new LinkedBlockingQueue[CollectorRequest]()

  val worker = new Thread {
    override def run() {
      while (true) {
        val event = queue.take()
        RequestUtils.send(queue, host, port, https, event)
      }
    }
  }

  worker.setDaemon(true)

  /**
   * Method called to send an event from the tracker to the emitter
   * Adds the event to the queue
   *
   * @param event Fully assembled event
   */
  def input(event: Map[String, String]): Unit = {
    queue.put(GetCollectorRequest(1, event))
  }

  private def startWorker(): Unit = {
    worker.start()
  }
}

