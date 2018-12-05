/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.service.impl;

import eu.esmo.sessionmng.service.KeyStoreService;
import eu.esmo.sessionmng.service.ParameterService;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 *
 * @author nikos
 */
@Service
public class KeyStoreServiceImpl implements KeyStoreService {

    private final String certPath;
    private final String keyPass;
    private final String storePass;
    private final String keyAlias;

    private KeyStore keystore;

    private ParameterService paramServ;

    @Autowired
    public KeyStoreServiceImpl(ParameterService paramServ) throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        this.paramServ = paramServ;
        certPath = this.paramServ.getProperty("KEYSTORE_PATH");
        keyPass = this.paramServ.getProperty("KEY_PASS");
        storePass = this.paramServ.getProperty("STORE_PASS");
        keyAlias = this.paramServ.getProperty("CERT_ALIAS");

        keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        if (!StringUtils.isEmpty(paramServ.getProperty("ASYNC_SIGNATURE")) && Boolean.parseBoolean(paramServ.getProperty("ASYNC_SIGNATURE"))) {
            File jwtCertFile = new File(certPath);
            InputStream certIS = new FileInputStream(jwtCertFile);
            keystore.load(certIS, storePass.toCharArray());
        } else {
            //init an empty keystore otherwise an exception is thrown
            keystore.load(null, null);
        }

    }

    public Key getSigningKey() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, UnsupportedEncodingException {
        //"jwtkey"
        //return keystore.getKey(keyAlias, "keypassword".toCharArray());
        String asyncSignature = paramServ.getProperty("ASYNC_SIGNATURE");
        if (!StringUtils.isEmpty(asyncSignature) && Boolean.valueOf(asyncSignature)) {
            return keystore.getKey(keyAlias, keyPass.toCharArray());
        }
        String secretKey = paramServ.getProperty("SIGNING_SECRET");
        return new SecretKeySpec(secretKey.getBytes("UTF-8"), 0, secretKey.length(), "HmacSHA256");
    }

    public Key getPublicKey() throws KeyStoreException, UnsupportedEncodingException {
        //"jwtkey"
        String asyncSignature = paramServ.getProperty("ASYNC_SIGNATURE");
        if (!StringUtils.isEmpty(asyncSignature) && Boolean.valueOf(asyncSignature)) {
            Certificate cert = keystore.getCertificate(keyAlias);
            return cert.getPublicKey();
        }
        String secretKey = paramServ.getProperty("SIGNING_SECRET");
        return new SecretKeySpec(secretKey.getBytes("UTF-8"), 0, secretKey.length(), "HmacSHA256");
    }

    public KeyStore getKeystore() {
        return keystore;
    }

    public void setKeystore(KeyStore keystore) {
        this.keystore = keystore;
    }

    public ParameterService getParamServ() {
        return paramServ;
    }

    public void setParamServ(ParameterService paramServ) {
        this.paramServ = paramServ;
    }

    @Override
    public SignatureAlgorithm getAlgorithm() {
        if (!StringUtils.isEmpty(paramServ.getProperty("ASYNC_SIGNATURE")) && Boolean.parseBoolean(paramServ.getProperty("ASYNC_SIGNATURE"))) {
            return SignatureAlgorithm.RS256;
        }
        return SignatureAlgorithm.HS256;
    }

}