package uk.gov.hmcts.reform.pcs.idam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdamTokenRequest {
    
    @NotBlank(message = "Grant type is required")
    @JsonProperty("grant_type")
    private String grantType;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "Client ID is required")
    @JsonProperty("client_id")
    private String clientId;
    
    @NotBlank(message = "Client secret is required")
    @JsonProperty("client_secret")
    private String clientSecret;
    
    @NotBlank(message = "Scope is required")
    private String scope;
}
