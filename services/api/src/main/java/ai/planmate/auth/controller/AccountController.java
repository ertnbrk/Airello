package ai.planmate.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.auth.service.GdprService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/me")
@RequiredArgsConstructor
public class AccountController {

    private final GdprService gdprService;

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public String exportData() {
        return gdprService.exportUserData();
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount() {
        gdprService.deleteUserAccount();
    }
}
