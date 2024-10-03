package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.UploadFile;
import com.example.matchup.matchupbackend.dto.request.mentoring.CreateOrEditMentoringRequest;
import com.example.matchup.matchupbackend.dto.response.mentoring.MentoringSearchResponse;
import com.example.matchup.matchupbackend.dto.response.mentoring.MentoringSliceResponse;
import com.example.matchup.matchupbackend.entity.Mentoring;
import com.example.matchup.matchupbackend.entity.User;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.ResourceNotFoundException;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.repository.LikeRepository;
import com.example.matchup.matchupbackend.repository.mentoring.*;
import com.example.matchup.matchupbackend.repository.team.TeamRepository;
import com.example.matchup.matchupbackend.repository.TeamUserRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MentoringServiceTest {

    private AlertCreateService alertCreateService;
    private LikeRepository likeRepository;
    private FileService fileService;
    private MentoringRepository mentoringRepository;
    private MentoringTagRepository mentoringTagRepository;
    private MentorVerifyRepository mentorVerifyRepository;
    private TeamRepository teamRepository;
    private TeamMentoringRepository teamMentoringRepository;
    private TeamUserRepository teamUserRepository;
    private TokenProvider tokenProvider;
    private UserRepository userRepository;
    private ReviewMentoringRepository reviewMentoringRepository;
    private MentoringService mentoringService;

    @BeforeEach
    void setUp() {
        alertCreateService = mock(AlertCreateService.class);
        likeRepository = mock(LikeRepository.class);
        fileService = mock(FileService.class);
        mentoringRepository = mock(MentoringRepository.class);
        mentoringTagRepository = mock(MentoringTagRepository.class);
        mentorVerifyRepository = mock(MentorVerifyRepository.class);
        teamRepository = mock(TeamRepository.class);
        teamMentoringRepository = mock(TeamMentoringRepository.class);
        teamUserRepository = mock(TeamUserRepository.class);
        tokenProvider = mock(TokenProvider.class);
        userRepository = mock(UserRepository.class);
        reviewMentoringRepository = mock(ReviewMentoringRepository.class);

        mentoringService = new MentoringService(
                alertCreateService, likeRepository, fileService, mentoringRepository,
                mentoringTagRepository, mentorVerifyRepository, teamRepository,
                teamMentoringRepository, teamUserRepository, tokenProvider,
                userRepository, reviewMentoringRepository
        );
    }

    @Test
    void 멘토링_상세_조회_실패_예외() {
        when(mentoringRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> mentoringService.showMentoringDetail(1L));
    }

    @Test
    void 멘토링_생성_멘토아님_예외() {
        String token = "Bearer test";
        Long userId = 10L;
        User mentor = mock(User.class);
        CreateOrEditMentoringRequest request = mock(CreateOrEditMentoringRequest.class);

        when(tokenProvider.getUserId(token)).thenReturn(userId);
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(mentor));
        when(mentor.getIsMentor()).thenReturn(false);

        assertThrows(Exception.class, () -> mentoringService.createMentoringByMentor(token, request));
    }

    @Test
    void 멘토링_삭제_성공() {
        String token = "Bearer test";
        Long userId = 10L;
        User mentor = mock(User.class);
        Mentoring mentoring = mock(Mentoring.class);

        when(tokenProvider.getUserId(token)).thenReturn(userId);
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(mentor));
        when(mentor.getIsMentor()).thenReturn(true);
        when(mentoringRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(mentoring));
        when(mentoring.getMentor()).thenReturn(mentor);
        when(mentoring.getId()).thenReturn(1L); // 이 부분 추가

        Long result = mentoringService.deleteMentoringByMentor(token, 1L);

        assertEquals(1L, result);
        verify(mentoring).delete();
    }

    @Test
    void 멘토링_삭제_권한없음_예외() {
        String token = "Bearer test";
        Long userId = 10L;
        User mentor = mock(User.class);
        User otherMentor = mock(User.class);
        Mentoring mentoring = mock(Mentoring.class);

        when(tokenProvider.getUserId(token)).thenReturn(userId);
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(mentor));
        when(mentoringRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(mentoring));
        when(mentoring.getMentor()).thenReturn(otherMentor);

        assertThrows(Exception.class, () -> mentoringService.deleteMentoringByMentor(token, 1L));
    }

    @Test
    void 멘토링_목록_조회_성공() {
        Pageable pageable = PageRequest.of(0, 10);
        Mentoring mentoring = mock(Mentoring.class);
        User mentor = mock(User.class);

        // mentor mock 세팅
        when(mentoring.getMentor()).thenReturn(mentor);
        when(mentor.getNickname()).thenReturn("닉네임");
        when(mentor.getUserLevel()).thenReturn(1L);
        when(mentor.getPictureUrl()).thenReturn("url");
        when(mentor.getId()).thenReturn(1L);

        // mentoring mock 세팅
        when(mentoring.getThumbnailUrl()).thenReturn("thumb");
        when(mentoring.getId()).thenReturn(1L);
        when(mentoring.getTitle()).thenReturn("title");
        when(mentoring.getRoleType()).thenReturn(null);
        when(mentoring.getCareer()).thenReturn(null);
        when(mentoring.getScore()).thenReturn(4.5);
        when(mentoring.getContent()).thenReturn("content");
        when(mentoring.getMentoringTags()).thenReturn(List.of());

        when(mentoringRepository.findMentoringByMentoringSearchParam(any(), eq(pageable)))
                .thenReturn(new SliceImpl<>(List.of(mentoring), pageable, false));
        when(likeRepository.countByMentoring(mentoring)).thenReturn(5L);

        MentoringSliceResponse response = mentoringService.showMentoringsInMentoringPage(null, null, pageable);

        assertEquals(10, response.getSize());
    }
}