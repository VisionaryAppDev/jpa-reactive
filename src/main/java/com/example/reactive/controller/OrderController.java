package com.example.reactive.controller;


import com.example.reactive.entity.Order;
import com.example.reactive.repository.OrderRepository;
import com.example.reactive.vo.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@RequiredArgsConstructor
@RestController
public class OrderController {
    private final OrderRepository orderRepository;


    @GetMapping("orders")
    public Flux<Order> getAllOrders() throws ExecutionException, InterruptedException {
        System.out.println(orderRepository.findAll().collectMap(order -> order).toFuture().get());
        System.out.println(orderRepository.findAll().collectList().toFuture().get());
        return orderRepository
                .findAll()
                .map(order -> order)
                .defaultIfEmpty(Order.of("","",1.1, 1, OrderStatus.REJECTED))
                .flatMap(orderRepository::save)
                .doOnNext(order -> {
                    /// publish event
                });
    }

    @PostMapping("orders")
    public Mono<Order> submitOrder() throws ExecutionException, InterruptedException {
        System.out.println(orderRepository.findById(2L).toFuture().get());
        return orderRepository.findById(2L);
    }

    @PutMapping("orders")
    public Mono<Order> updateOrder()  {
        return orderRepository.findById(2L).flatMap(orderRepository::save);
    }


    ////////////////////////////// -> https://reflectoring.io/getting-started-with-spring-webflux/
    @GetMapping("/{orderId}")
    public Mono<ResponseEntity<Order>> getOrderById(@PathVariable Long orderId){
        Mono<Order> user = orderRepository.findById(orderId);
        return user.map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{orderId}")
    public Mono<ResponseEntity<Order>> updateOrderById(@RequestBody Order user){
        return orderRepository.save(user)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @DeleteMapping("/{orderId}")
    public Mono<ResponseEntity<Void>> deleteOrderById(@PathVariable Long orderId){
        return orderRepository.deleteById(orderId)
                .map( r -> ResponseEntity.ok().<Void>build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    ////// Functional Routing and Handling
    @Bean
    RouterFunction<ServerResponse> routes() {
        return route(GET("/handler/users").and(accept(MediaType.APPLICATION_JSON)), this::getUserById)
                .andRoute(GET("/handler/users/{userId}").and(contentType(MediaType.APPLICATION_JSON)), this::getUserById);
    }

    public Mono<ServerResponse> getUserById(ServerRequest request) {
        return orderRepository
                .findById(Long.valueOf(request.pathVariable("userId")))
                .flatMap(user -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(user, Order.class)
                )
                .switchIfEmpty(ServerResponse.notFound().build());

//        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
//                .body(BodyInserters.fromObject("Hello, Spring WebFlux Example!"));
    }


    /// Server side streaming
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Order> streamAllOrders() {
        return orderRepository
                .findAll()
                .flatMap(user -> Flux
                        .zip(Flux.interval(Duration.ofSeconds(2)),
                                Flux.fromStream(Stream.generate(() -> user))
                        )
                        .map(Tuple2::getT2)
                );
    }
}
