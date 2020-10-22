/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.esmo.sessionmng.pojo.NewUpdateDataRequest;
import eu.esmo.sessionmng.pojo.RequestParameters;
import eu.esmo.sessionmng.pojo.SessionMngrResponse;
import eu.esmo.sessionmng.pojo.UpdateDataRequest;
import eu.esmo.sessionmng.service.HttpSignatureService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public void startSessionOld() throws Exception {

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("sessionId=sessionId".getBytes());
        String requestId = UUID.randomUUID().toString();

        Map<String, String> postParams = new HashMap();
        postParams.put("sessionId", "sessionId");

        mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
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
    public void startSession() throws Exception {

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("sessionId=sessionId".getBytes());
        String requestId = UUID.randomUUID().toString();

        ObjectMapper mapper = new ObjectMapper();
        RequestParameters postParams = new RequestParameters("data", "id", "sessionId", "type");
        String updateString = mapper.writeValueAsString(postParams);
        digest = MessageDigest.getInstance("SHA-256").digest(updateString.getBytes());

        mvc.perform(post("/sm/new/startSession")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession", postParams, "application/json", requestId))
                .header("host", "hostUrl")
                //                .header("(request-target)", "POST /startSession")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded")
                .header("content-type", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateString.getBytes())
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

        MvcResult result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
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
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/get/old", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/get/old")
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

        MvcResult result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", null, "application/x-www-form-urlencoded", requestId))
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

        MvcResult result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", null, "application/x-www-form-urlencoded", requestId))
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
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/get/old", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/get/old")
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
    public void add2ToSessionAndDelete1AndGetSecond() throws Exception {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        MvcResult result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", null, "application/x-www-form-urlencoded", requestId))
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

        resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        sessionId = resp.getSessionData().getSessionId();

        update = new NewUpdateDataRequest(sessionId, "dataSet", "\"{\"the\":\"object2\"}\"", "id2");
        updateString = mapper.writeValueAsString(update);
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

        date = new Date();
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        requestId = UUID.randomUUID().toString();
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/get/old", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/get/old")
                .header("authorization", authHeader)
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .param("sessionId", sessionId)
                .param("id", "id2")
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.additionalData", is("{\"id\":\"id2\",\"type\":\"dataSet\",\"data\":\"\\\"{\\\"the\\\":\\\"object2\\\"}\\\"\"}")));

    }

    @Test
    public void searchSessionIdAndType() throws Exception {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();

        MvcResult result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", null, "application/x-www-form-urlencoded", requestId))
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
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/search/old", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/search/old")
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

        MvcResult result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", null, "application/x-www-form-urlencoded", requestId))
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
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/search/old", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/search/old")
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
        result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
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

    @Test
    public void startNewSessionAndUpdateSessionDataAndReset() throws Exception {

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
        result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
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
        formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest(("sessionId=" + sessionId).getBytes());
        requestId = UUID.randomUUID().toString();
        postParams = new HashMap();
        postParams.put("sessionId", sessionId);
        result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
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

        date = new Date();
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        requestId = UUID.randomUUID().toString();
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/search/old", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/search/old")
                .header("authorization", authHeader)
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .param("sessionId", sessionId)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.additionalData", is("[]")));

    }

    //- After I generate a sessionId using the new API, I cannot make a call to GetSessionData (old API). Throws a Not Found error. Maybe the new sessionId's are not compatible with the old methods or smt?
    @Test
    public void startNewSessionGetDataOld() throws Exception {

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = UUID.randomUUID().toString();
        Map<String, String> postParams = new HashMap();
        ObjectMapper mapper = new ObjectMapper();

        MvcResult result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .header("content-type", "application/x-www-form-urlencoded")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(("").getBytes())
        )
                .andExpect(status().isOk())
                .andReturn();
        String sessionId = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class).getSessionData().getSessionId();

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

        date = new Date();
        formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        requestId = UUID.randomUUID().toString();

        mvc.perform(get("/sm/getSessionData?sessionId=" + sessionId + "&variableName=var1")
                .header("authorization", sigServ.generateSignature("hostUrl", "GET", "/sm/getSessionData?sessionId=" + sessionId + "&variableName=var1", null, "application/x-www-form-urlencoded", requestId))
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.sessionData.sessionVariables.var1", is("dataObject")))
                .andExpect(jsonPath("$.sessionData.sessionVariables.var2").doesNotExist());

    }

    @Test
    public void startSessionAndThenNewSession() throws Exception {

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
        result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", postParams, "application/x-www-form-urlencoded;charset=UTF-8", requestId))
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

        resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId2 = resp.getSessionData().getSessionId();

        assertEquals(sessionId, sessionId2);
    }

    @Test
    public void addTwoObjectsAndGetWithRealIdentifiers() throws Exception {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM YYYY HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String nowDate = formatter.format(date);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        String requestId = "21e28aaf-a3c1-4298-98d2-9780fcaa4768";

        MvcResult result = mvc.perform(post("/sm/new/startSession/old")
                .header("authorization", sigServ.generateSignature("hostUrl", "POST", "/sm/new/startSession/old", null, "application/x-www-form-urlencoded", requestId))
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

        NewUpdateDataRequest update = new NewUpdateDataRequest(sessionId, "dataSet", "{"
                + "    \"id\": \"urn:mace:project-seal.eu:id:dataset:edugainIDPms_001:https%3A%2F%2Feid-proxy.aai-dev.grnet.gr%2FSaml2IDP%2Fproxy.xml:128052%40gn-vho.grnet.gr\","
                + "    \"type\": \"dataSet\","
                + "    \"data\": \"{\\\"id\\\":\\\"89453f67-cef4-4052-86be-123dc60fba2b\\\",\\\"type\\\":\\\"eduGAIN\\\",\\\"categories\\\":null,\\\"issuerId\\\":\\\"issuerEntityId\\\",\\\"subjectId\\\":\\\"eduPersonPrincipalName\\\",\\\"loa\\\":null,\\\"issued\\\":\\\"Fri, 9 Oct 2020 14:00:10 GMT\\\",\\\"expiration\\\":null,\\\"attributes\\\":[{\\\"name\\\":\\\"issuerEntityId\\\",\\\"friendlyName\\\":\\\"issuerEntityId\\\",\\\"encoding\\\":null,\\\"language\\\":null,\\\"values\\\":[\\\"https://eid-proxy.aai-dev.grnet.gr/Saml2IDP/proxy.xml\\\"]},{\\\"name\\\":\\\"urn:oid:1.3.6.1.4.1.5923.1.1.1.10\\\",\\\"friendlyName\\\":\\\"eduPersonTargetedID\\\",\\\"encoding\\\":null,\\\"language\\\":null,\\\"values\\\":[null]},{\\\"name\\\":\\\"urn:oid:2.5.4.42\\\",\\\"friendlyName\\\":\\\"givenName\\\",\\\"encoding\\\":null,\\\"language\\\":null,\\\"values\\\":[\\\"ΧΡΙΣΤΙΝΑ CHRISTINA\\\"]},{\\\"name\\\":\\\"urn:oid:0.9.2342.19200300.100.1.3\\\",\\\"friendlyName\\\":\\\"mail\\\",\\\"encoding\\\":null,\\\"language\\\":null,\\\"values\\\":[\\\"seal-test0@example.com\\\"]},{\\\"name\\\":\\\"urn:oid:2.5.4.3\\\",\\\"friendlyName\\\":\\\"cn\\\",\\\"encoding\\\":null,\\\"language\\\":null,\\\"values\\\":[\\\"ΠΑΛΙΟΚΩΣΤΑ PALIOKOSTA ΧΡΙΣΤΙΝΑ CHRISTINA\\\"]},{\\\"name\\\":\\\"urn:oid:2.5.4.4\\\",\\\"friendlyName\\\":\\\"sn\\\",\\\"encoding\\\":null,\\\"language\\\":null,\\\"values\\\":[\\\"ΠΑΛΙΟΚΩΣΤΑ PALIOKOSTA\\\"]},{\\\"name\\\":\\\"urn:oid:2.16.840.1.113730.3.1.241\\\",\\\"friendlyName\\\":\\\"displayName\\\",\\\"encoding\\\":null,\\\"language\\\":null,\\\"values\\\":[\\\"ΧΡΙΣΤΙΝΑ CHRISTINA ΠΑΛΙΟΚΩΣΤΑ PALIOKOSTA\\\"]},{\\\"name\\\":\\\"urn:oid:1.3.6.1.4.1.5923.1.1.1.6\\\",\\\"friendlyName\\\":\\\"eduPersonPrincipalName\\\",\\\"encoding\\\":null,\\\"language\\\":null,\\\"values\\\":[\\\"128052@gn-vho.grnet.gr\\\"]},{\\\"name\\\":\\\"urn:oid:1.3.6.1.4.1.5923.1.1.1.7\\\",\\\"friendlyName\\\":\\\"eduPersonEntitlement\\\",\\\"encoding\\\":null,\\\"language\\\":null,\\\"values\\\":[\\\"urn:mace:grnet.gr:seal:test\\\"]}],\\\"properties\\\":null}\""
                + "  }", URLEncoder.encode("urn:mace:project-seal.eu:id:dataset:edugainIDPms_001:https%3A%2F%2Feid-proxy.aai-dev.grnet.gr%2FSaml2IDP%2Fproxy.xml:128052%40gn-vho.grnet.gr", StandardCharsets.UTF_8.toString()));
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

        update = new NewUpdateDataRequest(sessionId, "dataSet", " {"
                + "    \"id\": \"eIDASGR/GR/ABCD1234\","
                + "    \"type\": \"dataSet\","
                + "    \"data\": \"{\\\"id\\\":\\\"8beefd54-46fb-4514-9bf7-fae626a2b67b\\\",\\\"type\\\":\\\"eIDAS\\\",\\\"categories\\\":null,\\\"issuerId\\\":\\\"eIDAS\\\",\\\"subjectId\\\":null,\\\"loa\\\":null,\\\"issued\\\":\\\"Fri, 9 Oct 2020 14:02:48 GMT\\\",\\\"expiration\\\":null,\\\"attributes\\\":[{\\\"name\\\":\\\"http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName\\\",\\\"friendlyName\\\":\\\"FamilyName\\\",\\\"encoding\\\":\\\"UTF-8\\\",\\\"language\\\":\\\"N/A\\\",\\\"values\\\":[\\\"MyFamilyName\\\"]},{\\\"name\\\":\\\"http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName\\\",\\\"friendlyName\\\":\\\"GivenName\\\",\\\"encoding\\\":\\\"UTF-8\\\",\\\"language\\\":\\\"N/A\\\",\\\"values\\\":[\\\"MyGivenName\\\"]},{\\\"name\\\":\\\"http://eidas.europa.eu/attributes/naturalperson/DateOfBirth\\\",\\\"friendlyName\\\":\\\"DateOfBirth\\\",\\\"encoding\\\":\\\"UTF-8\\\",\\\"language\\\":\\\"N/A\\\",\\\"values\\\":[\\\"1980-01-02\\\"]},{\\\"name\\\":\\\"http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier\\\",\\\"friendlyName\\\":\\\"PersonIdentifier\\\",\\\"encoding\\\":\\\"UTF-8\\\",\\\"language\\\":\\\"N/A\\\",\\\"values\\\":[\\\"GR/GR/ABCD1234\\\"]},{\\\"name\\\":\\\"http://eidas.europa.eu/LoA\\\",\\\"friendlyName\\\":\\\"LevelOfAssurance\\\",\\\"encoding\\\":\\\"UTF-8\\\",\\\"language\\\":\\\"N/A\\\",\\\"values\\\":[null]}],\\\"properties\\\":null}\""
                + "  }", URLEncoder.encode("eIDASGR/GR/ABCD1234", StandardCharsets.UTF_8.toString()));
        updateString = mapper.writeValueAsString(update);
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
        String authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/get/old", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/get/old")
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

        date = new Date();
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        nowDate = formatter.format(date);
        digest = MessageDigest.getInstance("SHA-256").digest("".getBytes());
        requestId = UUID.randomUUID().toString();
        authHeader = sigServ.generateSignature("hostUrl", "GET", "/sm/new/get/old", null, "application/x-www-form-urlencoded", requestId);
        mvc.perform(get("/sm/new/get/old")
                .header("authorization", authHeader)
                .header("host", "hostUrl")
                .header("original-date", nowDate)
                .header("digest", "SHA-256=" + new String(org.tomitribe.auth.signatures.Base64.encodeBase64(digest)))
                .header("x-request-id", requestId)
                .param("sessionId", sessionId)
                .param("id", URLEncoder.encode("urn:mace:project-seal.eu:id:dataset:edugainIDPms_001:https%3A%2F%2Feid-proxy.aai-dev.grnet.gr%2FSaml2IDP%2Fproxy.xml:128052%40gn-vho.grnet.gr"))
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")));

    }

}
