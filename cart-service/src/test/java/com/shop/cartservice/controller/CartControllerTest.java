package com.shop.cartservice.controller;

import com.shop.cartservice.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private MockMvc mockMvc;
    private final String userId = "user123";
    private final String productId = "product456";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
    }

    @Test
    void getCart_ValidRequest_ReturnsOk() throws Exception {
        when(cartService.getCart(userId)).thenReturn(Map.of("products", List.of()));

        mockMvc.perform(get("/api/cart")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").exists());

        verify(cartService).getCart(userId);
    }

    @Test
    void getCart_MissingUserIdHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addToCart_ValidRequest_ReturnsNoContent() throws Exception {
        doNothing().when(cartService).addToCart(userId, productId);

        mockMvc.perform(post("/api/cart/add")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("product456"))
                .andExpect(status().isNoContent());

        verify(cartService).addToCart(userId, productId);
    }

    @Test
    void addToCart_EmptyProductId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeFromCart_ValidRequest_ReturnsNoContent() throws Exception {
        doNothing().when(cartService).removeFromCart(productId, userId);

        mockMvc.perform(post("/api/cart/remove")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("product456"))
                .andExpect(status().isNoContent());

        verify(cartService).removeFromCart(productId, userId);
    }

    @Test
    void removeAllAmountOfProduct_ValidRequest_ReturnsNoContent() throws Exception {
        doNothing().when(cartService).removeAllAmountOfProductFromCart(productId, userId);

        mockMvc.perform(delete("/api/cart/{id}", productId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(cartService).removeAllAmountOfProductFromCart(productId, userId);
    }

    @Test
    void deleteCart_ValidRequest_ReturnsNoContent() throws Exception {
        doNothing().when(cartService).deleteCart(userId);

        mockMvc.perform(delete("/api/cart")
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(cartService).deleteCart(userId);
    }

    @Test
    void validateCart_ValidRequest_ReturnsNoContent() throws Exception {
        doNothing().when(cartService).validateCart(userId);

        mockMvc.perform(get("/api/cart/valid")
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(cartService).validateCart(userId);
    }
}