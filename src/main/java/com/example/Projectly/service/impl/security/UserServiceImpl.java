package com.example.Projectly.service.impl.security;

import com.example.Projectly.bean.core.user.User;
import com.example.Projectly.exception.BusinessException;
import com.example.Projectly.exception.ErrorCode;
import com.example.Projectly.dao.facade.security.UserDao;
import com.example.Projectly.service.facade.security.UserService;
import com.example.Projectly.ws.converter.user.UserConverter;
import com.example.Projectly.ws.dto.user.ChangePasswordRequest;
import com.example.Projectly.ws.dto.user.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.Projectly.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String userEmail) throws UsernameNotFoundException {
        return this.userDao.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + userEmail));
    }

    @Override
    @Transactional(timeout = 30)
    public void updateProfileInfo(final ProfileUpdateRequest request, final String userId) {
        final User savedUser = this.userDao.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        this.userConverter.mergeUserInfo(savedUser, request);
        this.userDao.save(savedUser);
    }

    @Override
    @Transactional(timeout = 30)
    public void changePassword(final ChangePasswordRequest req, final String userId) {

        if (!req.getNewPassword().equals(req.getConfirmNewPassword())) {
            throw new BusinessException(CHANGE_PASSWORD_MISMATCH);
        }

        final User savedUser = this.userDao.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        if (!this.passwordEncoder.matches(req.getCurrentPassword(), savedUser.getPassword())) {
            throw new BusinessException(INVALID_CURRENT_PASSWORD);
        }

        final String encoded = this.passwordEncoder.encode(req.getNewPassword());
        savedUser.setPassword(encoded);
        this.userDao.save(savedUser);
    }

    @Override
    @Transactional(timeout = 30)
    public void deactivateAccount(final String userId) {
        final User user = this.userDao.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_DEACTIVATED);
        }

        user.setEnabled(false);
        this.userDao.save(user);
    }

    @Override
    @Transactional(timeout = 30)
    public void reactivateAccount(final String userId) {
        final User user = this.userDao.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        if (user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_DEACTIVATED);
        }

        user.setEnabled(true);
        this.userDao.save(user);
    }

    @Override
    @Transactional(timeout = 30)
    public void deleteAccount(final String userId) {
        final User user = this.userDao.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        user.setEnabled(false);
        user.setLocked(true);
        this.userDao.save(user);
    }
}
