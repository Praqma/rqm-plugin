/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model.exception;

/**
 *
 * @author Praqma
 */
public class LoginException extends Exception {
    public final String user;
    public final String password;
    
    public LoginException(String user, String password, Throwable cause) {
        super(String.format("Login failed for user %s using password %s", user, password), cause);
        this.user = user;
        this.password = password;
    }
}
