/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model;
import hudson.model.BuildListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.praqma.jenkins.rqm.model.exception.RQMObjectParseException;
import net.praqma.jenkins.rqm.request.RqmParameterList;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Attempt at making the RQM object a bit more human readable. 
 * @author Praqma
 */
public abstract class RqmObject<T> implements Serializable {

    private final static Logger log = Logger.getLogger(RqmObject.class.getName());
    protected String rqmObjectResourceUrl;
   
    public RqmObject() { }
    
    public abstract String getResourceName(); 
    
    public static Document getDocumentReader(String xml) throws RQMObjectParseException {
        Document doc = null;
        InputStream is = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();       
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            is = new BufferedInputStream(new ByteArrayInputStream(xml.getBytes(Charset.forName("utf8"))));
            doc = builder.parse(is);
            doc.normalize();
            is.close();
        } catch (IOException ex) {            
            log.logp(Level.SEVERE, RqmObject.class.getName(), "getDocumentReader()", "Error in getDocumentReader", ex);
            throw new RQMObjectParseException(String.format( "Failed to parse the following xml:%n%s",xml), ex);
        } catch (ParserConfigurationException ex) {
            log.logp(Level.SEVERE, RqmObject.class.getName(), "getDocumentReader()", "Error in getDocumentReader", ex);
            throw new RQMObjectParseException(String.format( "Failed to parse the following xml:%n%s",xml), ex);
        } catch (SAXException ex) {
            log.logp(Level.SEVERE, RqmObject.class.getName(), "getDocumentReader()", "Error in getDocumentReader", ex);
            throw new RQMObjectParseException(String.format( "Failed to parse the following xml:%n%s",xml), ex);
        } finally {
            if(is != null) {
                try {                   
                    is.close();
                } catch (IOException ex) {
                    //Ignore
                    log.log(Level.SEVERE, "Failed to close input stream", ex);
                }
            }
        } 
        return doc;
    }
    /**
     * Factory method for the xml document reader
     * @param is
     * @return a readable XML Document
     * @throws Exception 
     */
    public static Document getDocumentReader(InputStream is) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();       
        DocumentBuilder builder = dbFactory.newDocumentBuilder();
        Document doc = builder.parse(new BufferedInputStream(is));
        doc.normalize();
        is.close();
        return doc;
    }
    
    
    /**
     * Expects an internal resource id.
     * @param xml
     * @return
     * @throws RQMObjectParseException 
     */
    public abstract T initializeSingleResource(String xml) throws RQMObjectParseException;
    public T intializeSingleResource(InputStream is) throws RQMObjectParseException {
        BufferedReader bis = null;
        try {
            bis  = new BufferedReader(new InputStreamReader(is,"UTF-8"));            
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = bis.readLine()) != null) {               
                sb.append(line);
            }            
            return initializeSingleResource(sb.toString());
            
        } catch (IOException ioex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "intializeSingleResource()", "Failed to stream", ioex);
            throw new RQMObjectParseException(String.format("Failed to parse %s", this.getClass().getSimpleName()), ioex);
        } finally {
            if(bis != null) {
                try {
                    bis.close();
                } catch (IOException ex) {
                    throw new RQMObjectParseException(String.format("Unable to close stream %s", this.getClass().getSimpleName()), ex);
                }
            }
        }       
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RqmObject) {
            RqmObject other = (RqmObject)obj;  
            if(other.getRqmObjectResourceUrl() == null || other.getRqmObjectResourceUrl().equals("")) {
                return false;
            }
            
            if(this.getRqmObjectResourceUrl() == null || this.getRqmObjectResourceUrl().equals("")){
                return false;
            }            
            return this.getRqmObjectResourceUrl().equals(other.getRqmObjectResourceUrl());
            
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.getRqmObjectResourceUrl().hashCode();
    }
    
    public static String getDescriptor(RqmObject obj) {
        try {
            return String.format("[%s] %s%n", obj.getClass().getSimpleName(), obj.getRqmObjectResourceUrl().split("urn:")[1]);
        } catch (Exception ex) {
            log.fine( String.format("The descriptor does not contain an internal url reference%n The resource url is: %s",obj.getRqmObjectResourceUrl()) );
            return String.format("[%s] %s%n", obj.getClass().getSimpleName(), obj.getRqmObjectResourceUrl());
        }
    }
    
    public static String getDescriptor(RqmObject obj, String title) {
        try {
            return String.format("[%s] %s (%s)%n", obj.getClass().getSimpleName(), title, obj.getRqmObjectResourceUrl().split("urn:")[1]);
        } catch (Exception ex) {
            log.fine( String.format("The descriptor does not contain an internal url reference%n The resource url is: %s",obj.getRqmObjectResourceUrl()) );
            return String.format("[%s] %s%n", obj.getClass().getSimpleName(), title);
        }
    }

    /**
     * @return the rqmObjectResourceUrl
     */
    public String getRqmObjectResourceUrl() {
        return rqmObjectResourceUrl;
    }

    /**
     * @param rqmObjectResourceUrl the rqmObjectResourceUrl to set
     */
    public void setRqmObjectResourceUrl(String rqmObjectResourceUrl) {
        this.rqmObjectResourceUrl = rqmObjectResourceUrl;
    }
    
    public int getInternalId() throws RQMObjectParseException{
        try {
            int beginIndex = getRqmObjectResourceUrl().lastIndexOf(":");
            int length = getRqmObjectResourceUrl().length();
            return Integer.parseInt(getRqmObjectResourceUrl().substring(beginIndex+1, length));
        } catch (NumberFormatException ex) {
            throw new RQMObjectParseException(String.format("Failed to extract internal id from resource url: %s",getRqmObjectResourceUrl()), ex);
        }
    }
    
    /**
     * Override this method if you wish to read data from RQM (Primarily GET) 
     * 
     * @param parameters
     * @return a 'constructed' rqm object. Read populates the object given an array of parameters
     * @throws IOException 
     */
    public List<T> read(RqmParameterList parameters, BuildListener listener) throws IOException { return null; }
    public List<T> readMultiple( RqmParameterList parameters, BuildListener listener ) throws IOException {
        return read(parameters, listener);
    }
    
    /**
     * Override this method if you wish to push data from RQM (PUT && POST) 
     * 
     * @param parameters
     * @return Create or updates the object given an array of parameters. Use the initializeSingleResource() method to parse the object given the returned XML
     * @throws IOException 
     */
    public List<T> createOrUpdate(RqmParameterList parameters, BuildListener listener) throws IOException { return null; } 
    
    public HashMap<String,String> attributes() {
        return new HashMap<String, String>();
    }
    
    public static <T extends RqmObject> T createObject(Class<T> clazz, RqmParameterList parameters, BuildListener listener) throws InstantiationException, IllegalAccessException, IOException {
        T t = clazz.newInstance();
        t.read(parameters, listener);
        return t;
    }
}
