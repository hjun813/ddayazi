package com.certpath.api;

import com.certpath.api.ApiModels.ApiResponse;
import com.certpath.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/me/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserRepository users;
    public record ProfileResponse(String nickname,String desiredJobRole) {}
    @GetMapping
    public ApiResponse<ProfileResponse> profile(Principal principal){
        var user=users.findByEmail(principal.getName()).orElseThrow();
        return ApiResponse.ok(new ProfileResponse(user.getNickname(),user.getDesiredJobRole()==null?null:user.getDesiredJobRole().getName()));
    }
}