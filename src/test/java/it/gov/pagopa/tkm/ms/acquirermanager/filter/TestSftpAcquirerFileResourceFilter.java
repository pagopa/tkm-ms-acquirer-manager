package it.gov.pagopa.tkm.ms.acquirermanager.filter;

import net.schmizz.sshj.sftp.PathComponents;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSftpAcquirerFileResourceFilter {
    private final String pattern = "^TKM\\..*\\.zip$";

    @Test
    void accept_true() {
        SftpAcquirerFileResourceFilter sftpAcquirerFileResourceFilter = new SftpAcquirerFileResourceFilter(pattern);
        RemoteResourceInfo path = new RemoteResourceInfo(new PathComponents("", "TKM.fileName.txt.zip", "path"), null);
        assertTrue(sftpAcquirerFileResourceFilter.accept(path));
    }

    @Test
    void accept_false() {
        SftpAcquirerFileResourceFilter sftpAcquirerFileResourceFilter = new SftpAcquirerFileResourceFilter(pattern);
        RemoteResourceInfo path = new RemoteResourceInfo(new PathComponents("", "TKM.fileName.txt", "path"), null);
        assertFalse(sftpAcquirerFileResourceFilter.accept(path));
    }
}