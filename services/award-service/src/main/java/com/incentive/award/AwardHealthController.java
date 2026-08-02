package com.incentive.award;
import java.util.Map; import org.springframework.web.bind.annotation.GetMapping; import org.springframework.web.bind.annotation.RequestMapping; import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1") public class AwardHealthController { @GetMapping("/awards") Map<String,String> awards() { return Map.of("service", "award-service", "status", "foundation-ready"); } }

