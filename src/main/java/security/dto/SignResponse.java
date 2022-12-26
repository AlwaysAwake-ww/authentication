package security.dto;


import jakarta.persistence.Column;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignResponse {

    private Long id;

    private String email;

    private String name;

    private List<Authority> roles = new ArrayList<>();

    private TokenDto token;

    public SignResponse(Member member){
        this.id = member.getId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.roles = member.getRoles();

    }
}
