package com.recipe.controller;

import com.recipe.service.PosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posters")
@RequiredArgsConstructor
public class PosterController {

    private final PosterService posterService;

    @GetMapping("/recipe/{id}")
    public ResponseEntity<byte[]> generateRecipePoster(@PathVariable Long id) {
        byte[] posterBytes = posterService.generateRecipePoster(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(posterBytes.length);
        headers.set("Content-Disposition", "inline; filename=recipe-poster-" + id + ".png");

        return ResponseEntity.ok()
                .headers(headers)
                .body(posterBytes);
    }
}
