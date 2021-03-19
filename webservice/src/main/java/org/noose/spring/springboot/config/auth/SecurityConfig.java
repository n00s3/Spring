package org.noose.spring.springboot.config.auth;

import lombok.RequiredArgsConstructor;
import org.noose.spring.springboot.domain.user.Role;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@RequiredArgsConstructor
@EnableWebSecurity  //SpringSecurity 설정들을 활성화
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .headers().frameOptions().disable()
                .and()
                .authorizeRequests()// URL 별 권한 관리를 설정하는 옵션의 시작
                    .antMatchers("/", "/css/**", "/images/**", "/js/**", "/h2-console/**").permitAll()
                    .antMatchers("/api/v1/**").hasRole(Role.USER.name()) // API는 USER 권한만 접근 가능
                    .anyRequest().authenticated()   // 나머지 URL들은 모두 인증된 사용자들에게만 허용
                    .and()
                        .logout()
                            .logoutSuccessUrl("/")  // 로그아웃 성공 시 / 주소로 이동
                    .and()
                        .oauth2Login()
                            .userInfoEndpoint()
                                .userService(customOAuth2UserService);

    }

}
