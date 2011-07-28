package org.nick.wwwjdic.app.server;

import java.util.HashMap;
import java.util.Map;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;
import net.sf.jsr107cache.CacheStatistics;

import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;

public class CacheController {

    private static Cache cache;

    static {
        try {
            CacheFactory cacheFactory = CacheManager.getInstance()
                    .getCacheFactory();
            @SuppressWarnings("rawtypes")
            Map props = createPolicyMap();
            cache = cacheFactory.createCache(props);
        } catch (CacheException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map createPolicyMap() {
        Map props = new HashMap();
        props.put(GCacheFactory.EXPIRATION_DELTA, 1800);
        return props;
    }

    public static String fetchCacheStatistics() {
        CacheStatistics stats = cache.getCacheStatistics();
        int hits = stats.getCacheHits();
        int misses = stats.getCacheMisses();
        return "Cache Hits=" + hits + " : Cache Misses=" + misses;
    }

    public static synchronized void put(String key, Object value) {
        cache.put(key, value);
    }

    public static Object get(String key) {
        return cache.get(key);
    }

    public static synchronized void clear() {
        cache.clear();
    }

    public static synchronized void remove(Object obj) {
        cache.remove(obj);
    }
}
