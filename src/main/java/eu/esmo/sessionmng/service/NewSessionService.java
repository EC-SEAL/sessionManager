/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.service;

import eu.esmo.sessionmng.pojo.DataStoreObject;
import java.util.List;

/**
 *
 * @author nikos
 */
public interface NewSessionService {

    public void makeNewSession(String sessionId);

    public String add(String sessionId, String id, String type, String object);

    public String get(String sessionId, String id);

    public List<DataStoreObject> get(String sessionId);

    public String delete(String sessionId, String id);

    public List<DataStoreObject> search(String sessionId, String type);

    public boolean checkSession(String sessionId);
}
