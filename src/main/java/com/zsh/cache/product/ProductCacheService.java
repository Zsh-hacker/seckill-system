package com.zsh.cache.product;

import com.zsh.vo.ProductVO;

import java.util.List;
import java.util.Map;

public interface ProductCacheService {

    // 商品信息缓存
    ProductVO getProductById(Long productId);
    void cacheProduct(ProductVO products);
    void batchCacheProducts(List<ProductVO> products);
    void deleteProductCache(Long productId);
    void deleteProductCache(String... keys);

    // 商品库存缓存
    Integer getProductStock(Long productId);
    void cacheProductStock(Long productId, Integer stock);
    void decreaseProductStock(Long productId, Integer quantity);
    void increaseProductStock(Long productId, Integer quantity);
    void deleteProductStockCache(Long productId);

    // 批量操作
    Map<Long, ProductVO> batchGetProducts(List<Long> productIds);
    Map<Long, Integer> batchGetProductsStocks(List<Long> productIds);

    // 缓存预热
    void preloadHotProducts();
    void preloadProductStocks();

    // 统计信息
    long getProductCacheHitCount();
    long getProductCacheMissCount();
}
