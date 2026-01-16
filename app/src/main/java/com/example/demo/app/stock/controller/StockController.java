package com.example.demo.app.stock.controller;

import com.example.demo.app.stock.service.StockApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private static final Logger logger = LoggerFactory.getLogger(StockController.class);

    @Autowired
    private StockApiService stockApiService;

    // Inner class for order request payload
    public static class OrderRequest {
        private String stockCode;
        private int quantity;

        public String getStockCode() {
            return stockCode;
        }

        public void setStockCode(String stockCode) {
            this.stockCode = stockCode;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buyStock(@RequestBody OrderRequest orderRequest) {
        try {
            logger.info("Received buy order request: {} shares of {}", orderRequest.getQuantity(), orderRequest.getStockCode());
            Object response = stockApiService.orderStock(orderRequest.getStockCode(), orderRequest.getQuantity());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing stock buy order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "매수 주문 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<?> sellStock(@RequestBody OrderRequest orderRequest) {
        try {
            logger.info("Received sell order request: {} shares of {}", orderRequest.getQuantity(), orderRequest.getStockCode());
            Object response = stockApiService.sellStock(orderRequest.getStockCode(), orderRequest.getQuantity());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing stock sell order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "매도 주문 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/my-stocks")
    public ResponseEntity<Map<String, Object>> getMyStocks() {
        logger.info("========== 보유 주식 조회 API 호출됨 ==========");
        try {
            Map<String, Object> myStocks = stockApiService.getMyStocks();
            return ResponseEntity.ok(myStocks);
        } catch (Exception e) {
            logger.error("Error fetching my stocks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "보유 주식 정보를 가져오는 데 실패했습니다."));
        }
    }

    @GetMapping("/details/{stockCode}")
    public ResponseEntity<Map<String, Object>> getStockDetails(@PathVariable String stockCode) {
        try {
            Map<String, Object> stockDetails = stockApiService.getStockDetails(stockCode);
            if (stockDetails == null || stockDetails.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "해당 종목 정보를 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(stockDetails);
        } catch (Exception e) {
            logger.error("Error fetching stock details for code: {}", stockCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "주식 상세 정보를 가져오는 중 오류가 발생했습니다."));
        }
    }
}
