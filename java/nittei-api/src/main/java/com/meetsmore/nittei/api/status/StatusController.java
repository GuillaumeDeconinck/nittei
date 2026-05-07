package com.meetsmore.nittei.api.status;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class StatusController {

  @GetMapping("/healthcheck")
  public ResponseEntity<Map<String, String>> healthcheck() {
    return ResponseEntity.ok(Map.of("message", "Ok!"));
  }

  @GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> metrics() {
    return ResponseEntity.ok("# metrics placeholder\n");
  }
}
