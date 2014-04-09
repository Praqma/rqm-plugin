/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model.exception;

/**
 *
 * @author Praqma
 */
public class ClientCreationException extends Exception {
    public ClientCreationException(String msg, Throwable cause) {
        super(msg,cause);
    }
}
