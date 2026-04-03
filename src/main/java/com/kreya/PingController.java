package com.kreya;

import com.kreya.shared.dto.ApiResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PingController {

    @GetMapping("/ping")
    public ApiResponse<PingResponse> ping() {
        return ApiResponse.success(new PingResponse("pong"), "Kreya backend is running", "/api/v1/ping");
    }

    public record PingResponse(String status) {
    }
}
