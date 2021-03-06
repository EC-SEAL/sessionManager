/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.service;

import eu.esmo.sessionmng.pojo.MSConfigurationResponse;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author nikos
 */
public interface MSConfigurationService {
    

    public MSConfigurationResponse.MicroService[] getConfigurationJSON();

    public Set<String> getMsIDfromRSAFingerprint(String rsaFingerPrint) throws IOException, NoSuchAlgorithmException;

    public Optional<PublicKey> getPublicKeyFromFingerPrint(String rsaFingerPrint) throws InvalidKeyException, IOException, NoSuchAlgorithmException, InvalidKeySpecException;

}
