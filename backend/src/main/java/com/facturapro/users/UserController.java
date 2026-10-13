package com.facturapro.users;

import com.facturapro.common.web.PageResponse;
import com.facturapro.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("hasAuthority('USERS_VIEW')")
    public PageResponse<UserDtos.Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.list(search, active, Math.max(0, page), Math.min(100, Math.max(1, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USERS_VIEW')")
    public UserDtos.Response get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USERS_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtos.Response create(@Valid @RequestBody UserDtos.CreateRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USERS_WRITE')")
    public UserDtos.Response update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateRequest request) {
        return userService.update(id, request);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USERS_WRITE')")
    public Map<String, String> resetPassword(@PathVariable Long id,
                                             @Valid @RequestBody UserDtos.AdminResetPasswordRequest request) {
        userService.adminResetPassword(id, request);
        return Map.of("message", "Contrasena actualizada.");
    }

    @GetMapping("/me")
    public UserDtos.Response me() {
        return userService.get(currentUser.id());
    }

    @PostMapping("/me/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOwnPassword(@Valid @RequestBody UserDtos.ChangePasswordRequest request) {
        userService.changeOwnPassword(request);
    }
}
