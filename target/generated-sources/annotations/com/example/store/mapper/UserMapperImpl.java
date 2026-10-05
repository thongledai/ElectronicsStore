package com.example.store.mapper;

import com.example.store.dto.RegisterDTO;
import com.example.store.dto.UserResponseDTO;
import com.example.store.entity.Role;
import com.example.store.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-06T00:33:19+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 22 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toUser(RegisterDTO dto) {
        if ( dto == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.fullName( dto.getFullName() );
        user.email( dto.getEmail() );

        user.isEmailActive( false );
        user.isPhoneActive( false );
        user.point( 0 );
        user.eWallet( java.math.BigDecimal.ZERO );
        user.phone( dto.getResolvedPhone() );

        return user.build();
    }

    @Override
    public UserResponseDTO toUserResponseDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponseDTO.UserResponseDTOBuilder userResponseDTO = UserResponseDTO.builder();

        userResponseDTO.role( userRoleName( user ) );
        userResponseDTO.id( user.getId() );
        userResponseDTO.fullName( user.getFullName() );
        userResponseDTO.email( user.getEmail() );
        userResponseDTO.phone( user.getPhone() );
        userResponseDTO.avatar( user.getAvatar() );
        userResponseDTO.isEmailActive( user.getIsEmailActive() );

        return userResponseDTO.build();
    }

    private String userRoleName(User user) {
        Role role = user.getRole();
        if ( role == null ) {
            return null;
        }
        return role.getName();
    }
}
