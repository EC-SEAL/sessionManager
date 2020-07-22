/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.esmo.sessionmng.enums.ResponseCode;
import eu.esmo.sessionmng.model.TO.MngrSessionTO;
import eu.esmo.sessionmng.pojo.DataStoreObject;
import eu.esmo.sessionmng.pojo.NewUpdateDataRequest;
import eu.esmo.sessionmng.pojo.SessionMngrResponse;
import eu.esmo.sessionmng.service.NewSessionService;
import eu.esmo.sessionmng.service.SessionService;
import io.swagger.annotations.ApiOperation;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author nikos
 */
@RestController
@RequestMapping("sm/new")
@Slf4j
public class NewAPIRest {

    @Autowired
    NewSessionService sessionServ;

    @Autowired
    SessionService oldSessionServ;

    @RequestMapping(value = "/startSession", method = RequestMethod.POST, produces = "application/json", consumes = {"application/x-www-form-urlencoded"})
    @ResponseStatus(code = HttpStatus.OK)
    @ApiOperation(value = "Starts a new session, by setting the code to NEW and the identifier at sessionData.sessionId", response = SessionMngrResponse.class, code = 200)
    public @ResponseBody
    SessionMngrResponse startSession(@RequestParam(required = false) String sessionId) {
        if (sessionId == null) {
            UUID nsessionId = UUID.randomUUID();
            sessionId = nsessionId.toString();
        }
        if (oldSessionServ.findBySessionId(sessionId) == null) {
            oldSessionServ.makeNewSession(sessionId);

        }
        sessionServ.makeNewSession(sessionId);

        log.info("created new session with Id:: " + sessionId.toString());
        return new SessionMngrResponse(ResponseCode.NEW, new MngrSessionTO(sessionId.toString(), new HashMap()), null, null);
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseStatus(code = HttpStatus.OK)
    @ApiOperation(value = "adds the givent data, under the provided session and for the given id. Error if no sesion is found", response = SessionMngrResponse.class, code = 200)
    public @ResponseBody
    SessionMngrResponse add(@RequestBody(required = false) NewUpdateDataRequest updateRequest, HttpServletRequest req) {
        String response = sessionServ.add(updateRequest.getSessionId(), updateRequest.getId(), updateRequest.getType(), updateRequest.getData());
        if (response.equals("ERROR")) {
            return new SessionMngrResponse(ResponseCode.ERROR, null, null, "no session found for " + updateRequest.getSessionId());
        }
        return new SessionMngrResponse(ResponseCode.OK, null, null, null);
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value = "returns in the extraData  the object (JSON strigified) for the given session id and object id, or if not object id, the array of all objects for the given sessionID", response = SessionMngrResponse.class, code = 200)
    public @ResponseBody
    SessionMngrResponse getSessionData(@RequestParam String sessionId, @RequestParam(required = false) String id) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (StringUtils.isEmpty(id)) {
                List<DataStoreObject> requestedObj = sessionServ.get(sessionId);
                return new SessionMngrResponse(ResponseCode.OK, null, mapper.writeValueAsString(requestedObj), null);
            } else {
                String requestedObj = sessionServ.get(sessionId, id);
                if (requestedObj == null) {
                    return new SessionMngrResponse(ResponseCode.ERROR, null, null, "no session found for " + sessionId + " or objectId " + id);
                }
                return new SessionMngrResponse(ResponseCode.OK, null, requestedObj, null);
            }
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
            return new SessionMngrResponse(ResponseCode.ERROR, null, null, "error marshalling the objects for session" + sessionId);

        }
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value = "deletes the given object (based on id) from the session", response = SessionMngrResponse.class, code = 200)
    public @ResponseBody
    SessionMngrResponse delete(@RequestBody(required = false) NewUpdateDataRequest deleteRequest) {
        String result = sessionServ.delete(deleteRequest.getSessionId(), deleteRequest.getId());
        if (!result.equals("ERROR")) {
            return new SessionMngrResponse(ResponseCode.OK, null, null, null);
        }
        return new SessionMngrResponse(ResponseCode.ERROR, null, null, "no session found for " + deleteRequest.getSessionId());
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value = "returns in the extraData field the array of JSON objects matching the given type, or if no type is given all session objects", response = SessionMngrResponse.class, code = 200)
    public @ResponseBody
    SessionMngrResponse searchData(@RequestParam String sessionId, @RequestParam(required = false) String type) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (StringUtils.isEmpty(type)) {
                List<DataStoreObject> requestedObj = sessionServ.get(sessionId);
                return new SessionMngrResponse(ResponseCode.OK, null, mapper.writeValueAsString(requestedObj), null);
            } else {
                List<DataStoreObject> requestedObj = sessionServ.search(sessionId, type);
                if (requestedObj == null) {
                    return new SessionMngrResponse(ResponseCode.ERROR, null, null, "no session found for " + sessionId);
                }
                return new SessionMngrResponse(ResponseCode.OK, null, mapper.writeValueAsString(requestedObj), null);
            }
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
            return new SessionMngrResponse(ResponseCode.ERROR, null, null, "error marshalling the objects for session" + sessionId);

        }
    }

}
