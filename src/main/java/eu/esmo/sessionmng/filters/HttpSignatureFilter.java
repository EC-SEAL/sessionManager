/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng.filters;

import eu.esmo.sessionmng.enums.HttpResponseEnum;
import eu.esmo.sessionmng.service.HttpSignatureService;
import eu.esmo.sessionmng.service.KeyStoreService;
import eu.esmo.sessionmng.service.MSConfigurationService;
import eu.esmo.sessionmng.service.impl.HttpSignatureServiceImpl;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.spec.InvalidKeySpecException;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.GenericFilterBean;

/**
 *
 * @author nikos
 */
@Slf4j
public class HttpSignatureFilter extends GenericFilterBean {

    private final HttpSignatureService sigServ;
    private final MSConfigurationService confServ;
    private final Logger Logger = LoggerFactory.getLogger(HttpSignatureFilter.class);

    @Autowired
    public HttpSignatureFilter(KeyStoreService keysServ, MSConfigurationService confServ) throws KeyStoreException, UnsupportedEncodingException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeySpecException, IOException {
        this.sigServ = new HttpSignatureServiceImpl(DigestUtils.sha256Hex(keysServ.getHttpSigPublicKey().getEncoded()), keysServ.getHttpSigningKey());
        this.confServ = confServ;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        try {

            request.setCharacterEncoding("UTF-8");

            final HttpServletRequest currentRequest = (HttpServletRequest) request;
            if (currentRequest.getMethod().toLowerCase().equals("post")) {

                final MultiReadHttpServletRequest wrappedRequest = new MultiReadHttpServletRequest(currentRequest);

                boolean result = sigServ.verifySignature((HttpServletRequest) wrappedRequest, confServ).equals(HttpResponseEnum.AUTHORIZED);
                if (result) {
                    chain.doFilter(wrappedRequest, response);
                } else {
                    throw new ServletException("Error Validating Http Signature from request");
                }

//                }
            } else {
                boolean result = sigServ.verifySignature((HttpServletRequest) request, confServ).equals(HttpResponseEnum.AUTHORIZED);
                if (result) {
                    chain.doFilter(request, response);
                } else {
                    throw new ServletException("Error Validating Http Signature from request");
                }

            }

        } catch (KeyStoreException ex) {
            Logger.error(ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            Logger.error(ex.getMessage());
        } catch (UnrecoverableKeyException ex) {
            Logger.error(ex.getMessage());
        } catch (UnsupportedEncodingException ex) {
            Logger.error(ex.getMessage());
        }

    }

    public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {

        private ByteArrayOutputStream cachedBytes;

        public MultiReadHttpServletRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (cachedBytes == null) {
                cacheInputStream();
            }

            return new CachedServletInputStream(cachedBytes.toByteArray());
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        private void cacheInputStream() throws IOException {
            /* Cache the inputstream in order to read it multiple times. For
     * convenience, I use apache.commons IOUtils
             */
            cachedBytes = new ByteArrayOutputStream();
            IOUtils.copy(super.getInputStream(), cachedBytes);
        }

        /* An inputstream which reads the cached request body */
        class CachedServletInputStream extends ServletInputStream {

            private final ByteArrayInputStream buffer;

            public CachedServletInputStream(byte[] contents) {
                this.buffer = new ByteArrayInputStream(contents);
            }

            @Override
            public int read() throws IOException {
                return buffer.read();
            }

            @Override
            public boolean isFinished() {
                return buffer.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new RuntimeException("Not implemented");
            }
        }
    }

}
