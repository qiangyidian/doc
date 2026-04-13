package com.example.productcache.controller;

import com.example.productcache.common.Result;
import com.example.productcache.dto.UpdatePriceRequest;
import com.example.productcache.entity.ProductInfo;
import com.example.productcache.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public Result<ProductInfo> getProduct(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }

    @PutMapping("/{id}/price")
    public Result<ProductInfo> updatePrice(@PathVariable Long id, @RequestBody UpdatePriceRequest request) {
        return Result.success(productService.updatePrice(id, request.getPrice()));
    }

    @GetMapping("/cache/{id}")
    public Result<String> getCacheValue(@PathVariable Long id) {
        return Result.success(productService.getRawCacheValue(id));
    }
}