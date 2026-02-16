package ai.planmate.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.ai.service.QuotaGuardService;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.UserPlan;
import ai.planmate.auth.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/me/plan")
@RequiredArgsConstructor
@Slf4j
public class PlanController {

    private final QuotaGuardService quotaGuardService;
    private final AppUserRepository appUserRepository;

    @GetMapping
    public QuotaGuardService.QuotaStatus getQuotaStatus(@AuthenticationPrincipal AppUser user) {
        return quotaGuardService.getQuotaStatus(user);
    }

    /** Mock upgrade endpoint for testing. In production, this would be tied to Stripe payments. */
    @PostMapping("/upgrade")
    @ResponseStatus(HttpStatus.OK)
    public QuotaGuardService.QuotaStatus upgradeToPro(@AuthenticationPrincipal AppUser user) {
        log.info("Upgrading user { } to PRO plan", user.getId());

        user.setPlan(UserPlan.PRO);
        appUserRepository.save(user);

        return quotaGuardService.getQuotaStatus(user);
    }
}
