/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.esmo.sessionmng.enums.TypeEnum;
import eu.esmo.sessionmng.model.TO.MngrSessionTO;
import eu.esmo.sessionmng.model.dao.SessionRepository;
import eu.esmo.sessionmng.model.dmo.MngrSession;
import eu.esmo.sessionmng.pojo.AttributeSet;
import eu.esmo.sessionmng.pojo.AttributeType;
import eu.esmo.sessionmng.pojo.RequestParameters;
import eu.esmo.sessionmng.pojo.SessionMngrResponse;
import eu.esmo.sessionmng.pojo.UpdateDataRequest;
import eu.esmo.sessionmng.service.HttpSignatureService;
import eu.esmo.sessionmng.service.SessionService;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author nikos
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TestRestControllers {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private HttpSignatureService sigServ;

    @MockBean
    private SessionService sessionServ;

    @MockBean
    private SessionRepository sessionRep;

    @Test
    public void testGenerateTokenWithPayload() throws Exception {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        MvcResult result = mvc.perform(post("/sm/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/startSession", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded"))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        RequestParameters update = new RequestParameters("data", "id", sessionId, "type");
        String updateString = mapper.writeValueAsString(update);
        digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes());

        result = mvc.perform(post("/sm/generateTokenWithPayload?sessionId=" + sessionId + "&sender=sender&receiver=receiver")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/generateTokenWithPayload?sessionId=" + sessionId + "&sender=sender&receiver=receiver", update, "application/json", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

    }

    @Test
    public void testGetSessionDataNoVariableName() throws Exception {
        Map sessionVars = new HashMap();
        sessionVars.put("var1", "val1");
        sessionVars.put("var2", "val2");
        MngrSessionTO session = new MngrSessionTO("somesession", sessionVars);
        when(sessionServ.findBySessionId("somesession")).thenReturn(session);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();
        mvc.perform(get("/sm/getSessionData?sessionId=somesession")
                .header("authorization", sigServ.generateSignature("hostUrl", "GET", "/sm/getSessionData?sessionId=somesession", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "GET /getSessionData?sessionId=somesession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.sessionData.sessionId", is("somesession")))
                .andExpect(jsonPath("$.sessionData.sessionVariables.var1", is("val1")));
    }

    @Test
    public void testGetSessionDataWithVariableName() throws Exception {
        when(sessionServ.getValueByVariableAndId("somesession", "var1")).thenReturn("val1");

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        mvc.perform(get("/sm/getSessionData?sessionId=somesession&variableName=var1")
                .header("authorization", sigServ.generateSignature("hostUrl", "GET", "/sm/getSessionData?sessionId=somesession&variableName=var1", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "GET /getSessionData?sessionId=somesession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.sessionData.sessionId", is("somesession")))
                .andExpect(jsonPath("$.sessionData.sessionVariables.var1", is("val1")))
                .andExpect(jsonPath("$.sessionData.sessionVariables.var2").doesNotExist());
    }

    @Test
    public void testGetSessionDataWithNonExistingVariable() throws Exception {
        when(sessionServ.getValueByVariableAndId("somesession", "var1")).thenReturn(null);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        mvc.perform(get("/sm/getSessionData?sessionId=somesession&variableName=var1")
                .header("authorization", sigServ.generateSignature("hostUrl", "GET", "/sm/getSessionData?sessionId=somesession&variableName=var1", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "GET /getSessionData?sessionId=somesession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.sessionData.sessionId", is("somesession")))
                .andExpect(jsonPath("$.sessionData.sessionVariables.var1", nullValue()))
                .andExpect(jsonPath("$.sessionData.sessionVariables.var2").doesNotExist());
    }

    @Test
    public void testUpdateSessionDataExistingSession() throws Exception {
        MngrSession existingSession = new MngrSession("sessionId", new HashSet(), LocalDateTime.now());
        when(sessionRep.findBySessionId("sessionId")).thenReturn(existingSession);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        String requestId = UUID.randomUUID().toString();

        Map<String, String> postParams = new HashMap();
        postParams.put("sessionId", "sessionId");
        postParams.put("variableName", "var1");
        postParams.put("dataObject", "dataObject");

        AttributeType attributes = new AttributeType("name", "CurrentGivenName", "UTF-8", "en EN", false, new String[]{"NIKOS"});
        AttributeSet attrSet = new AttributeSet("uuid", TypeEnum.Request, "issuer", "recipient", new AttributeType[]{attributes}, null);

        ObjectMapper mapper = new ObjectMapper();
        String attrSetString = mapper.writeValueAsString(attrSet);

        UpdateDataRequest updateReq = new UpdateDataRequest("sessionId", "idpRequest", attrSetString);
//        UpdateDataRequest update = new UpdateDataRequest("sessionId", "var1", "dataObject");
//        ObjectMapper mapper = new ObjectMapper();
        String updateString = mapper.writeValueAsString(updateReq);

        byte[] digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        mvc.perform(post("/sm/updateSessionData")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/updateSessionData", updateReq, "application/json;charset=UTF-8", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /updateSessionData")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        //                        .param("sessionId", "sessionId")
        //                        .param("variableName", "var1")
        //                        .param("dataObject", "dataObject")
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("OK")));
    }

    private class UpdateMissingStuff {

        private String sessionId;
        private String dataObject;

        public UpdateMissingStuff() {
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getDataObject() {
            return dataObject;
        }

        public void setDataObject(String dataObject) {
            this.dataObject = dataObject;
        }
    }

    @Test
    public void testUpdateSessionDataExistingSessionNoVariableName() throws Exception {
        MngrSession existingSession = new MngrSession("sessionId", new HashSet(), LocalDateTime.now());
        when(sessionRep.findBySessionId("sessionId")).thenReturn(existingSession);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        String requestId = UUID.randomUUID().toString();

        UpdateMissingStuff update = new UpdateMissingStuff();
        update.setSessionId("sessionId");
        update.setDataObject("dataObject");
        String updateString = "{\"sessionId\":\"sessionId\",\"dataObject\":\"dataObject\"}";

        byte[] digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        mvc.perform(post("/sm/updateSessionData")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/updateSessionData", update, "application/json;charset=UTF-8", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /updateSessionData")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("OK")));
    }

    @Test
    public void testUpdateSessionDataNOTExistingSession() throws Exception {
//        when(sessionServ.updateSessionVariable("somesession",Mockito.any(String.class),Mockito.any(String.class))); //.thenThrow(new ChangeSetPersister.NotFoundException());

        doThrow(new ChangeSetPersister.NotFoundException()).when(sessionServ).updateSessionVariable("somesession", "var1", "dataObject");

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        String requestId = UUID.randomUUID().toString();

        UpdateDataRequest update = new UpdateDataRequest("somesession", "var1", "dataObject");
        ObjectMapper mapper = new ObjectMapper();
        String updateString = mapper.writeValueAsString(update);

        byte[] digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        mvc.perform(post("/sm/updateSessionData")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/updateSessionData", update, "application/json", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /updateSessionData")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("ERROR")))
                .andExpect(jsonPath("$.error", is("failed to update variable var1 NOT Found")));
    }

    @Test
    public void testGetSessionFromIdPUUUID() throws Exception {

        java.util.Optional<String> sessionIdOpt = java.util.Optional.of("sessionId");
        when(sessionServ.getSessionIdByVariableAndValue("varName", "varValue")).thenReturn(sessionIdOpt);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();
        mvc.perform(get("/sm/getSession?varName=varName&varValue=varValue")
                .header("authorization", sigServ.generateSignature("hostUrl", "GET", "/sm/getSession?varName=varName&varValue=varValue", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "GET /getSessionData?sessionId=somesession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionData.sessionId", is("sessionId")));

    }

    @Test
    public void testGetSessionFromIdPUUUIDSessionNotFound() throws Exception {

        java.util.Optional<String> sessionIdOpt = java.util.Optional.empty();
        when(sessionServ.getSessionIdByVariableAndValue("varName", "varValue")).thenReturn(sessionIdOpt);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();
        mvc.perform(get("/sm/getSession?varName=varName&varValue=varValue")
                .header("authorization", sigServ.generateSignature("hostUrl", "GET", "/sm/getSession?varName=varName&varValue=varValue", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "GET /getSessionData?sessionId=somesession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("ERROR")))
                .andExpect(jsonPath("$.error", is("No sessions found")));

    }

    @Test
    public void testGetSessionFromIdPUUUIDSessionManySessionsMatching() throws Exception {

        doThrow(new ArithmeticException()).when(sessionServ).getSessionIdByVariableAndValue("varName", "varValue");

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();
        mvc.perform(get("/sm/getSession?varName=varName&varValue=varValue")
                .header("authorization", sigServ.generateSignature("hostUrl", "GET", "/sm/getSession?varName=varName&varValue=varValue", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "GET /getSessionData?sessionId=somesession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("ERROR")))
                .andExpect(jsonPath("$.error", is("More than one sessions match criteria!")));

    }

    @Test
    public void startSession() throws Exception {

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("sessionId=sessionId".getBytes());
        String requestId = UUID.randomUUID().toString();

        Map<String, String> postParams = new HashMap();
        postParams.put("sessionId", "sessionId");

        mvc.perform(post("/sm/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/startSession", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /startSession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("sessionId=sessionId".getBytes())
        )
                .andExpect(status().isCreated())
                .andReturn();
    }

    @Test
    public void testUpdateWholeSessionDataExistingSessionNoSigRequired_FakeSM() throws Exception {
        MngrSession existingSession = new MngrSession("sessionId", new HashSet(), LocalDateTime.now());
        when(sessionRep.findBySessionId("sessionId")).thenReturn(existingSession);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        String requestId = UUID.randomUUID().toString();

        Map<String, String> postParams = new HashMap();
        postParams.put("sessionId", "sessionId");
        postParams.put("dataObject", "dataObject");

        AttributeType attributes = new AttributeType("name", "CurrentGivenName", "UTF-8", "en EN", false, new String[]{"NIKOS"});
        AttributeSet attrSet = new AttributeSet("uuid", TypeEnum.Request, "issuer", "recipient", new AttributeType[]{attributes}, null);

        ObjectMapper mapper = new ObjectMapper();
        String attrSetString = mapper.writeValueAsString(attrSet);

        UpdateDataRequest updateReq = new UpdateDataRequest("sessionId", "idpRequest", attrSetString);

        String updateString = mapper.writeValueAsString(updateReq);

        byte[] digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        mvc.perform(post("/fakeSm/updateSessionData")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/updateSessionData", updateReq, "application/json;charset=UTF-8", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /updateSessionData")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("OK")));
    }

}
