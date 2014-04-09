/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model.exception;

/**
 *
 * @author Praqma
 */
public class RQMObjectParseException extends Exception {
    public RQMObjectParseException(String msg, Throwable cause) {
        super(msg,cause);
    }
}
