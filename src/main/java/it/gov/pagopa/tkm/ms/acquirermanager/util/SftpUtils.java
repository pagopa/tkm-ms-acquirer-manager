package it.gov.pagopa.tkm.ms.acquirermanager.util;

import it.gov.pagopa.tkm.ms.acquirermanager.filter.SftpAcquirerFileResourceFilter;
import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Log4j2
@Component
public class SftpUtils {
    @Value("${sftp.sftpHostname}")
    private String sftpHostname;

    @Value("${sftp.sftpPort}")
    private int sftpPort;

    @Value("${sftp.sftpPrivateKey}")
    private String sftpPrivateKey;

    @Value("${sftp.sftpPassPhrase}")
    private String sftpPassPhrase;

    @Value("${sftp.sftpUser}")
    private String sftpUser;

    @Value("${sftp.sftpFolder}")
    private String sftpFolder;

    @Value("${sftp.sftpFilesPattern}")
    private String sftpFilesPattern;

    private SSHClient createClientAndConnect() throws IOException {
        SSHClient client = new SSHClient();
        KeyProvider keys = client.loadKeys(sftpPrivateKey, null, PasswordUtils.createOneOff(sftpPassPhrase.toCharArray()));
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(sftpHostname, sftpPort);
        client.authPublickey(sftpUser, keys);
        return client;
    }

    public List<RemoteResourceInfo> listFile() throws IOException {
        SSHClient sshClient = createClientAndConnect();
        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            return sftpClient.ls(sftpFolder, new SftpAcquirerFileResourceFilter(sftpFilesPattern));
        } finally {
            sshClient.disconnect();
        }
    }

    public void downloadFile(String fileInput, String fileOutput) throws IOException {
        SSHClient sshClient = createClientAndConnect();
        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            sftpClient.get(fileInput, fileOutput);
        } finally {
            sshClient.disconnect();
        }
    }
}
