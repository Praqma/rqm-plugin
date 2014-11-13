/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model.exception;

import java.io.IOException;
import net.praqma.util.structure.Tuple;

/**
 *
 * @author Praqma
 */
public class RequestException extends IOException {
    public final Tuple<Integer,String> result;
    
    public RequestException(Tuple<Integer,String> result) {
        super(String.format("Request failed with return code %s", result.t1));
        this.result = result;
    }
    
    public RequestException(String message, Tuple<Integer,String> result) {
        super((result != null && result.t1 != null) ? String.format("%s Reason: Request failed with return code %s", message, result.t1) : String.format("%s", message));
        this.result = result;
    }
    
    public RequestException(Tuple<Integer,String> result, Throwable cause) {
        super( String.format("Request failed with return code %s", result.t1), cause);
        this.result = result;
    }
    
}
