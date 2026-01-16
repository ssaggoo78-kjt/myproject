package com.example.demo.app.stock.controller;

import com.example.demo.app.stock.service.StockApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stock-recommendation")
public class StockRecommendationController {

    @Autowired
    private StockApiService stockApiService;

    @GetMapping("/top-traded")
    public ResponseEntity<Map<String, Object>> getTopTradedStocks() {
        return ResponseEntity.ok(stockApiService.getTopTradedStocks());
    }
}
