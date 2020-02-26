package fr.lpr.membership.service;

import fr.lpr.membership.domain.Authority;
import fr.lpr.membership.domain.User;
import fr.lpr.membership.repository.AuthorityRepository;
import fr.lpr.membership.repository.PersistentTokenRepository;
import fr.lpr.membership.repository.UserRepository;
import fr.lpr.membership.security.SecurityUtils;
import fr.lpr.membership.service.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service class for managing users.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {

	private final PasswordEncoder passwordEncoder;

	private final UserRepository userRepository;

	private final PersistentTokenRepository persistentTokenRepository;

	private final AuthorityRepository authorityRepository;

	public Optional<User> activateRegistration(String key) {
		log.debug("Activating user for activation key {}", key);
		userRepository.findOneByActivationKey(key).map(user -> {
			// activate given user for the registration key.
			user.setActivated(true);
			user.setActivationKey(null);
			userRepository.save(user);
			log.debug("Activated user: {}", user);
			return user;
		});
		return Optional.empty();
	}

	public Optional<User> completePasswordReset(String newPassword, String key) {
		log.debug("Reset user password for reset key {}", key);

		return userRepository.findOneByResetKey(key).filter(user -> {
			final LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
			return user.getResetDate().isAfter(oneDayAgo);
		}).map(user -> {
			user.setActivated(true);
			user.setPassword(passwordEncoder.encode(newPassword));
			user.setResetKey(null);
			user.setResetDate(null);
			userRepository.save(user);
			return user;
		});
	}

	public Optional<User> requestPasswordReset(String mail) {
		return userRepository.findOneByEmail(mail).map(user -> {
			user.setResetKey(RandomUtil.generateResetKey());
			user.setResetDate(LocalDateTime.now());
			userRepository.save(user);
			return user;
		});
	}

	public User createUserInformation(String login, String password, String firstName, String lastName, String email, String langKey) {

		final User newUser = new User();
		final Authority authority = authorityRepository.getOne("ROLE_USER");
		final Set<Authority> authorities = new HashSet<>();
		final String encryptedPassword = passwordEncoder.encode(password);
		newUser.setLogin(login);
		// new user gets initially a generated password
		newUser.setPassword(encryptedPassword);
		newUser.setFirstName(firstName);
		newUser.setLastName(lastName);
		newUser.setEmail(email);
		newUser.setLangKey(langKey);
		// new user is not active
		newUser.setActivated(false);
		// new user gets registration key
		newUser.setActivationKey(RandomUtil.generateActivationKey());
		authorities.add(authority);
		newUser.setAuthorities(authorities);
		userRepository.save(newUser);
		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	public void updateUserInformation(String firstName, String lastName, String email, String langKey) {
		userRepository.findOneByLogin(SecurityUtils.getCurrentLogin()).ifPresent(u -> {
			u.setFirstName(firstName);
			u.setLastName(lastName);
			u.setEmail(email);
			u.setLangKey(langKey);
			userRepository.save(u);
			log.debug("Changed Information for User: {}", u);
		});
	}

	public void changePassword(String password) {
		userRepository.findOneByLogin(SecurityUtils.getCurrentLogin()).ifPresent(u -> {
			final String encryptedPassword = passwordEncoder.encode(password);
			u.setPassword(encryptedPassword);
			userRepository.save(u);
			log.debug("Changed password for User: {}", u);
		});
	}

	@Transactional(readOnly = true)
	public User getUserWithAuthorities() {
		final User currentUser = userRepository.findOneByLogin(SecurityUtils.getCurrentLogin()).get();
		currentUser.getAuthorities().size(); // eagerly load the association
		return currentUser;
	}

	/**
	 * <p>
	 * Persistent Token are used for providing automatic authentication, they should be automatically deleted after 30 days.
	 * </p>
	 * <p>
	 * This is scheduled to get fired everyday, at midnight.
	 * </p>
	 */
	@Scheduled(cron = "0 0 0 * * ?")
	public void removeOldPersistentTokens() {
		final LocalDate now = LocalDate.now();
		persistentTokenRepository.findByTokenDateBefore(now.minusMonths(1)).forEach(token -> {
			log.debug("Deleting token {}", token.getSeries());
			final User user = token.getUser();
			user.getPersistentTokens().remove(token);
			persistentTokenRepository.delete(token);
		});
	}

	/**
	 * <p>
	 * Not activated users should be automatically deleted after 3 days.
	 * </p>
	 * <p>
	 * This is scheduled to get fired everyday, at 01:00 (am).
	 * </p>
	 */
	@Scheduled(cron = "0 0 1 * * ?")
	public void removeNotActivatedUsers() {
		final LocalDateTime now = LocalDateTime.now();
		final List<User> users = userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(now.minusDays(3));
		for (final User user : users) {
			log.debug("Deleting not activated user {}", user.getLogin());
			userRepository.delete(user);
		}
	}
}
