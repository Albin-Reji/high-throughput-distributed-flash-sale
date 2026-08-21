package com.project_aegis.inventory_service.client;

import com.project_aegis.inventory_service.config.FeignSecurityConfig;
import com.project_aegis.inventory_service.dto.response.SkuResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

//If you're using Eureka/service discovery,
// don't specify url: in FeignClient
@FeignClient(name = "product-service",
            url = "${service.product.url}",
            configuration = FeignSecurityConfig.class
)
public interface ProductClient {

    @GetMapping("/api/v1/products/skus/{skuId}")
    SkuResponse getSku(@PathVariable UUID skuId);

}
