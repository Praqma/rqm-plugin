/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.request;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import java.io.Serializable;
import java.util.Arrays;
import net.praqma.jenkins.rqm.model.exception.ClientCreationException;
import org.apache.commons.httpclient.NameValuePair;

/**
 *
 * @author Praqma
 */
public class RqmParameterList implements Serializable {
    public NameValuePair[] parameterList;
    public final String hostName;
    public final String contextRoot;
    public String requestString;
    public final String projectName;
 
    public int port;
    public String methodType;
    public String requestContent;
    private UsernamePasswordCredentials credentials;
    
    @Deprecated
    public String userName;
    @Deprecated
    public String passwd;
    
    @Deprecated
    public RqmParameterList(final String hostName, final int port, final String contextRoot, final String projectName, final String userName, final String passwd, final String requestString, final NameValuePair[] parameterList, final String methodType, final String requestContent) throws ClientCreationException {
        this.hostName = hostName;
        this.port = port;
        this.projectName = projectName;
        this.userName = userName;
        this.passwd = passwd;
        this.contextRoot = contextRoot;
        this.requestString = requestString;
        this.parameterList = parameterList;       
        this.methodType = methodType;
        this.requestContent = requestContent;
    }
    
    public RqmParameterList(final String hostName, final int port, final String contextRoot, final String projectName, final UsernamePasswordCredentials credentials, final String requestString, final NameValuePair[] parameterList, final String methodType, final String requestContent) throws ClientCreationException {
        this.hostName = hostName;
        this.port = port;
        this.projectName = projectName;
        this.contextRoot = contextRoot;
        this.requestString = requestString;
        this.parameterList = parameterList;       
        this.methodType = methodType;
        this.requestContent = requestContent;
        this.credentials = credentials;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(fillParameter("HostName", hostName));
        builder.append(fillParameter("Port", port+""));
        builder.append(fillParameter("ProjectName", projectName));
        builder.append(fillParameter("UserName", userName));
        builder.append(fillParameter("Password", passwd));
        builder.append(fillParameter("Context", contextRoot));
        builder.append(fillParameter("RequestString", requestString));
        builder.append(fillParameter("ParameterList", Arrays.toString(parameterList)));
        builder.append(fillParameter("MethodType", methodType));
        builder.append(fillParameter("RequestContent", requestContent));
        return builder.toString();
    }
    
    private String fillParameter(String key, String value) {
        return String.format("%s : %s%n", key, value);
    }

    /**
     * @return the credentials
     */
    public UsernamePasswordCredentials getCredentials() {
        return credentials;
    }

    /**
     * @param credentials the credentials to set
     */
    public void setCredentials(UsernamePasswordCredentials credentials) {
        this.credentials = credentials;
    }
    
    
}
