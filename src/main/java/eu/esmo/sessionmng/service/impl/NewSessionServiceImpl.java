/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.esmo.sessionmng.pojo.DataStoreObject;
import eu.esmo.sessionmng.service.NewSessionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 *
 * @author nikos
 */
@Service
@Slf4j
public class NewSessionServiceImpl implements NewSessionService {

    private CacheManager cm;
    private final static String SESSION_CACHE_NAME = "sessionCache";

    @Autowired
    public NewSessionServiceImpl(CacheManager cacheManager) {
        this.cm = cacheManager;
    }

    @Override
    public void makeNewSession(String sessionId) {
        Map<String, DataStoreObject> session = new ConcurrentHashMap<>();
        cm.getCache(SESSION_CACHE_NAME).evict(sessionId);
        cm.getCache(SESSION_CACHE_NAME).put(sessionId, session);
    }

    @Override
    public String add(String sessionId, String objectId, String type, String object) {
        if (readFromCache(sessionId).isPresent()) {
            Map<String, DataStoreObject> session = readFromCache(sessionId).get();
            DataStoreObject storedObject = session.get(objectId);
            if (storedObject != null) {
                log.info("Object was found with objectId" + objectId + "for session" + sessionId);
            }
            session.put(objectId, new DataStoreObject(objectId, type, object));
            this.cm.getCache(SESSION_CACHE_NAME).evict(sessionId);
            this.cm.getCache(SESSION_CACHE_NAME).put(sessionId, session);
            return "OK";
        } else {
            Map<String, DataStoreObject> session = new ConcurrentHashMap<>();
            session.put(objectId, new DataStoreObject(objectId, type, object));
            this.cm.getCache(SESSION_CACHE_NAME).put(sessionId, session);

            return "OK";

        }
//        return "ERROR";
    }

    @Override
    public String get(String sessionId, String id) {
        Optional<Map<String, DataStoreObject>> session = readFromCache(sessionId);
        if (session.isPresent()) {
            if (session.get().get(id) != null) {
                ObjectMapper mapper = new ObjectMapper();
                DataStoreObject obj = session.get().get(id);
                try {
                    return mapper.writeValueAsString(obj);
                } catch (JsonProcessingException ex) {
                    log.error(ex.getMessage());
                }
            }
        }
        return "ERROR";
    }

    @Override
    public List<DataStoreObject> get(String sessionId) {
        Optional<Map<String, DataStoreObject>> session = readFromCache(sessionId);
        if (session.isPresent()) {
            return session.get().entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

        }
        return null;
    }

    @Override
    public String delete(String sessionId, String id) {
        Optional<Map<String, DataStoreObject>> session = readFromCache(sessionId);
        if (session.isPresent()) {
            if (id == null) {
                this.cm.getCache(SESSION_CACHE_NAME).evict(sessionId);
                this.cm.getCache(SESSION_CACHE_NAME).put(sessionId, new HashMap<String, DataStoreObject>());
            } else {
                session.get().remove(id);
                this.cm.getCache(SESSION_CACHE_NAME).evict(sessionId);
                this.cm.getCache(SESSION_CACHE_NAME).put(sessionId, session.get());
            }

            return "OK";
        }
        return "ERROR";
    }

    @Override
    public List<DataStoreObject> search(String sessionId, String type) {
        Optional<Map<String, DataStoreObject>> session = readFromCache(sessionId);
        if (session.isPresent()) {
            return session.get().entrySet()
                    .stream()
                    .filter(e -> e.getValue().getType().equals(type))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
        }
        return null;
    }

    private Optional<Map<String, DataStoreObject>> readFromCache(String key) {
        if (this.cm.getCache(SESSION_CACHE_NAME).get(key) == null) {
            return Optional.empty();
        }
        return Optional.of((Map<String, DataStoreObject>) this.cm.getCache(SESSION_CACHE_NAME).get(key).get());
    }

    @Override
    public boolean checkSession(String sessionId) {
        return readFromCache(sessionId).isPresent();
    }

}
