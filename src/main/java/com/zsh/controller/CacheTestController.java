package com.zsh.controller;

import com.zsh.cache.CacheManager;
import com.zsh.cache.CacheService;
import com.zsh.cache.CacheStats;
import com.zsh.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheTestController {

    private final CacheManager cacheManager;

    @PostMapping("/set")
    public Result<Boolean> setCache(@RequestParam String key,
                                    @RequestParam String value,
                                    @RequestParam(required = false, defaultValue = "300") Long ttl) {
        cacheManager.set(key, value, ttl, TimeUnit.SECONDS);
        return Result.success(true);
    }

    @GetMapping("/get")
    public Result<String> getCache(@RequestParam String key) {
        String value = cacheManager.get(key, String.class);
        return Result.success(value);
    }

    @DeleteMapping("/delete")
    public Result<Boolean> deleteCache(@RequestParam String key) {
        Boolean result = cacheManager.delete(key);
        return Result.success(result);
    }

    @GetMapping("stats")
    public Result<CacheStats> getCacheStats() {
        CacheStats stats = cacheManager.getStats();
        return Result.success(stats);
    }

}
