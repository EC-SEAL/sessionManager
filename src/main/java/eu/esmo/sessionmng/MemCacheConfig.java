/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.esmo.sessionmng;

import com.google.code.ssm.Cache;
import com.google.code.ssm.CacheFactory;
import com.google.code.ssm.config.AddressProvider;
import com.google.code.ssm.config.DefaultAddressProvider;
import com.google.code.ssm.providers.CacheConfiguration;
import com.google.code.ssm.providers.spymemcached.MemcacheClientFactoryImpl;
import com.google.code.ssm.spring.ExtendedSSMCacheManager;
import com.google.code.ssm.spring.SSMCache;
import eu.esmo.sessionmng.service.ParameterService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 *
 * @author nikos
 */
@Configuration
public class MemCacheConfig {

    private ParameterService paramServ;

    private final static Logger log = LoggerFactory.getLogger(MemCacheConfig.class);
    private String _memcachedHost = "172.17.0.1";// when running in docker bridge mode the address of memchached becomse the the
    // default brider for more info look at https://stackoverflow.com/questions/24319662/from-inside-of-a-docker-container-how-do-i-connect-to-the-localhost-of-the-mach
    //"127.0.0.1"; //Machine where memcached is running
    private int _memcachedPort = 11211;    //Port on which memcached is running
    public final static String BLACKLIST = "blackList";

    @Autowired
    public MemCacheConfig(ParameterService paramserv) {
        this.paramServ = paramserv;
        try {
            this._memcachedHost = StringUtils.isEmpty(this.paramServ.getProperty("MEMCACHED_HOST")) ? "172.17.0.1" : this.paramServ.getProperty("MEMCACHED_HOST");
            this._memcachedPort = StringUtils.isEmpty(this.paramServ.getProperty("MEMCACHED_PORT")) ? 11211 : Integer.parseInt(this.paramServ.getProperty("MEMCACHED_PORT"));
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
            this._memcachedPort = 11211;
            this._memcachedHost = "172.17.0.1";
        }

    }

    @Bean
    public CacheManager cacheManager() {
        //Extended manager used as it will give custom-expiry value facility in future if needed
        ExtendedSSMCacheManager ssmCacheManager = new ExtendedSSMCacheManager();

        //We can create more than one cache, hence list
        List<SSMCache> cacheList = new ArrayList<SSMCache>();

        try {
            String minutes = paramServ.getProperty("EXPIRES");
            //First cache: blacklist
            SSMCache testCache = createNewCache(_memcachedHost, _memcachedPort,
                    BLACKLIST, Integer.valueOf(minutes) * 60);
            cacheList.add(testCache);
        } catch (NumberFormatException e) {
            log.debug(e.getMessage());
        }

        SSMCache dummy = createNewCache(_memcachedHost, _memcachedPort,
                "sessionCache", 0);
        cacheList.add(dummy);
        //Adding cache list to cache manager
        ssmCacheManager.setCaches(cacheList);

        return ssmCacheManager;
    }

    //expiryTimeInSeconds: time(in seconds) after which a given element will expire
    //
    private SSMCache createNewCache(String memcachedServer, int port,
            String cacheName, int expiryTimeInSeconds) {
        //Basic client factory to be used. This is SpyMemcached for now.
        MemcacheClientFactoryImpl cacheClientFactory = new MemcacheClientFactoryImpl();

        //Memcached server address parameters
        //"127.0.0.1:11211"
        String serverAddressStr = memcachedServer + ":" + String.valueOf(port);
        AddressProvider addressProvider = new DefaultAddressProvider(serverAddressStr);

        //Basic configuration object
        CacheConfiguration cacheConfigToUse = getNewCacheConfiguration();

        //Create cache factory
        CacheFactory cacheFactory = new CacheFactory();
        cacheFactory.setCacheName(cacheName);
        cacheFactory.setCacheClientFactory(cacheClientFactory);
        cacheFactory.setAddressProvider(addressProvider);
        cacheFactory.setConfiguration(cacheConfigToUse);

        //Get Cache object
        Cache object = null;
        try {
            object = cacheFactory.getObject();
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        //allow/disallow remove all entries from this cache!!
        boolean allowClearFlag = false;
        SSMCache ssmCache = new SSMCache(object, expiryTimeInSeconds, allowClearFlag);

        return ssmCache;

    }

    private CacheConfiguration getNewCacheConfiguration() {
        CacheConfiguration ssmCacheConfiguration = new CacheConfiguration();
        ssmCacheConfiguration.setConsistentHashing(true);
        //ssmCacheConfiguration.setUseBinaryProtocol(true);
        return ssmCacheConfiguration;
    }

}
