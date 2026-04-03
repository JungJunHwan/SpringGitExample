package com.example.board.member;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.example.board.member.model.Member;

@Component
public class MemberValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        // 검증하려는 객체가 Member 타입이거나 Member를 상속받은 클래스인지 확인
        return Member.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Member member = (Member) target;

        String pw1 = member.getPassword();
        String pw2 = member.getPassword2();

        // 비밀번호와 비밀번호 확인 값이 일치하는지 검증
        if (pw1 != null && pw1.equals(pw2)) {
            // 일치할 경우 통과
            // pass
        } else {
            // 일치하지 않을 경우 에러 추가
            // 필드명, 에러 코드, 기본 메시지 순서
            errors.rejectValue("password2", "PASSWORD_NOT_EQUALS", "비밀번호 확인이 다릅니다");
        }
    }
}