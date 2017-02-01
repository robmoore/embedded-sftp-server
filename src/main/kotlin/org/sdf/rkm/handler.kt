package org.sdf.rkm

import mu.KLogging
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.stereotype.Service

@Service
@EnableBinding(Processor::class)
class UploadHandler() {
    companion object : KLogging()

    @StreamListener(Sink.INPUT)
    fun process(upload: ByteArray) {
        println("Received payload of size ${upload.size}")
    }
}
