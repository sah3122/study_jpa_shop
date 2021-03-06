package me.study.studyjpashop.controller;

import lombok.RequiredArgsConstructor;
import me.study.studyjpashop.domain.Address;
import me.study.studyjpashop.domain.Member;
import me.study.studyjpashop.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/members/new")
    public String createForm(Model model) {
        model.addAttribute("memberForm", new MemberForm());
        return "members/createMemberForm";
    }

    @PostMapping("/members/new")
    public String create(@Valid MemberForm form, BindingResult result) {

        if(result.hasErrors()) { // BindingResult는 Valid 에서 에러가 날 경우 해당 에러를 result 에 담아둔다.
            return "members/createMemberForm";
        }

        Address address = new Address(form.getCity(), form.getStreet(), form.getZipcode());

        Member member = new Member();
        member.setName(form.getName());
        member.setAddress(address);

        memberService.join(member);

        return "redirect:/";
    }

    @GetMapping("/members")
    public String list(Model model) { // 기본적으로 entity 를 외부로 바로 보내는건 안된다. 특히 api 에서 ssr 시 선택적으로 사용 가능. dto나 form 객체를 만들어 사용하자.
        List<Member> members = memberService.findMembers();
        model.addAttribute("members", members);

        return "members/memberList";
    }
}
