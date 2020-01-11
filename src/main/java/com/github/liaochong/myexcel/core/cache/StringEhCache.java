/*
 * Copyright 2019 liaochong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liaochong.myexcel.core.cache;

import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

/**
 * @author liaochong
 * @version 1.0
 */
public class StringEhCache implements Cache<Integer, String> {

    private static volatile CacheManager cacheManager;

    private static volatile long count;

    private org.ehcache.Cache<Integer, String> stringsCache;

    private String cacheName;

    public StringEhCache() {
        synchronized (StringEhCache.class) {
            cacheName = "stringsCache" + count++;
        }
    }

    @Override
    public void cache(Integer key, String value) {
        initCache();
        stringsCache.put(key, value);
    }

    private void initCache() {
        if (stringsCache == null) {
            if (cacheManager == null) {
                synchronized (StringEhCache.class) {
                    if (cacheManager == null) {
                        cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
                    }
                }
            }
            stringsCache = cacheManager.createCache(cacheName,
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, String.class,
                            ResourcePoolsBuilder.newResourcePoolsBuilder().heap(20, MemoryUnit.MB)
                                    .disk(10, MemoryUnit.GB)).build());
        }
    }

    @Override
    public String get(Integer key) {
        return stringsCache.get(key);
    }

    @Override
    public void clearAll() {
        if (stringsCache == null) {
            return;
        }
        cacheManager.removeCache(cacheName);
    }
}
