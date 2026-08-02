package com.incentive.activity;
import java.util.Map; import org.springframework.web.bind.annotation.GetMapping; import org.springframework.web.bind.annotation.RequestMapping; import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1") public class IncentiveHealthController { @GetMapping("/activities") Map<String,String> activities() { return Map.of("service", "incentive-service", "status", "foundation-ready"); } }

