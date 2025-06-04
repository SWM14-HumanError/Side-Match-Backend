package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.dto.request.user.ProfileCreateRequest;
import com.example.matchup.matchupbackend.dto.request.user.UserSearchRequest;
import com.example.matchup.matchupbackend.dto.response.user.InviteMyTeamResponse;
import com.example.matchup.matchupbackend.dto.response.user.SliceUserCardResponse;
import com.example.matchup.matchupbackend.dto.response.user.SuggestInviteMyTeamRequest;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.service.AlertCreateService;
import com.example.matchup.matchupbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.example.matchup.matchupbackend.global.config.jwt.TokenProvider.HEADER_AUTHORIZATION;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AlertCreateService alertCreateService;
    private final TokenProvider tokenProvider;

    @GetMapping("/list/user")
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "메인 페이지 / 유저 API")
    public SliceUserCardResponse showUsers(UserSearchRequest userSearchRequest, Pageable pageable) {
        return userService.searchSliceUserCard(userSearchRequest, pageable);
    }

    /**
     * 회원가입 후에 최소 정보를 받는다.
     * 서비스에 사용할 닉네임과 프로필 사진, 생년월일, 개발 연차
     */
    @PutMapping("/login/user/info")
    @ResponseStatus(HttpStatus.CREATED)
    public String additionalUserInfo(@Valid @RequestBody ProfileCreateRequest request) {
        return userService.saveAdditionalUserInfo(request);
    }

    /**
     * 액세스 토큰이 만료되었을 때, 리프레시 토큰을 이용해 액세스 토큰을 재발급
     * 이용약관에 동의하지 않은, 최초 로그인 유저는 이용약관 동의 페이지로 이동하기 위해서 signup query를 추가함
     * 관련 값은 tokenService.createNewAccessToken(token)에서 처리
     */
    @GetMapping("/login/token/refresh")
    @ResponseStatus(HttpStatus.OK)
    public void loginToken(HttpServletRequest request, HttpServletResponse response) {
        userService.tokenRefresh(request, response);
    }

    /**
     * 유저가 이용약관에 동의
     */
//    @GetMapping("/login/user/term")
//    @ResponseStatus(HttpStatus.OK)
//    public String userAgreeTermOfService(@RequestParam("email") String email, @RequestParam("id") Long id) {
//        return userService.userAgreeTermOfService(email, id);
//    }

    /**
     * 유저가 온라인 상태를 지속하면 로그인 시간 최신화
     */
    @GetMapping("/user/online")
    @ResponseStatus(HttpStatus.OK)
    public void isUserOnline(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader) {
        userService.updateUserLastLogin(authorizationHeader);
    }

    /**
     * 내 프로젝트에 초대하기에서 내 프로젝트 목록을 조회합니다.
     */
    @GetMapping("/user/invite")
    @ResponseStatus(HttpStatus.OK)
    public InviteMyTeamResponse showInviteMyTeam(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                                 @RequestParam("receiver") Long receiverId) {
        Long userId = tokenProvider.getUserId(authorizationHeader, "getInviteMyTeam");
        return userService.getInviteMyTeam(userId, receiverId);
    }

    @PostMapping("/user/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public void suggestInviteMyTeam(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                    @Valid @RequestBody SuggestInviteMyTeamRequest request) {
        alertCreateService.postInviteMyTeam(authorizationHeader, request);
    }

    @DeleteMapping("/user/delete")
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "회원 탈퇴하는 API")
    public void deleteUser(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader){
        userService.deleteUser(authorizationHeader);
    }
}
