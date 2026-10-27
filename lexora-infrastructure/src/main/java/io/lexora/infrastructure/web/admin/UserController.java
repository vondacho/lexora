package io.lexora.infrastructure.web.admin;

import io.lexora.application.user.UserAccount;
import io.lexora.application.user.UserUseCases;
import io.lexora.domain.user.Grant;
import io.lexora.domain.user.User;
import io.lexora.domain.user.Username;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.UsersApi;
import io.lexora.infrastructure.web.api.model.CreateUserInputDto;
import io.lexora.infrastructure.web.api.model.GrantDto;
import io.lexora.infrastructure.web.api.model.UpdateUserInputDto;
import io.lexora.infrastructure.web.api.model.UserDto;
import io.lexora.infrastructure.web.api.model.UserSummaryDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class UserController implements UsersApi {

    private final UserUseCases users;

    UserController(UserUseCases users) {
        this.users = users;
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public UserDto getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return toDto(users.getUser(Username.of(username)));
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public List<UserSummaryDto> listUsers() {
        return Json.map(users.listUsers(), UserController::toSummary);
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public UserDto createUser(CreateUserInputDto in) {
        return toDto(users.createUser(Username.of(in.getUsername()), in.getPassword(), in.getFirstName(),
                in.getLastName(), in.getShortcut()));
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public UserDto getUser(String username) {
        return toDto(users.getUser(Username.of(username)));
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public UserDto updateUser(String username, UpdateUserInputDto in) {
        return toDto(users.updateUser(Username.of(username), in.getPassword(), in.getFirstName(), in.getLastName(),
                in.getShortcut()));
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public void deleteUser(String username) {
        users.deleteUser(Username.of(username));
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public List<GrantDto> listGrantCatalog() {
        return Json.map(users.grantCatalog(), g -> new GrantDto().module(g.module()).page(g.page()));
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public UserDto grantAll(String username) {
        return toDto(users.grantAll(Username.of(username)));
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public UserDto revokeAll(String username) {
        return toDto(users.revokeAll(Username.of(username)));
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public UserDto grant(String username, String module, String page) {
        return toDto(users.grant(Username.of(username), Grant.of(module, page)));
    }

    @Override
    @PreAuthorize(Permissions.ADMIN_USERS)
    public UserDto revoke(String username, String module, String page) {
        return toDto(users.revoke(Username.of(username), Grant.of(module, page)));
    }

    private static UserSummaryDto toSummary(User u) {
        return new UserSummaryDto().username(u.username().value()).firstName(u.firstName()).lastName(u.lastName())
                .shortcut(u.shortcut());
    }

    private static UserDto toDto(UserAccount account) {
        User u = account.user();
        return new UserDto().username(u.username().value()).firstName(u.firstName()).lastName(u.lastName())
                .shortcut(u.shortcut()).grants(account.grants().stream().map(Grant::authority).sorted().toList());
    }
}
