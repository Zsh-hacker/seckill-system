package com.zsh.cache.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zsh.cache.CacheManager;
import com.zsh.cache.product.ProductCacheService;
import com.zsh.constant.CacheKey;
import com.zsh.dao.ProductDao;
import com.zsh.dao.ProductStockDao;
import com.zsh.entity.Product;
import com.zsh.entity.ProductStock;
import com.zsh.vo.ProductVO;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheServieImpl implements ProductCacheService {

    private final CacheManager cacheManager;
    private final ProductDao productDAO;
    private final ProductStockDao productStockDAO;

    // 缓存过期时间配置
    private static final long PRODUCT_TTL = TimeUnit.MINUTES.toSeconds(30);
    private static final long STOCK_TTL = TimeUnit.MINUTES.toSeconds(5);

    // 热点商品ID（可以配置化）
    private static final List<Long> HOT_PRODUCT_IDS = Arrays.asList(1L, 2L, 3L);


    @Override
    public ProductVO getProductById(Long productId) {
        String cacheKey = CacheKey.format(CacheKey.PRODUCT_KEY, productId);

        return cacheManager.getWithCachePenetrationProtection(
                cacheKey,
                ProductVO.class,
                () -> {
                    Product product = productDAO.selectById(productId);
                    if (product == null) {
                        return null;
                    }

                    // 转换为VO
                    ProductVO vo = convertToVO(product);

                    // 获取库存信息
                    ProductStock stock = productStockDAO.selectOne(
                            new QueryWrapper<ProductStock>().lambda()
                                    .eq(ProductStock::getProductId, productId)
                    );
                    if (stock != null) {
                        vo.setAvailableStock(stock.getAvailableStock());
                    }

                    return vo;
                },
                PRODUCT_TTL,
                TimeUnit.SECONDS.SECONDS
        );
    }

    @Override
    public void cacheProduct(ProductVO productVO) {
        if (productVO == null || productVO.getId() == null) {
            return;
        }
        String cacheKey = CacheKey.format(CacheKey.PRODUCT_KEY, productVO.getId());
        cacheManager.set(cacheKey, productVO, PRODUCT_TTL, TimeUnit.SECONDS);
        log.debug("Cached product: {}", productVO.getId());
    }

    @Override
    public void batchCacheProducts(List<ProductVO> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        Map<String, ProductVO> cacheMap = new HashMap<>();
        for (ProductVO product : products) {
            if (product != null && product.getId() != null) {
                String cacheKey = CacheKey.format(CacheKey.PRODUCT_KEY, product.getId());
                cacheMap.put(cacheKey, product);
            }
        }

        cacheManager.multiSet(cacheMap);
        log.debug("Batch cached {} products", cacheMap.size());
    }

    @Override
    public void deleteProductCache(Long productId) {
        String cacheKey = CacheKey.format(CacheKey.PRODUCT_KEY, productId);
        cacheManager.delete(cacheKey);
        log.debug("Deleted product cache: {}", productId);
    }

    @Override
    public void deleteProductCache(String... keys) {
        for (String key : keys) {
            cacheManager.delete(key);
        }
    }

    @Override
    public Integer getProductStock(Long productId) {
        String cacheKey = CacheKey.format(CacheKey.PRODUCT_KEY, productId);
        return cacheManager.getWithCachePenetrationProtection(
                cacheKey,
                Integer.class,
                () -> {
                    ProductStock stock = productStockDAO.selectOne(
                            new QueryWrapper<ProductStock>().lambda()
                            .eq(ProductStock::getProductId, productId));
                    return stock != null ? stock.getAvailableStock() : null;
                },
                STOCK_TTL,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void cacheProductStock(Long productId, Integer stock) {
        if (productId == null || stock == null) {
            return;
        }

        String cacheKey = CacheKey.format(CacheKey.PRODUCT_STOCK_KEY, productId);
        cacheManager.set(cacheKey, stock, STOCK_TTL, TimeUnit.SECONDS);
        log.debug("Cached product stock: {} -> {}", productId, stock);
    }

    @Override
    public void decreaseProductStock(Long productId, Integer quantity) {
        String cacheKey = CacheKey.format(CacheKey.PRODUCT_STOCK_KEY, productId);

        // 使用原子操作减少库存
        Integer currentStock = cacheManager.get(cacheKey, Integer.class);
        if (currentStock != null) {
            int newStock = Math.max(0, currentStock - quantity);
            cacheManager.set(cacheKey, newStock, STOCK_TTL, TimeUnit.SECONDS);
            log.debug("Decreased product stock: {} from {} to {}",
                    productId, currentStock, newStock);
        }
    }

    @Override
    public void increaseProductStock(Long productId, Integer quantity) {
        String cacheKey = CacheKey.format(CacheKey.PRODUCT_STOCK_KEY, productId);

        // 使用原子操作增加库存
        Integer currentStock = cacheManager.get(cacheKey, Integer.class);
        if (currentStock != null) {
            int newStock = currentStock + quantity;
            cacheManager.set(cacheKey, newStock, STOCK_TTL, TimeUnit.SECONDS);
            log.debug("Increased product stock: {} from {} to {}",
                    productId, currentStock, newStock);
        }
    }

    @Override
    public void deleteProductStockCache(Long productId) {
        String cacheKey = CacheKey.format(CacheKey.PRODUCT_STOCK_KEY, productId);
        cacheManager.delete(cacheKey);
        log.debug("Deleted product stock cache: {}", productId);
    }

    @Override
    public Map<Long, ProductVO> batchGetProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 构建缓存key
        Set<String> cacheKeys = productIds.stream()
                .map(id -> CacheKey.format(CacheKey.PRODUCT_KEY, id))
                .collect(Collectors.toSet());

        // 批量从缓存获取
        Map<String, ProductVO> cacheProducts = cacheManager.multiGet(cacheKeys, ProductVO.class);
        Map<Long, ProductVO> result = new HashMap<>();
        for (Long productId : productIds) {
            String cacheKey = CacheKey.format(CacheKey.PRODUCT_KEY, productId);
            ProductVO cacheProduct = cacheProducts.get(cacheKey);

            if (cacheProduct != null) {
                result.put(productId, cacheProduct);
            } else {
                // 如果缓存中没有，单独加载
                ProductVO product = getProductById(productId);
                if (product != null) {
                    result.put(productId, product);
                }
            }
        }
        return result;
    }

    @Override
    public Map<Long, Integer> batchGetProductsStocks(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构建缓存key
        Set<String> cacheKeys = productIds.stream()
                .map(id -> CacheKey.format(CacheKey.PRODUCT_STOCK_KEY, id))
                .collect(Collectors.toSet());

        // 批量从缓存获取
        Map<String, Integer> cachedStocks = cacheManager.multiGet(cacheKeys, Integer.class);

        Map<Long, Integer> result = new HashMap<>();
        for (Long productId : productIds) {
            String cacheKey = CacheKey.format(CacheKey.PRODUCT_STOCK_KEY, productId);
            Integer cachedStock = cachedStocks.get(cacheKey);

            if (cachedStock != null) {
                result.put(productId, cachedStock);
            } else {
                // 如果缓存中没有，单独加载
                Integer stock = getProductStock(productId);
                if (stock != null) {
                    result.put(productId, stock);
                }
            }
        }

        return result;
    }

    @Override
    public void preloadHotProducts() {
        log.info("Preloading hot products: {}", HOT_PRODUCT_IDS);

        for (Long productId : HOT_PRODUCT_IDS) {
            try {
                getProductById(productId); // 触发缓存加载
                getProductStock(productId); // 触发库存缓存加载
            } catch (Exception e) {
                log.error("Failed to preload product: {}", productId, e);
            }
        }

        log.info("Hot products preload completed");
    }

    @Override
    public void preloadProductStocks() {
        log.info("Preloading all product stocks");

        // 获取所有商品库存并缓存
        List<ProductStock> stocks = productStockDAO.selectList(null);
        for (ProductStock stock : stocks) {
            try {
                cacheProductStock(stock.getProductId(), stock.getAvailableStock());
            } catch (Exception e) {
                log.error("Failed to preload product stock: {}", stock.getProductId(), e);
            }
        }

        log.info("Product stocks preload completed, total: {}", stocks.size());
    }

    @Override
    public long getProductCacheHitCount() {
        return cacheManager.getLocalCacheStats().getHitCount();
    }

    @Override
    public long getProductCacheMissCount() {
        return cacheManager.getLocalCacheStats().getMissCount();
    }

    // 工具方法：将Product实体转换为ProductVO
    private ProductVO convertToVO(Product product) {
        if (product == null) {
            return null;
        }

        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }
}
