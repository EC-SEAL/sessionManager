/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.service.impl;

import eu.esmo.sessionmng.factory.MSConfigurationResponseFactory;
import eu.esmo.sessionmng.service.MSConfigurationService;
import eu.esmo.sessionmng.pojo.MSConfigurationResponse;
import eu.esmo.sessionmng.pojo.MSConfigurationResponse.MicroService;
import eu.esmo.sessionmng.service.ParameterService;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 *
 * @author nikos
 */
@Service
@Profile("test")
public class MSConfigurationsServiceImplSTUB implements MSConfigurationService {

    private final static Logger log = LoggerFactory.getLogger(MSConfigurationsServiceImplSTUB.class);

    private ParameterService paramServ;

    @Autowired
    public MSConfigurationsServiceImplSTUB(ParameterService paramServ) {
        this.paramServ = paramServ;
    }

    @Override
    public MicroService[] getConfigurationJSON() {

        try {
            String configPath = StringUtils.isEmpty(paramServ.getProperty("CONFIG_JSON")) ? "configurationResponse.json" : paramServ.getProperty("CONFIG_JSON");
            return MSConfigurationResponseFactory.makeMSConfigResponseFromJSON(getFile(configPath));
        } catch (IOException e) {
            log.error("file not found ", e);
            return null;
        }

    }

    private String getFile(String fileName) {
        StringBuilder result = new StringBuilder("");
        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file;
        if (StringUtils.isEmpty(paramServ.getProperty("CONFIG_JSON"))) {
            file = new File(classLoader.getResource(fileName).getFile());
        } else {
            file = new File(paramServ.getProperty("CONFIG_JSON"));
        }

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();

    }

    @Override
    public Set<String> getMsIDfromRSAFingerprint(String rsaFingerPrint) throws IOException, NoSuchAlgorithmException {
        String configPath = StringUtils.isEmpty(paramServ.getProperty("CONFIG_JSON")) ? "configurationResponse.json" : paramServ.getProperty("CONFIG_JSON");
        MicroService[] configResp = MSConfigurationResponseFactory.makeMSConfigResponseFromJSON(getFile(configPath));
        Set<String> matches = new HashSet<>();
        Arrays.stream(configResp).filter(msConfig -> {
            try {
                return DigestUtils.sha256Hex(getPublicKey(msConfig.getRsaPublicKeyBinary()).getEncoded()).equals(rsaFingerPrint);
            } catch (Exception e) {
                log.error("error parsing public key! " + msConfig.getRsaPublicKeyBinary());
                log.error(e.getLocalizedMessage());
                return false;
            }
        }).forEach(ms -> {
            matches.add(ms.getMsId());
        });

        return matches;
    }

    @Override
    public Optional<PublicKey> getPublicKeyFromFingerPrint(String rsaFingerPrint) throws InvalidKeyException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        MSConfigurationResponse.MicroService[] config = getConfigurationJSON();

        if (config != null) {
            log.info("found metadata");
            Optional<MSConfigurationResponse.MicroService> msMatch = Arrays.stream(getConfigurationJSON()).filter(msConfig -> {
                try {
                    byte[] encodedBytes = getPublicKey(msConfig.getRsaPublicKeyBinary()).getEncoded();
//                    System.out.println(
//                            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCi7jZVwQFxQ2SY4lxjr05IexolQJJobwYzrvE5pk7AcQpG46kuJBzD8ziiqFFCGSNZ07cLWys+b5JmJ6kU44lKLVeGbEpgaO0OTBDLMk2fi5U83T8dezgWgaPFiy/N3sHPpcW2Y3ZePo0UbM7MLzv14TR+jxTOyrmwWwGwDJsz+wIDAQAB"
//                            .equals(msConfig.getRsaPublicKeyBinary()));
//                    
//                    System.out.println(msConfig.getRsaPublicKeyBinary());
                    return DigestUtils.sha256Hex(encodedBytes).equals(rsaFingerPrint);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    log.error("error parsing msconfig public keys ");
                    log.error(e.getMessage());
                    return false;
                }
            }).findFirst();
            if (msMatch.isPresent()) {
                return Optional.of(getPublicKey(msMatch.get().getRsaPublicKeyBinary()));
            }
        } else {
            log.error("error connecting to configMngr " + paramServ.getProperty("CONFIGURATION_MANAGER_URL") + "/metadata/microservices");
        }
        return Optional.empty();
    }

    static byte[] getBinarySha256Fingerprint(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public RSAPublicKey getPublicKey(String keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicBytes = Base64.getDecoder().decode(keyBytes);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);

    }

}
