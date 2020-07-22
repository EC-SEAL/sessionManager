/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.esmo.sessionmng.pojo.NewUpdateDataRequest;
import eu.esmo.sessionmng.pojo.SessionMngrResponse;
import eu.esmo.sessionmng.pojo.UpdateDataRequest;
import eu.esmo.sessionmng.service.HttpSignatureService;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
public class TestNewRestControllers {

    static {
        System.setProperty("MEMCACHED_PORT", "11211");
        System.setProperty("MEMCACHED_HOST", "localhost");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private HttpSignatureService sigServ;

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

        mvc.perform(post("/sm/new/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /startSession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("sessionId=sessionId".getBytes())
        )
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
    }

    @Test
    public void startSessionAndGet() throws Exception {

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("sessionId=sessionId".getBytes());
        String requestId = UUID.randomUUID().toString();

        Map<String, String> postParams = new HashMap();
        postParams.put("sessionId", "sessionId");

        MvcResult result = mvc.perform(post("/sm/new/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /startSession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("sessionId=sessionId".getBytes())
        )
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        date = new Date();
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        requestId = UUID.randomUUID().toString();
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/get", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/get")
                .header("authorization", authHeader)
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .param("sessionId", sessionId)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")));

    }


    /*

     */
    @Test
    public void addToSession() throws Exception {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        MvcResult result = mvc.perform(post("/sm/new/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        NewUpdateDataRequest update = new NewUpdateDataRequest(sessionId, "dataSet", "\"{\"the\":\"object\"}\"", "id");
        String updateString = mapper.writeValueAsString(update);
        digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        date = new Date();
        nowDate = formatter.format(date);
        mvc.perform(post("/sm/new/add")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/add", update, "application/json", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")));

        assertEquals(true, true);
    }

    @Test
    public void addToSessionAndGet() throws Exception {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        MvcResult result = mvc.perform(post("/sm/new/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        NewUpdateDataRequest update = new NewUpdateDataRequest(sessionId, "dataSet", "\"{\"the\":\"object\"}\"", "id");
        String updateString = mapper.writeValueAsString(update);
        digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        date = new Date();
        nowDate = formatter.format(date);
        mvc.perform(post("/sm/new/add")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/add", update, "application/json", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")));

        date = new Date();
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        requestId = UUID.randomUUID().toString();
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/get", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/get")
                .header("authorization", authHeader)
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .param("sessionId", sessionId)
                .param("id", "id")
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.additionalData", is("{\"id\":\"id\",\"type\":\"dataSet\",\"data\":\"\\\"{\\\"the\\\":\\\"object\\\"}\\\"\"}")));

    }

    @Test
    public void addToSessionAndDelete() throws Exception {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        MvcResult result = mvc.perform(post("/sm/new/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        NewUpdateDataRequest update = new NewUpdateDataRequest(sessionId, "dataSet", "\"{\"the\":\"object\"}\"", "id");
        String updateString = mapper.writeValueAsString(update);
        digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        date = new Date();
        nowDate = formatter.format(date);
        mvc.perform(post("/sm/new/add")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/add", update, "application/json", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")));

        update = new NewUpdateDataRequest(sessionId, null, null, "id");
        updateString = mapper.writeValueAsString(update);
        digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding
        date = new Date();
        nowDate = formatter.format(date);
        mvc.perform(post("/sm/new/delete")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/delete", update, "application/json", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")));

    }

    @Test
    public void searchSessionIdAndType() throws Exception {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        MvcResult result = mvc.perform(post("/sm/new/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        NewUpdateDataRequest update = new NewUpdateDataRequest(sessionId, "dataSet", "\"{\"the\":\"object\"}\"", "id");
        String updateString = mapper.writeValueAsString(update);
        digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        date = new Date();
        nowDate = formatter.format(date);
        mvc.perform(post("/sm/new/add")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/add", update, "application/json", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")));

        date = new Date();
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        requestId = UUID.randomUUID().toString();
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/search", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/search")
                .header("authorization", authHeader)
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .param("sessionId", sessionId)
                .param("id", "id")
                .param("type", "dataSet")
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.additionalData", is("[{\"id\":\"id\",\"type\":\"dataSet\",\"data\":\"\\\"{\\\"the\\\":\\\"object\\\"}\\\"\"}]")));
    }

    @Test
    public void searchSessionIdNoType() throws Exception {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        MvcResult result = mvc.perform(post("/sm/new/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        NewUpdateDataRequest update = new NewUpdateDataRequest(sessionId, "dataSet", "\"{\"the\":\"object\"}\"", "id");
        String updateString = mapper.writeValueAsString(update);
        digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        date = new Date();
        nowDate = formatter.format(date);
        mvc.perform(post("/sm/new/add")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/add", update, "application/json", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")));

        date = new Date();
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        requestId = UUID.randomUUID().toString();
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/search", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/search")
                .header("authorization", authHeader)
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .param("sessionId", sessionId)
                .param("id", "id")
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.additionalData", is("[{\"id\":\"id\",\"type\":\"dataSet\",\"data\":\"\\\"{\\\"the\\\":\\\"object\\\"}\\\"\"}]")));
    }

    @Test
    public void startSessionStartNewSessionAndUpdateSessionData() throws Exception {

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();
        Map<String, String> postParams = new HashMap();

        MvcResult result = mvc.perform(post("/sm/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/startSession", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /startSession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded"))
                .andExpect(status().isCreated())
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        date = new Date();
        formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest(("sessionId=" + sessionId).getBytes());
        requestId = UUID.randomUUID().toString();

        postParams = new HashMap();
        postParams.put("sessionId", sessionId);
        result = mvc.perform(post("/sm/new/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /startSession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(("sessionId=" + sessionId).getBytes())
        )
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(sessionId, mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class).getSessionData().getSessionId());

        UpdateDataRequest update = new UpdateDataRequest(sessionId, "var1", "dataObject");
        String updateString = mapper.writeValueAsString(update);
        digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes()); // post parameters are added as uri parameters not in the body when form-encoding

        nowDate = formatter.format(date);
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
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("OK")));

    }

}
