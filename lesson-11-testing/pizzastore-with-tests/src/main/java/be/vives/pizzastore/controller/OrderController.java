package be.vives.pizzastore.controller;

import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.dto.request.CreateOrderRequest;
import be.vives.pizzastore.dto.request.UpdateOrderStatusRequest;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {

        log.debug("GET /api/orders - customerId: {}, status: {}, pageable: {}",
                customerId, status, pageable);

        Page<OrderResponse> orders;

        if (customerId != null) {
            orders = orderService.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            orders = orderService.findByStatus(status, pageable);
        } else {
            orders = orderService.findAll(pageable);
        }

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        log.debug("GET /api/orders/{}", id);
        OrderResponse order = orderService.findById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        log.debug("POST /api/orders - {}", request);

        OrderResponse created = orderService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest request) {

        log.debug("PATCH /api/orders/{}/status - {}", id, request.status());

        OrderResponse updated = orderService.updateStatus(id, request.status());
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        log.debug("DELETE /api/orders/{}", id);

        orderService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
