## 1) OpenFeign with Service Discovery

If you're using **Eureka or another service discovery mechanism**, you don't need to specify the `url` in `@FeignClient`.

Feign uses the service name (`product-service`) to discover the available service instance.

### Without Service Discovery

```java
@FeignClient(
        name = "product-service",
        url = "${service.product.url}"
)
public interface ProductClient {

    @GetMapping("/api/v1/products/skus/{skuId}")
    SkuResponse getSku(@PathVariable UUID skuId);

}