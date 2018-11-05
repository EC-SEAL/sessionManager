/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.esmo.sessionmng.SessionMngApplication;
import eu.esmo.sessionmng.model.TO.MngrSessionTO;
import eu.esmo.sessionmng.model.dmo.MngrSession;
import eu.esmo.sessionmng.model.service.JwtService;
import eu.esmo.sessionmng.model.service.KeyStoreService;
import eu.esmo.sessionmng.model.service.ParameterService;
import eu.esmo.sessionmng.model.service.SessionService;
import eu.esmo.sessionmng.pojo.SessionMngrResponse;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author nikos
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SessionMngApplication.class)
@AutoConfigureMockMvc
public class RestControllersIntegration {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private KeyStoreService keyServ;

    @MockBean
    private ParameterService paramServ;

    @Autowired
    private JwtService jwtServ;

    @Autowired
    private SessionService sessionServ;

    @Test
    public void testsStartSession() throws Exception {
        MvcResult result = mvc.perform(post("/startSession"))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);

        MngrSessionTO createdSession = sessionServ.findBySessionId(resp.getSessionData().getSessionId());
        assertNotNull(createdSession);
        assertEquals(result.getResponse().getContentAsString(), "{\"code\":\"NEW\",\"sessionData\":{\"sessionId\":\"" + resp.getSessionData().getSessionId() + "\",\"sessionVariables\":{}},\"additionalData\":null,\"error\":null}");

    }

    @Test
    public void deleteExistingSession() throws Exception {
        MvcResult result = mvc.perform(post("/startSession"))
                .andExpect(status().isOk())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();
        mvc.perform(delete("/endSession")
                .param("sessionId", sessionId))
                .andExpect(status().isOk());
        assertEquals(sessionServ.findBySessionId(sessionId), null);
    }

    @Test
    public void testUpdateSessionDataExistingSession() throws Exception {
        MvcResult result = mvc.perform(post("/startSession"))
                .andExpect(status().isOk())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        mvc.perform(post("/updateSessionData")
                .param("sessionId", sessionId)
                .param("variableName", "var1")
                .param("dataObject", "dataObject")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")));

        assertEquals(sessionServ.findBySessionId(sessionId).getSessionVariables().get("var1"), "dataObject");

    }

    @Test
    public void testGenerateTokenExistingSession() throws Exception {
        String secretKey = "QjG+wP1CbAH2z4PWlWIDkxP4oRlgK2vos5/jXFfeBw8=";
        Key key = new SecretKeySpec(secretKey.getBytes("UTF-8"), 0, secretKey.length(), "HmacSHA256");

        when(keyServ.getAlgorithm()).thenReturn(SignatureAlgorithm.HS256);
        when(keyServ.getSigningKey()).thenReturn(key);
        when(keyServ.getPublicKey()).thenReturn(key);

        when(paramServ.getProperty("ISSUER")).thenReturn("EMSO_SESSION_MANAGER");
        when(paramServ.getProperty("EXPIRES")).thenReturn("5");

        MvcResult result = mvc.perform(post("/startSession"))
                .andExpect(status().isOk())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        SessionMngrResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SessionMngrResponse.class);
        String sessionId = resp.getSessionData().getSessionId();

        MvcResult jwtResult = mvc.perform(get("/generateToken")
                .param("sessionId", sessionId)
                .param("sender", "senderId")
                .param("receiver", "receiverId")
                .param("data", "extraData")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("NEW"))).andReturn();

        SessionMngrResponse jwtResponse = mapper.readValue(jwtResult.getResponse().getContentAsString(), SessionMngrResponse.class);
        assertNotNull(jwtResponse);
        assertNotNull(jwtResponse.getAdditionalData());
        assertEquals(jwtResponse.getSessionData(), null);

        assertEquals(jwtServ.validateJwt(jwtResponse.getAdditionalData()).getSessionData().getSessionId(), sessionId);

    }

    @Test
    public void testGenerateTokenFAKESession() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String secretKey = "QjG+wP1CbAH2z4PWlWIDkxP4oRlgK2vos5/jXFfeBw8=";
        Key key = new SecretKeySpec(secretKey.getBytes("UTF-8"), 0, secretKey.length(), "HmacSHA256");

        when(keyServ.getAlgorithm()).thenReturn(SignatureAlgorithm.HS256);
        when(keyServ.getSigningKey()).thenReturn(key);
        when(keyServ.getPublicKey()).thenReturn(key);

        when(paramServ.getProperty("ISSUER")).thenReturn("EMSO_SESSION_MANAGER");
        when(paramServ.getProperty("EXPIRES")).thenReturn("5");

        MvcResult jwtResult = mvc.perform(get("/generateToken")
                .param("sessionId", "fakeSession")
                .param("sender", "senderId")
                .param("receiver", "receiverId")
                .param("data", "extraData")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("ERROR"))).andReturn();

        SessionMngrResponse jwtResponse = mapper.readValue(jwtResult.getResponse().getContentAsString(), SessionMngrResponse.class);
        assertNotNull(jwtResponse);
        assertNotNull(jwtResponse.getError());
        assertEquals(jwtResponse.getError(), "sessionId not found");


    }

    @Test
    public void validateToken() throws JsonProcessingException, UnsupportedEncodingException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, Exception {

        String secretKey = "QjG+wP1CbAH2z4PWlWIDkxP4oRlgK2vos5/jXFfeBw8=";
        Key key = new SecretKeySpec(secretKey.getBytes("UTF-8"), 0, secretKey.length(), "HmacSHA256");

        when(keyServ.getAlgorithm()).thenReturn(SignatureAlgorithm.HS256);
        when(keyServ.getSigningKey()).thenReturn(key);
        when(keyServ.getPublicKey()).thenReturn(key);

        String jwt = jwtServ.makeJwt("sessionId", "extraData", "ISSUER", "sender", "receiver", Long.valueOf(5));
        mvc.perform(get("/validateToken").param("token", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.sessionData.sessionId", is("sessionId")));

    }

}