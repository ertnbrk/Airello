package ai.planmate.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.planmate.auth.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GdprService {

    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public String exportUserData() {
        throw new UnsupportedOperationException(
                "GDPR export requires authentication to be enabled");
    }

    @Transactional
    public void deleteUserAccount() {
        throw new UnsupportedOperationException(
                "Account deletion requires authentication to be enabled");
    }
}
