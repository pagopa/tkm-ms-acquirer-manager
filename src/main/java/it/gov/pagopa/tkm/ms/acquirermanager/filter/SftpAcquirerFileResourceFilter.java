package it.gov.pagopa.tkm.ms.acquirermanager.filter;

import lombok.AllArgsConstructor;
import net.schmizz.sshj.sftp.RemoteResourceFilter;
import net.schmizz.sshj.sftp.RemoteResourceInfo;

import java.util.regex.Pattern;

@AllArgsConstructor
public class SftpAcquirerFileResourceFilter implements RemoteResourceFilter {
    private String pattern;

    @Override
    public boolean accept(RemoteResourceInfo resource) {
        Pattern p = Pattern.compile(pattern);
        return p.matcher(resource.getName()).matches();
    }
}
