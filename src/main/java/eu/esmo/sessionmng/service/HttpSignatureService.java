/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.service;

import eu.esmo.sessionmng.enums.HttpResponseEnum;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author nikos
 */
public interface HttpSignatureService {

    public String getFakeSignature() throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, UnsupportedEncodingException, IOException;

    public HttpResponseEnum verifySignature(HttpServletRequest httpRequest, MSConfigurationService confServ) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, UnsupportedEncodingException, IOException;

    public String generateSignature(String hostUrl, String method, String uri, Object postParams, String contentType, String requestId)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, UnsupportedEncodingException, IOException, UnsupportedEncodingException;

}
