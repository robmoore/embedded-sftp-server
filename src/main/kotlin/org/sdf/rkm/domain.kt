package org.sdf.rkm

import org.springframework.cloud.stream.messaging.Source
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Component
class UploadNotification(val source: Source) {
    fun process(upload: ByteArray) {
        source.output().send(MessageBuilder.withPayload(upload).build())
    }
}
