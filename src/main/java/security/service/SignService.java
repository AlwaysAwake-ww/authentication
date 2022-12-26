package security.service;


import lombok.RequiredArgsConstructor;
import org.springframework.expression.ExpressionException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import security.dto.*;
import security.jwt.JwtProvider;
import security.repository.MemberRepository;
import security.repository.TokenRepository;

import java.util.Collections;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SignService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    private final TokenRepository tokenRepository;

    public SignResponse login(SignRequest request) throws Exception {
        Member member = memberRepository.findByEmail(request.getEmail()).orElseThrow(() ->
                new BadCredentialsException("잘못된 계정정보입니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BadCredentialsException("잘못된 계정정보입니다.");
        }

        member.setRefreshToken(createRefreshToken(member));

        return SignResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .roles(member.getRoles())
                .token(TokenDto.builder()
                        .accessToken(jwtProvider.createToken(member.getEmail(), member.getRoles()))
                        .refreshToken(member.getRefreshToken())
                        .build())
                .build();
    }

    public boolean register(SignRequest request) throws Exception{

        try {
            Member member = Member.builder()
                    .password(passwordEncoder.encode(request.getPassword()))
                    .name(request.getName())
                    .email(request.getEmail())
                    .build();

            member.setRoles(Collections.singletonList(Authority.builder().name("ROLE_USER").build()));

            memberRepository.save(member);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new Exception("잘못된 요청입니다.");
        }
        return true;
    }

    public SignResponse getMember(String email) throws Exception{

        Member member = memberRepository.findByEmail(email).orElseThrow(()->new Exception("cannot find account"));
        return new SignResponse(member);
    }





    /**
     * Refresh 토큰을 생성한다.
     * Redis 내부에는
     * refreshToken:memberId : tokenValue
     * 형태로 저장한다.
     */

    public String createRefreshToken(Member member){

        System.out.println("### create refresh token!! ###");

        Token token1 =Token.builder()
                .id(member.getId())
                .refreshToken(UUID.randomUUID().toString())
//                        .expiration(300)
                .expiration(30)
                .build();
        System.out.println(token1.getId());
        System.out.println(token1.getRefreshToken());
        System.out.println(token1.getExpiration());


        Token token = tokenRepository.save(
                Token.builder()
                        .id(member.getId())
                        .refreshToken(UUID.randomUUID().toString())
//                        .expiration(300)
                        .expiration(300)
                        .build()
        );
        System.out.println(token);
        return token.getRefreshToken();
    }

    public Token validRefreshToken(Member member, String refreshToken) throws Exception {
        Token token = tokenRepository.findById(member.getId()).orElseThrow(() -> new Exception("만료된 계정입니다. 로그인을 다시 시도하세요"));
        // 해당유저의 Refresh 토큰 만료 : Redis에 해당 유저의 토큰이 존재하지 않음
        if (token.getRefreshToken() == null) {
            return null;
        } else {
            // 리프레시 토큰 만료일자가 얼마 남지 않았을 때 만료시간 연장..?
            if(token.getExpiration() < 10) {
                token.setExpiration(1000);
                tokenRepository.save(token);
            }

            // 토큰이 같은지 비교
            if(!token.getRefreshToken().equals(refreshToken)) {
                return null;
            } else {
                return token;
            }
        }
    }

    public TokenDto refreshAccessToken(TokenDto token) throws Exception {

        String email = jwtProvider.getAccount(token.getAccessToken());
        Member member = memberRepository.findByEmail(email).orElseThrow(() ->
                new BadCredentialsException("잘못된 계정정보입니다."));

        Token refreshToken = validRefreshToken(member, token.getRefreshToken());

        if (refreshToken != null) {
            return TokenDto.builder()
                    .accessToken(jwtProvider.createToken(email, member.getRoles()))
                    .refreshToken(refreshToken.getRefreshToken())
                    .build();
        } else {
            throw new Exception("로그인을 해주세요");
        }
    }

}
