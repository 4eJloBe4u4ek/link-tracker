package backend.academy.bot.controller;

import backend.academy.bot.service.UpdateService;
import backend.academy.shared.dto.LinkUpdate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UpdateController {
    private final UpdateService updateService;

    public UpdateController(UpdateService updateService) {
        this.updateService = updateService;
    }

    @PostMapping("/updates")
    public ResponseEntity<Void> updateLink(@RequestBody LinkUpdate update) {
        updateService.processUpdate(update);
        return ResponseEntity.ok().build();
    }
}
