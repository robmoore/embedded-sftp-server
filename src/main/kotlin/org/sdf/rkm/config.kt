package org.sdf.rkm

import org.apache.sshd.common.NamedFactory
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.apache.sshd.common.util.SecurityUtils
import org.apache.sshd.common.util.ValidateUtils
import org.apache.sshd.server.Command
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.subsystem.sftp.AbstractSftpEventListenerAdapter
import org.apache.sshd.server.subsystem.sftp.Handle
import org.apache.sshd.server.subsystem.sftp.SftpEventListener
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PreDestroy

@Configuration
class SftpServer(val port: Int = 2222, val sshd: SshServer = SshServer.setUpDefaultServer()) {

    @Bean
    fun configServer(sftpEventListener: SftpEventListener): SshServer {
        System.err.println("Starting SSHD on port " + port)
        sshd.port = port

        sshd.keyPairProvider = createHostKeyProvider()
        // accept any credentials where username == password
        sshd.setPasswordAuthenticator { username, password, session -> username == password }
        sshd.publickeyAuthenticator = AcceptAllPublickeyAuthenticator.INSTANCE

        val sftpSubsystemFactory = SftpSubsystemFactory()
        sftpSubsystemFactory.addSftpEventListener(sftpEventListener)
        sshd.subsystemFactories = listOf<NamedFactory<Command>>(sftpSubsystemFactory)

        sshd.fileSystemFactory = DefaultFileSystemFactory(defaultHomeDir = Paths.get("build/sftp/home"))

        sshd.start()

        return sshd
    }

    @Throws(IOException::class)
    private fun createHostKeyProvider(): KeyPairProvider {
        val hostKeyFile = File("build/keys/key.pem").toPath()
        val hostKeyProvider = SecurityUtils.createGeneratorHostKeyProvider(hostKeyFile)
        hostKeyProvider.algorithm = KeyUtils.RSA_ALGORITHM

        ValidateUtils.checkNotNullAndNotEmpty(hostKeyProvider.loadKeys(),
                "Failed to load keys from %s", hostKeyFile)

        return hostKeyProvider
    }

    @Bean
    fun createListener(uploadNotification: UploadNotification): SftpEventListener {
        return DefaultSftpEventListener(uploadNotification)
    }

    @PreDestroy
    fun shutdownServer() {
        sshd.stop()
    }
}

class DefaultSftpEventListener(val uploadNotification: UploadNotification) : AbstractSftpEventListenerAdapter() {
    override fun close(session: ServerSession, remoteHandle: String, localHandle: Handle) {
        super.close(session, remoteHandle, localHandle)
        val path = localHandle.file
        if (Files.isRegularFile(path)) {
            uploadNotification.process(path.toFile().readBytes())
        }
    }
}

class DefaultFileSystemFactory(defaultHomeDir: Path) : VirtualFileSystemFactory(defaultHomeDir) {
    @Throws(IOException::class)
    override fun computeRootDir(userName: String): Path = getUserHomeDir(userName) ?: makePath(userName)

    private fun makePath(userName: String): Path {
        val p = defaultHomeDir.resolve(userName)
        if (Files.exists(p) && !Files.isDirectory(p)) {
            NotDirectoryException(p.toString())
        } else {
            Files.createDirectories(p)
        }
        setUserHomeDir(userName, p)
        return p
    }
}