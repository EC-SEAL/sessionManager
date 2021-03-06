/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.service.impl;

/**
 *
 * @author nikos
 */
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.esmo.sessionmng.factory.MngrSessionFactory;
import eu.esmo.sessionmng.model.TO.MngrSessionTO;
import eu.esmo.sessionmng.model.dao.SessionRepository;
import eu.esmo.sessionmng.model.dmo.MngrSession;
import eu.esmo.sessionmng.model.dmo.SessionVariable;
import eu.esmo.sessionmng.service.SessionService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import realAES.realAES;

@Transactional
@Service
public class SessionServiceImpl implements SessionService {

//    @Autowired
    private final SessionRepository sessionRepo;
    private realAES aesEncrypt;
    private String decryptedKey;

    private final static Logger LOG = LoggerFactory.getLogger(SessionServiceImpl.class);

    @Autowired
    private Environment env;

    @Autowired
    public SessionServiceImpl(SessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
        this.aesEncrypt = new realAES();
        try {
            String ecryptedKey = env.getProperty("encrypted.key");
            decryptedKey = aesEncrypt.aesDecrypt(ecryptedKey, "SieBcx3RlfgJ3b5e5SkZTrHPkKDFEfYSsJ/N1UbCtFU=");
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
        }

    }

    @Override
    public List<MngrSession> findAll() {

        return sessionRepo.findAll();
    }

    @Override
    public MngrSessionTO findBySessionId(String sessionId) {
        return MngrSessionFactory.getMngrSessionTO(sessionRepo.findBySessionId(sessionId));
    }

    @Override
    public String getValueByVariableAndId(String sessionId, String variable) {
        return sessionRepo.getValueByVariableAndId(sessionId, variable);
    }

    @Override
    public void save(MngrSession session) {
        sessionRepo.save(session);
    }

    //TODO
    /*
        Maybe this can be done more easily with sql?
     */
    @Override
    public void updateSessionVariable(String sessionId, String variableName, String newValue) throws ChangeSetPersister.NotFoundException {
        MngrSession existingSession = sessionRepo.findBySessionId(sessionId);
        if (existingSession != null) {
            ArrayList<SessionVariable> variables = new ArrayList();
            variables.addAll(existingSession.getVariable());
            Optional<SessionVariable> matchVariable = variables.stream().
                    filter(v -> {
                        return v.getName().equals(variableName);
                    })
                    .findFirst();
            SessionVariable varToUpdate;
            if (!matchVariable.isPresent()) {
                varToUpdate = new SessionVariable(variableName, newValue);
            } else {
                varToUpdate = matchVariable.get();
                existingSession.getVariable().remove(varToUpdate);
            }

            varToUpdate.setValue(newValue);
            existingSession.getVariable().add(varToUpdate);
            sessionRepo.save(existingSession);
        } else {
            throw new ChangeSetPersister.NotFoundException();
        }

    }

    @Override
    @Transactional
    public void makeNewSession(String sessionId) {
        MngrSession session = new MngrSession(sessionId, new HashSet(), LocalDateTime.now());
        this.save(session);
    }

    @Override
    @Transactional
    public void delete(MngrSession session) {
        this.sessionRepo.deleteBySessionId(session.getSessionId());
    }

    @Override
    @Transactional
    public void delete(String sessionId) {
        this.sessionRepo.deleteBySessionId(sessionId);
    }

    @Override
    @Transactional
    public Optional<String> getSessionIdByVariableAndValue(String variableName, String value) {
        Optional<List<String>> result = this.sessionRepo.getSessionIdByVariableAndValue(variableName, value);
        if (result.isPresent()) {
            if (result.get().size() != 1) {
                throw new ArithmeticException("More than one sessions match criteria!");
            }
            return Optional.of(result.get().iterator().next());
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public void replaceSession(String sessionId, String newValuesMap) throws ChangeSetPersister.NotFoundException, IOException {
        if (sessionRepo.findBySessionId(sessionId) != null) {
            this.sessionRepo.deleteBySessionId(sessionId);
            MngrSession session = new MngrSession(sessionId, new HashSet(), LocalDateTime.now());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> map = mapper.readValue(newValuesMap, Map.class);

            map.entrySet().stream().forEach(entry -> {
                SessionVariable newVariable;
                try {
                    if (entry.getValue() instanceof String) {
                        newVariable = new SessionVariable(entry.getKey(), entry.getValue());
                    } else {
                        newVariable = new SessionVariable(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
                    }
                    session.getVariable().add(newVariable);
                } catch (JsonProcessingException ex) {
                    LOG.error(ex.getMessage());
                }
            });

            this.sessionRepo.save(session);
        } else {
            throw new ChangeSetPersister.NotFoundException();
        }

    }

    private String decryptData(String encryptedData) {
        return this.aesEncrypt.aesDecrypt(encryptedData, decryptedKey);
    }

    private String encryptData(String plaintextData) {
        return this.aesEncrypt.aesEncrypt(plaintextData, decryptedKey);
    }

    public MngrSessionTO encryptMngrSessionTO(MngrSessionTO session) {
        MngrSessionTO encrypted = new MngrSessionTO();
        encrypted.setSessionId(session.getSessionId());
        Map<String, String> encVariables = new HashMap<>();
        ((Map<String, String>) session.getSessionVariables()).entrySet()
                .stream().forEach(entry -> {
                    encVariables.put(encryptData(entry.getKey()), encryptData(entry.getValue()));
                });
        encrypted.setSessionVariables(encVariables);
        return encrypted;
    }

    public MngrSessionTO decryptMngrSessionTO(MngrSessionTO encrypted) {
        MngrSessionTO session = new MngrSessionTO();
        session.setSessionId(decryptData(encrypted.getSessionId()));
        Map<String, String> decVariables = new HashMap();
        ((Map<String, String>) encrypted.getSessionVariables()).entrySet().forEach(entry -> {
            decVariables.put(decryptData(entry.getKey()), decryptData(entry.getValue()));
        });
        session.setSessionVariables(decVariables);
        return session;
    }

}
