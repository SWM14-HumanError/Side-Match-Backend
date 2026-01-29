package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.Position;
import com.example.matchup.matchupbackend.dto.UploadFile;
import com.example.matchup.matchupbackend.dto.UserCardResponse;
import com.example.matchup.matchupbackend.dto.request.user.ProfileCreateRequest;
import com.example.matchup.matchupbackend.dto.request.user.UserSearchRequest;
import com.example.matchup.matchupbackend.dto.response.user.InviteMyTeamInfoResponse;
import com.example.matchup.matchupbackend.dto.response.user.InviteMyTeamResponse;
import com.example.matchup.matchupbackend.dto.response.user.SliceUserCardResponse;
import com.example.matchup.matchupbackend.entity.*;
import com.example.matchup.matchupbackend.error.exception.AuthorizeException;
import com.example.matchup.matchupbackend.error.exception.ExpiredTokenException;
import com.example.matchup.matchupbackend.error.exception.InvalidValueEx.InvalidUserDeleteException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.global.config.jwt.TokenService;
import com.example.matchup.matchupbackend.global.util.CookieUtil;
import com.example.matchup.matchupbackend.repository.InviteTeamRepository;
import com.example.matchup.matchupbackend.repository.mentoring.MentoringRepository;
import com.example.matchup.matchupbackend.repository.team.TeamRepository;
import com.example.matchup.matchupbackend.repository.TeamUserRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import com.example.matchup.matchupbackend.repository.UserPositionRepository;
import com.example.matchup.matchupbackend.repository.UserProfileRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.matchup.matchupbackend.global.config.oauth.handler.OAuth2SuccessHandler.*;
    
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final TokenService tokenService;
    private final UserPositionRepository userPositionRepository;
    private final UserProfileRepository userProfileRepository;
    private final FileService fileService;
    private final UserProfileService userProfileService;
    private final InviteTeamRepository inviteTeamRepository;
    private final MentoringRepository mentoringRepository;
    private final TeamRepository teamRepository;
    private final TeamUserRepository teamUserRepository;
    
    public SliceUserCardResponse searchSliceUserCard(UserSearchRequest userSearchRequest, Pageable pageable) {
        Slice<User> userListByUserRequest = userRepository.findUserListByUserRequest(userSearchRequest, pageable);
        SliceUserCardResponse sliceUserCardResponse = SliceUserCardResponse.builder()
                .userCardResponses(userCardResponseList(userListByUserRequest.getContent()))
                .size(userListByUserRequest.getSize())
                .hasNextSlice(userListByUserRequest.hasNext())
                .build();
        return sliceUserCardResponse;
    }

    public List<UserCardResponse> userCardResponseList(List<User> userList) {
        List<UserPosition> userPositionList = userPositionRepository.findAllJoinUserBy(userList);
        return userList.stream().map(
                user -> {
                    List<UserPosition> userPositions = userPositionList.stream()
                            .filter(userPosition -> userPosition.getUser().getId().equals(user.getId()))
                            .sorted(Comparator.comparing(UserPosition::getTypeLevel).reversed()).collect(Collectors.toList());

                    return UserCardResponse.of(user.getId(), user.getPictureUrl(), user.getUserLevel(),
                            user.getNickname(), userPositions.isEmpty() ? Position.of("없음", 0L) : Position.from(userPositions.get(0)),
                            user.getFeedbackScore(), user.getLikes(), user.returnStackList());

                }
        ).collect(Collectors.toList());
    }

    @Transactional
    public String saveAdditionalUserInfo(ProfileCreateRequest request) {
        // 토큰을 발행하는 과정에서 보안을 위한 email과 id 값을 받고 DB와 비교합니다.
        isAuthorized(request);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UserNotFoundException("회원가입을 진행하면서 존재하지 않는 이메일"));

        // todo: 여기서 UnsupportedOperationException 에러 발생: 현재는 immutable list라는 것으로 이해하고 있지만 왜 인지는 모르겟음
        //  List<UserPosition> userPositions = request.getUserPositionLevels().entrySet().stream().map(positionName -> UserPosition.builder().positionName(positionName.getKey()).positionLevel(positionName.getValue()).user(user).build()).toList();
        List<UserPosition> userPositions = request.getProfileTagPositions().stream()
                        .map(userPosition -> UserPosition.create(userPosition, user))
                        .toList();
        if (request.getPictureUrl() != null) { //썸네일 사진이 있는 경우
            UploadFile uploadFile = fileService.storeBase64ToFile(request.getPictureUrl(), request.getImageName());
            user.setUploadFile(uploadFile);
        }
        User updatedUser = user.updateFirstLogin(request, userPositions);
        userProfileService.updateUserTags(request.getProfileTagPositions(), updatedUser);
        userPositionRepository.saveAll(userPositions);
        userProfileRepository.save(UserProfile.createSignUp(user));

        return tokenProvider.generateToken(user, ACCESS_TOKEN_DURATION);
    }

    public void tokenRefresh(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String refreshToken = Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals("refresh_token"))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
            String newAccessToken = tokenService.createNewAccessToken(refreshToken);
            CookieUtil.deleteCookie(request, response, "token");
            CookieUtil.addCookie(response, "token", newAccessToken, 2 * 60 * 60);
        } else {
            throw new ExpiredTokenException("만료된 refresh 토큰으로 접근하였거나 토큰이 없습니다.");
        }
    }

    @Transactional
    public void updateUserLastLogin(String authorizationHeader) {
        Long userId = tokenProvider.getUserId(authorizationHeader, "유저 온라인 상태를 확인하면서 유효하지 않은 토큰을 받았습니다.");
        User user = userRepository.findUserById(userId).orElseThrow(() -> new UserNotFoundException("유저 온라인 상태를 확인하면서 존재하지 않는 유저 id를 받았습니다."));

        user.updateUserLastLogin();
    }

    /**
     * 기업 프로젝트에 초대하기에 필요한 프로젝트 목록을 조회합니다.
     */
    public InviteMyTeamResponse getInviteMyTeam(Long senderId, Long receiverId) {
        User sender = userRepository.findUserById(receiverId).orElseThrow(() -> new UserNotFoundException("초대 하는 사람이 없습니다."));
        User receiver = userRepository.findUserById(receiverId).orElseThrow(() -> new UserNotFoundException("초대 받는 사람이 없습니다."));
        List<TeamUser> senderTeamList = teamUserRepository.findAcceptedTeamUserByUserId(senderId);

        List<InviteTeam> inviteTeams = inviteTeamRepository.findAllBySenderAndReceiver(sender, receiver);
        Set<Team> inviteTeamsSet = inviteTeams.stream()
                .map(InviteTeam::getTeam)
                .collect(Collectors.toSet());

        List<InviteMyTeamInfoResponse> response = senderTeamList.stream()
                .map(TeamUser::getTeam)
                .filter(team -> team.getIsDeleted().equals(0L) && notDuplicateSuggestTeam(inviteTeamsSet, team))
                .map(team -> InviteMyTeamInfoResponse.builder().team(team).build())
                .toList();
        return new InviteMyTeamResponse(response);
    }

    /**
     * OAuth2SuccessHandler 로그인에 성공하면
     * Refresh 토큰을 User 엔터티에 저장
     * 그리고 RefreshToken 쿠키에 저장
     */
    @Transactional
    public User saveRefreshToken(HttpServletRequest request, HttpServletResponse response, String email, Long id) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("로그인에 성공하면서 Authorization 서버로부터 받은 이메일과 다른 이메일로 요청했습니다."));
        String refreshToken = tokenProvider.generateToken(user, REFRESH_TOKEN_DURATION);

        user.updateNewRefreshToken(refreshToken, id);
        addRefreshTokenToCookie(request, response, refreshToken);

        return user;
    }

    private void addRefreshTokenToCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        int cookieMaxAge = (int) REFRESH_TOKEN_DURATION.toSeconds();

        CookieUtil.deleteCookie(request, response, REFRESH_TOKEN_COOKIE_NAME);
        CookieUtil.addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, cookieMaxAge);
    }

    private void isAuthorized(ProfileCreateRequest request) {
        String email = request.getEmail();
        Long id = request.getId();
        if (email == null && id == null) {
            throw new AuthorizeException("인가받지 못한 사용자가 회원가입을 시도하고 있습니다.");
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("회원가입을 하면서 존재하지 않는 유저 이메일로 요청했습니다."));
        if (!user.getAgreeTermOfServiceId().equals(id)) {
            throw new AuthorizeException("회원가입을 하면서 유효하지 않은 id 값으로 요청했습니다.");
        }
    }


    /**
     * 회원 탈퇴를 진행하는 메서드
     */
    @Transactional
    public void deleteUser(String userToken) {
        Long userId = tokenProvider.getUserId(userToken, "deleteUser");
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("회원 탈퇴하는 유저를 찾을수 없습니다."));
        isDeletableUser(user);
        user.deleteUser();
    }

    /**
     * 회원 탈퇴가 가능한 유저인가 검증
     */
    private void isDeletableUser(User user) {
        if (user.getIsDeleted()) {
            throw new InvalidUserDeleteException("이미 탈퇴된 유저입니다. - userID: " + user.getId());
        }
        if (user.getIsMentor()) {
            boolean mentoringEmpty = mentoringRepository.findAllByMentorAndIsDeleted(user, false).isEmpty();
            if (!mentoringEmpty) {
                throw new InvalidUserDeleteException("멘토링을 모두 마무리 하고 탈퇴해주세요:) - userID: " + user.getId());
            }
        }
        boolean teamEmpty = teamRepository.findActiveTeamByLeaderID(user.getId()).isEmpty();
        if (!teamEmpty) {
            throw new InvalidUserDeleteException("팀을 모두 마무리 하고 탈퇴해주세요:) - userID: " + user.getId());
        }
    }

    private boolean notDuplicateSuggestTeam(Set<Team> inviteTeamsSet, Team team) {
        return !inviteTeamsSet.contains(team);
    }
}
