/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model.exception;

import net.praqma.util.structure.Tuple;

/**
 *
 * @author Praqma
 */
public class RequestException extends Exception {
    public final Tuple<Integer,String> result;
    
    public RequestException(Tuple<Integer,String> result, Throwable cause) {
        super(String.format("Request failed with return code %s",result.t1), cause);
        this.result = result;
    }
}
