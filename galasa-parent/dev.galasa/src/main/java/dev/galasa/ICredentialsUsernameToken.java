/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa;

public interface ICredentialsUsernameToken extends ICredentialsUsername {

    byte[] getToken();

}
